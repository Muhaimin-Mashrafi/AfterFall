package com.afterfall.recap;

import com.afterfall.application.persistence.RunCheckpoint;
import com.afterfall.application.persistence.RunId;
import com.afterfall.application.persistence.RunMetadata;
import com.afterfall.application.persistence.RunRecapInputs;
import com.afterfall.chapter.ChapterTransition;
import com.afterfall.chapter.ChapterType;
import com.afterfall.choice.ChoiceRecord;
import com.afterfall.domain.*;
import com.afterfall.observer.InterventionRecord;
import com.afterfall.observer.InterventionStatus;
import com.afterfall.observer.InterventionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RecapGeneratorTest {

    private final RecapGenerator generator = new RecapGenerator();

    @Test
    @DisplayName("Deterministically synthesizes RunRecap with accurate population and crisis counts")
    void testRecapAccuracyAndDeterminism() {
        RunId runId = RunId.of("run-recap-test");
        RunMetadata meta = new RunMetadata(runId, 42L, 25, Instant.now(), null, "CHAPTER_4_THE_LAST_RECORDING", "COMPLETED");

        Map<ItemType, Integer> items = new java.util.EnumMap<>(ItemType.class);
        for (ItemType item : ItemType.values()) {
            items.put(item, 0);
        }

        List<SurvivorSnapshot> snapshots = List.of(
                new SurvivorSnapshot(new SurvivorId(1), "Sarah Chen", Personality.neutral(), new Profession(ProfessionType.MEDIC, 70),
                        Map.of(Need.FOOD, 80, Need.WATER, 80, Need.REST, 80, Need.SAFETY, 80), 90, HealthCondition.HEALTHY,
                        items, 10, Optional.of(RegionId.REGION_1), SurvivorStatus.ALIVE),
                new SurvivorSnapshot(new SurvivorId(2), "Lucas Drake", Personality.neutral(), new Profession(ProfessionType.GUARD, 60),
                        Map.of(Need.FOOD, 10, Need.WATER, 10, Need.REST, 10, Need.SAFETY, 10), 0, HealthCondition.HEALTHY,
                        items, 10, Optional.of(RegionId.REGION_2), SurvivorStatus.DEAD)
        );

        List<RunCheckpoint> checkpoints = List.of(
                new RunCheckpoint(runId, 10L, 25, 0, 40, 40, 20, "CHAPTER_1_A_NEW_BEGINNING", Instant.now()),
                new RunCheckpoint(runId, 20L, 24, 1, 5, 30, 15, "CHAPTER_2_THE_FRACTURE", Instant.now())
        );

        List<ChoiceRecord> choices = List.of(
                new ChoiceRecord("c1", 12L, "CHAPTER_2_THE_FRACTURE", "opt-a", "Ration food", "Consequences logged", false)
        );

        List<InterventionRecord> interventions = List.of(
                new InterventionRecord(101L, 15L, InterventionType.INFORMATION_DISTORTION, "Survivor #1", "Reason", "Effect", "Result",
                        InterventionStatus.APPLIED, "Food ok", "Food critical")
        );

        List<ChapterTransition> transitions = List.of(
                new ChapterTransition(10L, ChapterType.CHAPTER_1_A_NEW_BEGINNING, ChapterType.CHAPTER_2_THE_FRACTURE, "Trust drop", false)
        );

        RunRecapInputs inputs = new RunRecapInputs(
                runId, meta, transitions, choices, interventions, Collections.emptyList(), checkpoints, snapshots
        );

        RunRecap recap1 = generator.generate(inputs, Set.of("EV-101"));
        RunRecap recap2 = generator.generate(inputs, Set.of("EV-101"));

        assertEquals(recap1.runId(), recap2.runId());
        assertEquals(recap1.finalAliveSurvivors(), recap2.finalAliveSurvivors());
        assertEquals(1, recap1.finalAliveSurvivors());
        assertEquals(1, recap1.totalDeceasedSurvivors());
        assertEquals(1, recap1.crisisTimeline().size());
        assertEquals(1, recap1.choices().size());
        assertEquals(1, recap1.availableEvidence().size());
        assertTrue(recap1.discoveredEvidenceIds().contains("EV-101"));
        assertEquals(FinalRecording.MANDATORY_CLOSING_LINE, recap1.finalRecording().closingLine());
    }
}
