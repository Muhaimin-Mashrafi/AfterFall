package com.afterfall.choice;

import com.afterfall.domain.InitialPopulation;
import com.afterfall.domain.Need;
import com.afterfall.domain.ResourceType;
import com.afterfall.domain.SurvivorId;
import com.afterfall.domain.World;
import com.afterfall.simulation.SimulationEngine;
import com.afterfall.simulation.SimulationLifecycle;
import com.afterfall.simulation.TickCoordinator;
import com.afterfall.simulation.TickResult;
import com.afterfall.simulation.WorldClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChoicePauseResumeBoundaryTest {

    private World world;
    private WorldClock clock;
    private TickCoordinator coordinator;
    private SimulationEngine engine;
    private ChoiceCoordinator choiceCoordinator;

    @BeforeEach
    void setUp() {
        world = InitialPopulation.createDefaultWorld("Haven");
        clock = new WorldClock(4);
        coordinator = new TickCoordinator(world, clock);
        choiceCoordinator = coordinator.choiceCoordinator();
        engine = new SimulationEngine(world, clock, coordinator);
    }

    @Test
    void pendingChoiceMomentPausesEngineOnlyAfterCurrentTickFullyCommits() {
        engine.start();
        assertThat(engine.getStatus()).isEqualTo(SimulationLifecycle.RUNNING);

        // Tick 1 executes cleanly
        TickResult tick1 = engine.tick();
        assertThat(tick1.isSuccess()).isTrue();
        assertThat(tick1.tickNumber()).isEqualTo(1L);
        assertThat(engine.getStatus()).isEqualTo(SimulationLifecycle.RUNNING);

        // A choice moment arises during tick 1 / before tick 2
        ChoiceOption optA = new ChoiceOption("OPT_A", "Conserve Food", "Rations preserved",
                ChoiceConsequence.builder("Conserve 20 food")
                        .resourceDelta(ResourceType.FOOD, 20)
                        .needDelta(Need.FOOD, -10)
                        .targetSurvivors(List.of(new SurvivorId(1)))
                        .build(), true);
        ChoiceOption optB = new ChoiceOption("OPT_B", "Distribute Extra Food", "Extra portions",
                ChoiceConsequence.builder("Spend 10 food")
                        .resourceDelta(ResourceType.FOOD, -10)
                        .needDelta(Need.FOOD, 20)
                        .targetSurvivors(List.of(new SurvivorId(1)))
                        .build(), false);

        ChoiceMoment moment = new ChoiceMoment("CHOICE_1", 2L, "CHAP_1", "Ration Directive", "Preserve or distribute rations?", List.of(optA, optB), "OPT_A");
        choiceCoordinator.triggerChoice(moment);
        assertThat(choiceCoordinator.hasPendingPause()).isTrue();

        // If pause is requested on engine, state is PAUSE_REQUESTED during execution
        engine.requestPause();
        assertThat(engine.getStatus()).isEqualTo(SimulationLifecycle.PAUSE_REQUESTED);

        int initialFood = world.settlement().resourceStock().quantity(ResourceType.FOOD);

        // Tick 2 executes and fully commits before transitioning to PAUSED
        TickResult tick2 = engine.tick();
        assertThat(tick2.isSuccess()).isTrue();
        assertThat(tick2.tickNumber()).isEqualTo(2L);
        assertThat(clock.tick()).isEqualTo(2L);
        assertThat(engine.getLatestSnapshot().tick()).isEqualTo(2L);

        // Consequence has NOT yet been applied during tick 2 (choice not yet submitted)
        assertThat(world.settlement().resourceStock().quantity(ResourceType.FOOD)).isEqualTo(initialFood);

        // Engine is now paused at the safe tick boundary
        assertThat(engine.getStatus()).isEqualTo(SimulationLifecycle.PAUSED);
        assertThatThrownBy(engine::tick)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot execute tick: engine is in state PAUSED");
    }

    @Test
    void submittingChoiceQueuesConsequenceForNextTickAndResumeDoesNotDuplicateOrSkip() {
        engine.start();
        engine.tick(); // Tick 1

        ChoiceOption optA = new ChoiceOption("OPT_A", "Conserve Food", "Rations preserved",
                ChoiceConsequence.builder("Conserve 25 food")
                        .resourceDelta(ResourceType.FOOD, 25)
                        .build(), true);
        ChoiceMoment moment = new ChoiceMoment("CHOICE_2", 2L, "CHAP_1", "Rationing", "Decision", List.of(optA), "OPT_A");
        choiceCoordinator.triggerChoice(moment);

        // Tick 2 executes, commits, and automatically pauses at tick boundary due to pending choice
        TickResult tick2 = engine.tick();
        assertThat(tick2.isSuccess()).isTrue();
        assertThat(tick2.tickNumber()).isEqualTo(2L);
        assertThat(engine.getStatus()).isEqualTo(SimulationLifecycle.PAUSED);

        int foodAtPause = world.settlement().resourceStock().quantity(ResourceType.FOOD);

        // Player submits valid choice while simulation is paused
        ChoiceRecord record = choiceCoordinator.submitDecision("CHOICE_2", "OPT_A");
        assertThat(record.selectedOptionId()).isEqualTo("OPT_A");
        assertThat(record.usedDefault()).isFalse();
        assertThat(choiceCoordinator.hasPendingPause()).isFalse();

        // Resume engine
        engine.resume();
        assertThat(engine.getStatus()).isEqualTo(SimulationLifecycle.RUNNING);

        // Next tick execution (Tick 3) applies queued consequence; no tick is skipped or duplicated
        TickResult tick3 = engine.tick();
        assertThat(tick3.isSuccess()).isTrue();
        assertThat(tick3.tickNumber()).isEqualTo(3L);
        assertThat(clock.tick()).isEqualTo(3L);

        // Consequence is applied exactly at tick 3 boundary
        assertThat(world.settlement().resourceStock().quantity(ResourceType.FOOD)).isEqualTo(foodAtPause + 25);

        // Subsequent tick 4 does not reapply the consequence
        TickResult tick4 = engine.tick();
        assertThat(tick4.isSuccess()).isTrue();
        assertThat(tick4.tickNumber()).isEqualTo(4L);
        assertThat(clock.tick()).isEqualTo(4L);
        // (settlement upkeep may forage one unit on this tick, but never a second +25)
        assertThat(world.settlement().resourceStock().quantity(ResourceType.FOOD))
                .isBetween(foodAtPause + 25, foodAtPause + 25 + 1);
    }
}
