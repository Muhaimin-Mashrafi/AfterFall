package com.afterfall.infrastructure.persistence;

import com.afterfall.application.persistence.RunCheckpoint;
import com.afterfall.application.persistence.RunId;
import com.afterfall.application.persistence.RunMetadata;
import com.afterfall.application.persistence.RunRecapInputs;
import com.afterfall.application.persistence.RunRepository;
import com.afterfall.chapter.ChapterTransition;
import com.afterfall.chapter.ChapterType;
import com.afterfall.choice.ChoiceRecord;
import com.afterfall.domain.EventId;
import com.afterfall.domain.SurvivorId;
import com.afterfall.events.EventSeverity;
import com.afterfall.narrator.StoryCaption;
import com.afterfall.observer.InterventionRecord;
import com.afterfall.observer.InterventionStatus;
import com.afterfall.observer.InterventionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reusable contract test suite for {@link RunRepository} implementations.
 */
public abstract class AbstractRunRepositoryContractTest {

    protected abstract RunRepository createRepository();

    private RunRepository repository;
    private RunId runId;

    @BeforeEach
    void setUp() {
        repository = createRepository();
        runId = RunId.of("run-repo-" + System.nanoTime());
    }

    @Test
    void savesAndRetrievesRunMetadata() {
        Instant now = Instant.now();
        RunMetadata meta = RunMetadata.started(runId, 42L, 25, now);
        repository.saveRun(meta);

        Optional<RunMetadata> retrieved = repository.findRun(runId);
        assertTrue(retrieved.isPresent());
        assertEquals(42L, retrieved.get().seed());
        assertEquals(25, retrieved.get().totalSurvivors());
        assertEquals("RUNNING", retrieved.get().status());

        RunMetadata completed = retrieved.get().completed(now.plusSeconds(300), "THE_LAST_RECORDING", "TRAGIC_CONCLUSION");
        repository.saveRun(completed);

        Optional<RunMetadata> updated = repository.findRun(runId);
        assertTrue(updated.isPresent());
        assertEquals("TRAGIC_CONCLUSION", updated.get().status());
        assertEquals("THE_LAST_RECORDING", updated.get().endingChapter());
    }

    @Test
    void recordsAndRetrievesCheckpointsInTickOrder() {
        Instant now = Instant.now();
        RunCheckpoint cp2 = new RunCheckpoint(runId, 20, 20, 5, 80, 70, 40, "THE_FRACTURE", now.plusSeconds(20));
        RunCheckpoint cp1 = new RunCheckpoint(runId, 10, 25, 0, 100, 90, 50, "A_NEW_BEGINNING", now.plusSeconds(10));

        repository.saveCheckpoint(cp2);
        repository.saveCheckpoint(cp1);

        List<RunCheckpoint> list = repository.checkpointsForRun(runId);
        assertEquals(2, list.size());
        assertEquals(10L, list.get(0).tick());
        assertEquals(20L, list.get(1).tick());
        assertEquals(25, list.get(0).aliveSurvivors());
        assertEquals(20, list.get(1).aliveSurvivors());
    }

    @Test
    void recordsChoicesIdempotently() {
        ChoiceRecord c1 = new ChoiceRecord("choice-1", 15, "A_NEW_BEGINNING", "opt-a", "Ration food", "Food saved", false);
        ChoiceRecord dup = new ChoiceRecord("choice-1", 15, "A_NEW_BEGINNING", "opt-b", "Feast", "Food wasted", true);

        repository.recordChoice(runId, c1);
        repository.recordChoice(runId, dup);

        List<ChoiceRecord> choices = repository.choicesForRun(runId);
        assertEquals(1, choices.size());
        assertEquals("opt-a", choices.get(0).selectedOptionId());
    }

    @Test
    void recordsInterventionsIdempotently() {
        InterventionRecord i1 = new InterventionRecord(
                101L, 12, InterventionType.INFORMATION_DISTORTION, "Survivor #1",
                "Erode trust", "Decrease solidarity", "Trust decreased by 15",
                InterventionStatus.APPLIED, "Full stockpiles reported", "Shortage reported"
        );
        InterventionRecord dup = new InterventionRecord(
                101L, 12, InterventionType.TRUST_EROSION, "Survivor #2",
                "Other", "Other", "Other",
                InterventionStatus.REJECTED, "Other", "Other"
        );

        repository.recordIntervention(runId, i1);
        repository.recordIntervention(runId, dup);

        List<InterventionRecord> list = repository.interventionsForRun(runId);
        assertEquals(1, list.size());
        assertEquals(101L, list.get(0).interventionId());
        assertEquals(InterventionType.INFORMATION_DISTORTION, list.get(0).type());
    }

    @Test
    void recordsChapterTransitionsAndCaptions() {
        ChapterTransition trans = new ChapterTransition(25, ChapterType.CHAPTER_1_A_NEW_BEGINNING, ChapterType.CHAPTER_2_THE_FRACTURE, "Trust fell below 50", false);
        repository.recordChapterTransition(runId, trans);

        StoryCaption caption = new StoryCaption(501L, 25, "The silence deepened.", EventSeverity.SIGNIFICANT, List.of(new EventId(1)), List.of(new SurvivorId(1)));
        repository.recordCaption(runId, caption);

        List<ChapterTransition> chapters = repository.chapterTransitionsForRun(runId);
        assertEquals(1, chapters.size());
        assertEquals(ChapterType.CHAPTER_2_THE_FRACTURE, chapters.get(0).toChapter());

        List<StoryCaption> captions = repository.captionsForRun(runId);
        assertEquals(1, captions.size());
        assertEquals("The silence deepened.", captions.get(0).text());
    }

    @Test
    void loadsRecapInputs() {
        RunMetadata meta = RunMetadata.started(runId, 99L, 25, Instant.now());
        repository.saveRun(meta);

        ChoiceRecord choice = new ChoiceRecord("choice-1", 10, "CHAPTER_1_A_NEW_BEGINNING", "opt-1", "Label", "Consequence", false);
        repository.recordChoice(runId, choice);

        ChapterTransition trans = new ChapterTransition(10, ChapterType.CHAPTER_1_A_NEW_BEGINNING, ChapterType.CHAPTER_2_THE_FRACTURE, "Reason", false);
        repository.recordChapterTransition(runId, trans);

        RunRecapInputs recap = repository.loadRecapInputs(runId);
        assertNotNull(recap);
        assertEquals(runId, recap.runId());
        assertNotNull(recap.metadata());
        assertEquals(1, recap.choices().size());
        assertEquals(1, recap.chapterTransitions().size());
    }
}
