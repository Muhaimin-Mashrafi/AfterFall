package com.afterfall.application.persistence;

import com.afterfall.chapter.ChapterTransition;
import com.afterfall.choice.ChoiceRecord;
import com.afterfall.domain.SurvivorSnapshot;
import com.afterfall.narrator.StoryCaption;
import com.afterfall.observer.InterventionRecord;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregated input data required to generate the final simulation documentary recap (M11).
 *
 * <p>Now includes an immutable list of {@link SurvivorSnapshot}s representing the final state,
 * eliminating any runtime dependency on a mutable live Settlement instance.
 */
public record RunRecapInputs(
        RunId runId,
        RunMetadata metadata,
        List<ChapterTransition> chapterTransitions,
        List<ChoiceRecord> choices,
        List<InterventionRecord> interventions,
        List<StoryCaption> captions,
        List<RunCheckpoint> checkpoints,
        List<SurvivorSnapshot> finalSurvivorSnapshots
) {
    public RunRecapInputs(
            RunId runId,
            RunMetadata metadata,
            List<ChapterTransition> chapterTransitions,
            List<ChoiceRecord> choices,
            List<InterventionRecord> interventions,
            List<StoryCaption> captions,
            List<RunCheckpoint> checkpoints
    ) {
        this(runId, metadata, chapterTransitions, choices, interventions, captions, checkpoints, Collections.emptyList());
    }

    public RunRecapInputs {
        Objects.requireNonNull(runId, "runId");
        chapterTransitions = List.copyOf(Objects.requireNonNull(chapterTransitions, "chapterTransitions"));
        choices = List.copyOf(Objects.requireNonNull(choices, "choices"));
        interventions = List.copyOf(Objects.requireNonNull(interventions, "interventions"));
        captions = List.copyOf(Objects.requireNonNull(captions, "captions"));
        checkpoints = List.copyOf(Objects.requireNonNull(checkpoints, "checkpoints"));
        finalSurvivorSnapshots = List.copyOf(Objects.requireNonNull(finalSurvivorSnapshots, "finalSurvivorSnapshots"));
    }
}
