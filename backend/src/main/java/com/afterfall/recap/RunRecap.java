package com.afterfall.recap;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable consolidated documentary summary of a simulation run.
 *
 * <p>Contains initial vs final population, survivor outcomes, resource trajectories,
 * crisis timeline, choice consequences, observer interventions, discovered evidence,
 * chapter transitions, and the final documentary recording.
 */
public record RunRecap(
        String runId,
        long totalTicks,
        String endingChapter,
        int initialPopulation,
        int finalAliveSurvivors,
        int totalDeceasedSurvivors,
        Map<String, Integer> finalResources,
        List<SurvivorOutcome> survivorOutcomes,
        List<CrisisTimelineEntry> crisisTimeline,
        List<ChoiceSummary> choices,
        List<InterventionSummary> interventions,
        List<ChapterProgressionEntry> chapterArc,
        List<EvidenceItem> availableEvidence,
        List<String> discoveredEvidenceIds,
        FinalRecording finalRecording
) {
    public RunRecap {
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(endingChapter, "endingChapter");
        Objects.requireNonNull(finalResources, "finalResources");
        survivorOutcomes = List.copyOf(Objects.requireNonNull(survivorOutcomes, "survivorOutcomes"));
        crisisTimeline = List.copyOf(Objects.requireNonNull(crisisTimeline, "crisisTimeline"));
        choices = List.copyOf(Objects.requireNonNull(choices, "choices"));
        interventions = List.copyOf(Objects.requireNonNull(interventions, "interventions"));
        chapterArc = List.copyOf(Objects.requireNonNull(chapterArc, "chapterArc"));
        availableEvidence = List.copyOf(Objects.requireNonNull(availableEvidence, "availableEvidence"));
        discoveredEvidenceIds = List.copyOf(Objects.requireNonNull(discoveredEvidenceIds, "discoveredEvidenceIds"));
        Objects.requireNonNull(finalRecording, "finalRecording");
    }

    public record SurvivorOutcome(
            int survivorId,
            String name,
            String profession,
            boolean alive,
            int health,
            String fateDescription
    ) {}

    public record CrisisTimelineEntry(
            long tick,
            String crisisType,
            String description
    ) {}

    public record ChoiceSummary(
            String choiceId,
            long tick,
            String promptOrTitle,
            String chosenOptionLabel,
            String consequenceSummary
    ) {}

    public record InterventionSummary(
            long id,
            long tick,
            String policy,
            String target,
            String status,
            String reason
    ) {}

    public record ChapterProgressionEntry(
            long tick,
            String fromChapter,
            String toChapter,
            String triggerReason
    ) {}
}
