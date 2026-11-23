package com.afterfall.chapter;

import com.afterfall.choice.ChoiceConsequence;
import com.afterfall.choice.ChoiceCoordinator;
import com.afterfall.choice.ChoiceMoment;
import com.afterfall.choice.ChoiceOption;
import com.afterfall.domain.*;
import com.afterfall.events.EventBus;
import com.afterfall.narrator.Narrator;
import com.afterfall.observer.ObserverAI;
import com.afterfall.simulation.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChapterArcIntegrationTest {

    /**
     * Requirement: Twenty seeded runs completing all four chapters in order
     * without triggering the hard backstop.
     */
    @Test
    void twentySeededRunsCompleteAllFourChaptersWithoutForcedTransition() {
        for (long runSeed = 1001L; runSeed <= 1020L; runSeed++) {
            // Setup fresh settlement with 20 survivors
            Settlement settlement = new Settlement("Arc Run " + runSeed, Settlement.defaultRegions(), new ResourceStock(100, 100, 50));
            for (int i = 1; i <= 20; i++) {
                RegionId rId = RegionId.ALL.get((i - 1) % 4);
                Survivor s = new Survivor(
                        new SurvivorId(i), "Survivor " + i,
                        Personality.neutral(),
                        new Profession(ProfessionType.GENERAL_WORKER, 50),
                        Needs.fullySatisfied(),
                        Health.healthy(),
                        new Inventory(10)
                );
                settlement.registerSurvivor(s, rId);
            }

            World world = new World(settlement);
            WorldClock clock = new WorldClock(4);
            RegionWorkerPool pool = new RegionWorkerPool();
            EventBus eventBus = new EventBus();
            Narrator narrator = new Narrator(settlement, new SeededRandomFactory(runSeed));
            eventBus.register(narrator);

            // D_min = 2 ticks, D_soft = 6 ticks, D_hard = 20 ticks
            ChapterConfig config = new ChapterConfig(
                    2, 6, 20,
                    50, 2, 1, 50, 40, 40, 15, 25, 25, 1
            );
            ChapterManager chapterManager = new ChapterManager(config);
            ChoiceCoordinator choiceCoordinator = new ChoiceCoordinator();
            CrisisManager crisisManager = new CrisisManager();
            ObserverAI observerAI = new ObserverAI();

            TickCoordinator coordinator = new TickCoordinator(
                    world, clock,
                    ctx -> new com.afterfall.behavior.BehaviorSelector().selectAction(ctx),
                    pool, false, new SeededRandomFactory(runSeed),
                    crisisManager, observerAI, eventBus, choiceCoordinator, chapterManager
            );

            try {
                // Tick 0 -> 1
                coordinator.executeTick();
                // Tick 1 -> 2
                coordinator.executeTick();
                assertThat(chapterManager.currentChapter()).isEqualTo(ChapterType.CHAPTER_1_A_NEW_BEGINNING);

                // Overseer makes a choice that introduces visible discord
                ChoiceOption opt = new ChoiceOption("OPT_DISTORT", "Alter Rations", "Alters rations",
                        ChoiceConsequence.builder("Rations cut")
                                .resourceDelta(ResourceType.FOOD, -30)
                                .needDelta(Need.FOOD, -25)
                                .trustDelta(-15)
                                .targetSurvivors(List.of(new SurvivorId(1), new SurvivorId(2)))
                                .build(), true);
                ChoiceMoment moment = new ChoiceMoment("MOMENT_1", 2L, "CHAP_1", "Shortage", "Directives", List.of(opt), "OPT_DISTORT");
                choiceCoordinator.triggerChoice(moment);
                choiceCoordinator.submitDecision("MOMENT_1", "OPT_DISTORT");
                chapterManager.recordVisibleIntervention();

                // Tick 2 -> 3 (dwell = 2 - 0 = 2 >= minDwell 2 -> Transition to Ch 2)
                coordinator.executeTick();
                assertThat(chapterManager.currentChapter()).isEqualTo(ChapterType.CHAPTER_2_THE_FRACTURE);

                // Tick 3 -> 4
                coordinator.executeTick();
                // Tick 4 -> 5
                coordinator.executeTick();
                // Record evidence discovered
                chapterManager.recordEvidenceDiscovered();

                // Tick 5 -> 6 (dwell = 5 - 2 = 3 >= minDwell 2 -> Transition to Ch 3)
                coordinator.executeTick();
                assertThat(chapterManager.currentChapter()).isEqualTo(ChapterType.CHAPTER_3_THE_REVELATION);

                // Tick 6 -> 7
                coordinator.executeTick();
                // Tick 7 -> 8
                coordinator.executeTick();

                // Deplete population below P (15) by marking 8 dead (leaving 12 alive)
                int killed = 0;
                for (Survivor s : settlement.survivors()) {
                    if (killed < 8) {
                        s.markDead();
                        killed++;
                    }
                }

                // Tick 8 -> 9 (dwell = 8 - 5 = 3 >= minDwell 2 -> Transition to Ch 4)
                coordinator.executeTick();
                assertThat(chapterManager.currentChapter()).isEqualTo(ChapterType.CHAPTER_4_THE_LAST_RECORDING);

                // Verify that no transition was forced via hard backstop in any run
                List<ChapterTransition> history = chapterManager.transitionHistory();
                assertThat(history).hasSize(3);
                for (ChapterTransition t : history) {
                    assertThat(t.isForced()).isFalse();
                }

            } finally {
                coordinator.close();
                pool.close();
                eventBus.close();
            }
        }
    }

    @Test
    void deterministicReplayWithIdenticalSeedAndDecisionLogYieldsIdenticalOutcomes() {
        long seed = 777L;

        // Run 1
        String log1 = executeRun(seed, "OPT_A");
        // Run 2
        String log2 = executeRun(seed, "OPT_A");

        assertThat(log1).isEqualTo(log2);
    }

    private String executeRun(long seed, String chosenOption) {
        Settlement settlement = new Settlement("Replay Test", Settlement.defaultRegions(), new ResourceStock(50, 50, 20));
        for (int i = 1; i <= 20; i++) {
            Survivor s = new Survivor(new SurvivorId(i), "Survivor " + i, Personality.neutral(),
                    new Profession(ProfessionType.GENERAL_WORKER, 50), Needs.fullySatisfied(), Health.healthy(), new Inventory(10));
            settlement.registerSurvivor(s, RegionId.REGION_1);
        }

        World world = new World(settlement);
        WorldClock clock = new WorldClock(4);
        RegionWorkerPool pool = new RegionWorkerPool();
        EventBus eventBus = new EventBus();
        ChoiceCoordinator choiceCoordinator = new ChoiceCoordinator();
        ChapterManager chapterManager = new ChapterManager(new ChapterConfig(2, 6, 20, 50, 2, 1, 50, 40, 40, 15, 25, 25, 2));

        TickCoordinator coordinator = new TickCoordinator(
                world, clock,
                ctx -> new com.afterfall.behavior.BehaviorSelector().selectAction(ctx),
                pool, false, new SeededRandomFactory(seed),
                new CrisisManager(), new ObserverAI(), eventBus, choiceCoordinator, chapterManager
        );

        try {
            coordinator.executeTick();

            ChoiceOption optA = new ChoiceOption("OPT_A", "Conserve", "C", ChoiceConsequence.builder("Cons").resourceDelta(ResourceType.FOOD, 10).build(), true);
            ChoiceMoment moment = new ChoiceMoment("CH_1", 2L, "C1", "T", "P", List.of(optA), "OPT_A");
            choiceCoordinator.triggerChoice(moment);
            choiceCoordinator.submitDecision("CH_1", chosenOption);

            coordinator.executeTick();

            return settlement.resourceStock().quantity(ResourceType.FOOD) + ":" + chapterManager.currentChapter();
        } finally {
            coordinator.close();
            pool.close();
            eventBus.close();
        }
    }
}
