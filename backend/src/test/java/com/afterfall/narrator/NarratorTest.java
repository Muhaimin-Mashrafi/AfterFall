package com.afterfall.narrator;

import com.afterfall.domain.*;
import com.afterfall.events.EventBus;
import com.afterfall.events.EventSeverity;
import com.afterfall.events.SimEvent;
import com.afterfall.simulation.SeededRandomFactory;
import com.afterfall.simulation.TickCoordinator;
import com.afterfall.simulation.TickResult;
import com.afterfall.simulation.WorldClock;
import com.afterfall.simulation.RegionWorkerPool;
import com.afterfall.simulation.CrisisManager;
import com.afterfall.observer.ObserverAI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class NarratorTest {

    private Settlement settlement;
    private SeededRandomFactory randomFactory;
    private Narrator narrator;

    @BeforeEach
    void setUp() {
        settlement = new Settlement("Narrator Test Settlement", Settlement.defaultRegions(), new ResourceStock(50, 50, 20));
        Survivor s1 = new Survivor(new SurvivorId(1), "Alice", Personality.neutral(), new Profession(ProfessionType.GENERAL_WORKER, 50), Needs.fullySatisfied(), Health.healthy(), new Inventory(10));
        Survivor s2 = new Survivor(new SurvivorId(2), "Bob", Personality.neutral(), new Profession(ProfessionType.FARMER, 50), Needs.fullySatisfied(), Health.healthy(), new Inventory(10));
        settlement.registerSurvivor(s1, RegionId.REGION_1);
        settlement.registerSurvivor(s2, RegionId.REGION_2);

        randomFactory = new SeededRandomFactory(12345L);
        narrator = new Narrator(settlement, randomFactory);
    }

    @Test
    void generatesCaptionLinkedToSourceEvent() {
        SimEvent event = new SimEvent(
                new EventId(42L),
                8L,
                "SURVIVOR_WORK",
                List.of(new SurvivorId(1)),
                Optional.of(RegionId.REGION_1),
                EventSeverity.SIGNIFICANT,
                "Repaired shelter",
                Map.of(),
                List.of()
        );

        narrator.onEvents(List.of(event));

        assertThat(narrator.captions()).hasSize(1);
        StoryCaption caption = narrator.captions().get(0);
        assertThat(caption.sourceEventIds()).containsExactly(new EventId(42L));
        assertThat(caption.referencedSurvivorIds()).containsExactly(new SurvivorId(1));
        assertThat(caption.severity()).isEqualTo(EventSeverity.SIGNIFICANT);
        assertThat(caption.text()).contains("Alice");
    }

    @Test
    void deterministicTemplateSelectionForIdenticalSeeds() {
        SimEvent event = new SimEvent(
                new EventId(10L),
                4L,
                "SURVIVOR_EAT",
                List.of(new SurvivorId(1)),
                Optional.of(RegionId.REGION_1),
                EventSeverity.SIGNIFICANT,
                "Eats ration",
                Map.of(),
                List.of()
        );

        Narrator narrator1 = new Narrator(settlement, new SeededRandomFactory(999L));
        Narrator narrator2 = new Narrator(settlement, new SeededRandomFactory(999L));

        narrator1.onEvents(List.of(event));
        narrator2.onEvents(List.of(event));

        assertThat(narrator1.captions().get(0).text()).isEqualTo(narrator2.captions().get(0).text());
    }

    @Test
    void endToEndIntegrationWithTickCoordinator() {
        World world = new World(settlement);
        WorldClock clock = new WorldClock(4);
        RegionWorkerPool pool = new RegionWorkerPool();
        EventBus eventBus = new EventBus();
        eventBus.register(narrator);

        TickCoordinator coordinator = new TickCoordinator(
                world, clock,
                ctx -> new com.afterfall.behavior.BehaviorSelector().selectAction(ctx),
                pool, false, new SeededRandomFactory(42L),
                new CrisisManager(), new ObserverAI(), eventBus
        );

        try {
            TickResult result = coordinator.executeTick();
            assertThat(result.isSuccess()).isTrue();

            // Await async EventBus dispatch to narrator
            eventBus.shutdown();
            eventBus.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS);

            // EventBus fanned out events to narrator, generating captions
            assertThat(narrator.captions()).isNotEmpty();
            for (StoryCaption c : narrator.captions()) {
                assertThat(c.sourceEventIds()).isNotEmpty();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            coordinator.close();
            pool.close();
        }
    }
}
