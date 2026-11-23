package com.afterfall.chapter;

import com.afterfall.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ChapterSystemTest {

    private Settlement settlement;
    private ChapterManager chapterManager;

    @BeforeEach
    void setUp() {
        settlement = new Settlement("Chapter Test", Settlement.defaultRegions(), new ResourceStock(50, 50, 20));
        // Register 20 survivors
        for (int i = 1; i <= 20; i++) {
            Survivor s = new Survivor(
                    new SurvivorId(i), "Survivor " + i,
                    Personality.neutral(),
                    new Profession(ProfessionType.GENERAL_WORKER, 50),
                    Needs.fullySatisfied(),
                    Health.healthy(),
                    new Inventory(10)
            );
            settlement.registerSurvivor(s, RegionId.REGION_1);
        }

        // Test config with minDwell = 4, soft = 10, hard = 20
        ChapterConfig config = new ChapterConfig(
                4, 10, 20,
                50, 4, 2, 60, 40, 40, 15, 25, 25, 6
        );
        chapterManager = new ChapterManager(config);
    }

    @Test
    void minimumDwellPreventsPrematureTransitionEvenIfConditionMet() {
        // Trigger condition: visible intervention
        chapterManager.recordVisibleIntervention();

        // At tick 2 (< minDwell 4), transition should NOT fire
        Optional<ChapterTransition> trans = chapterManager.evaluateTick(settlement, 2L, false);
        assertThat(trans).isEmpty();
        assertThat(chapterManager.currentChapter()).isEqualTo(ChapterType.CHAPTER_1_A_NEW_BEGINNING);

        // At tick 4 (>= minDwell 4), transition should fire
        Optional<ChapterTransition> transAt4 = chapterManager.evaluateTick(settlement, 4L, false);
        assertThat(transAt4).isPresent();
        assertThat(transAt4.get().toChapter()).isEqualTo(ChapterType.CHAPTER_2_THE_FRACTURE);
        assertThat(chapterManager.currentChapter()).isEqualTo(ChapterType.CHAPTER_2_THE_FRACTURE);
    }

    @Test
    void pressureEscalationBecomesActiveAfterSoftDwell() {
        assertThat(chapterManager.isPressureEscalationActive(5L)).isFalse();
        assertThat(chapterManager.isPressureEscalationActive(10L)).isTrue();
    }

    @Test
    void hardBackstopForcesTransitionAtHardLimit() {
        // Advance without meeting specific conditions until hard limit (20)
        for (long t = 1; t < 20; t++) {
            chapterManager.evaluateTick(settlement, t, false);
        }

        // At tick 20, hard backstop forces transition
        Optional<ChapterTransition> forcedTrans = chapterManager.evaluateTick(settlement, 20L, false);
        assertThat(forcedTrans).isPresent();
        assertThat(forcedTrans.get().isForced()).isTrue();
        assertThat(forcedTrans.get().toChapter()).isEqualTo(ChapterType.CHAPTER_2_THE_FRACTURE);
        assertThat(forcedTrans.get().triggerReason()).contains("Hard backstop triggered");
    }

    @Test
    void sequentialThresholdTransitionsThroughAllFourChapters() {
        // Ch 1 -> Ch 2 via visible intervention
        chapterManager.recordVisibleIntervention();
        Optional<ChapterTransition> toCh2 = chapterManager.evaluateTick(settlement, 4L, false);
        assertThat(toCh2).isPresent();
        assertThat(chapterManager.currentChapter()).isEqualTo(ChapterType.CHAPTER_2_THE_FRACTURE);

        // Ch 2 -> Ch 3 via discovered evidence
        chapterManager.recordEvidenceDiscovered();
        chapterManager.recordEvidenceDiscovered(); // evidenceCount >= 2
        Optional<ChapterTransition> toCh3 = chapterManager.evaluateTick(settlement, 9L, false); // dwell = 9 - 4 = 5 >= 4
        assertThat(toCh3).isPresent();
        assertThat(chapterManager.currentChapter()).isEqualTo(ChapterType.CHAPTER_3_THE_REVELATION);

        // Ch 3 -> Ch 4 via active population < P (15)
        // Kill 10 survivors so active population = 10 < 15
        int killed = 0;
        for (Survivor s : settlement.survivors()) {
            if (killed < 10) {
                s.markDead();
                killed++;
            }
        }
        Optional<ChapterTransition> toCh4 = chapterManager.evaluateTick(settlement, 14L, false); // dwell = 14 - 9 = 5 >= 4
        assertThat(toCh4).isPresent();
        assertThat(chapterManager.currentChapter()).isEqualTo(ChapterType.CHAPTER_4_THE_LAST_RECORDING);
        assertThat(toCh4.get().isForced()).isFalse();
    }
}
