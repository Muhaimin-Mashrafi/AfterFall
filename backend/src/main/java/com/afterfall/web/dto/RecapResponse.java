package com.afterfall.web.dto;

import com.afterfall.recap.FinalRecording;
import com.afterfall.recap.RunRecap;

import java.util.List;
import java.util.Map;

public record RecapResponse(
        String runId,
        long totalTicks,
        String endingChapter,
        int initialPopulation,
        int finalAliveSurvivors,
        int totalDeceasedSurvivors,
        Map<String, Integer> finalResources,
        List<RunRecap.SurvivorOutcome> survivorOutcomes,
        List<RunRecap.CrisisTimelineEntry> crisisTimeline,
        List<RunRecap.ChoiceSummary> choices,
        List<RunRecap.InterventionSummary> interventions,
        List<RunRecap.ChapterProgressionEntry> chapterArc,
        List<EvidenceDto> availableEvidence,
        List<String> discoveredEvidenceIds,
        FinalRecording finalRecording
) {}
