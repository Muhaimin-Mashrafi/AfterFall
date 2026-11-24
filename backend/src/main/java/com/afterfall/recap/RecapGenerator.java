package com.afterfall.recap;

import com.afterfall.application.persistence.RunCheckpoint;
import com.afterfall.application.persistence.RunRecapInputs;
import com.afterfall.chapter.ChapterTransition;
import com.afterfall.choice.ChoiceRecord;
import com.afterfall.domain.SurvivorSnapshot;
import com.afterfall.domain.SurvivorStatus;
import com.afterfall.observer.InterventionRecord;

import java.util.*;

/**
 * Domain generator synthesizing the immutable {@link RunRecap} from persisted {@link RunRecapInputs}.
 *
 * <p>Crucial architectural correction: does NOT depend on a mutable live Settlement instance.
 * It consumes immutable inputs (including {@link SurvivorSnapshot}s and {@link RunCheckpoint}s),
 * ensuring total reproducibility after Spring Boot application restarts.
 */
public class RecapGenerator {

    private final EvidenceService evidenceService;
    private final FinalRecordingGenerator recordingGenerator;

    public RecapGenerator() {
        this(new EvidenceService(), new FinalRecordingGenerator());
    }

    public RecapGenerator(EvidenceService evidenceService, FinalRecordingGenerator recordingGenerator) {
        this.evidenceService = Objects.requireNonNull(evidenceService, "evidenceService");
        this.recordingGenerator = Objects.requireNonNull(recordingGenerator, "recordingGenerator");
    }

    /**
     * Deterministically generates the comprehensive run recap.
     */
    public RunRecap generate(RunRecapInputs inputs, Set<String> discoveredEvidenceIds) {
        Objects.requireNonNull(inputs, "inputs");
        Set<String> discovered = discoveredEvidenceIds != null ? discoveredEvidenceIds : Collections.emptySet();

        String runIdStr = inputs.runId().value();
        String endingChapter = inputs.metadata() != null ? inputs.metadata().endingChapter() : "CHAPTER_1_A_NEW_BEGINNING";

        // Macro metrics from checkpoints or metadata
        int initialPop = inputs.metadata() != null ? inputs.metadata().totalSurvivors() : 25;
        int finalAlive = 0;
        int deceasedCount = 0;
        long totalTicks = 0L;
        Map<String, Integer> finalResources = new LinkedHashMap<>();

        if (!inputs.checkpoints().isEmpty()) {
            RunCheckpoint latestCp = inputs.checkpoints().get(inputs.checkpoints().size() - 1);
            totalTicks = latestCp.tick();
            finalAlive = latestCp.aliveSurvivors();
            deceasedCount = latestCp.deceasedSurvivors();
            finalResources.put("food", latestCp.foodQuantity());
            finalResources.put("water", latestCp.waterQuantity());
            finalResources.put("medicine", latestCp.medicineQuantity());
        } else {
            finalResources.put("food", 50);
            finalResources.put("water", 50);
            finalResources.put("medicine", 20);
        }

        // Survivor outcomes from snapshots (if present)
        List<RunRecap.SurvivorOutcome> survivorOutcomes = new ArrayList<>();
        if (!inputs.finalSurvivorSnapshots().isEmpty()) {
            finalAlive = 0;
            deceasedCount = 0;
            for (SurvivorSnapshot s : inputs.finalSurvivorSnapshots()) {
                boolean alive = (s.status() == SurvivorStatus.ALIVE);
                if (alive) finalAlive++; else deceasedCount++;
                String fate = alive ? "Endured in shelter" : "Fell silent during the crisis";
                survivorOutcomes.add(new RunRecap.SurvivorOutcome(
                        s.id().value(),
                        s.name(),
                        s.profession().displayName(),
                        alive,
                        s.vitality(),
                        fate
                ));
            }
        }

        // Crisis timeline from captions or checkpoints
        List<RunRecap.CrisisTimelineEntry> crisisTimeline = new ArrayList<>();
        for (RunCheckpoint cp : inputs.checkpoints()) {
            if (cp.foodQuantity() <= 10) {
                crisisTimeline.add(new RunRecap.CrisisTimelineEntry(cp.tick(), "FAMINE", "Severe food shortage recorded in stockpiles."));
            }
        }

        // Choices summaries
        List<RunRecap.ChoiceSummary> choiceSummaries = new ArrayList<>();
        for (ChoiceRecord cr : inputs.choices()) {
            choiceSummaries.add(new RunRecap.ChoiceSummary(
                    cr.choiceId(),
                    cr.tick(),
                    "Decision in " + cr.chapterId(),
                    cr.optionLabel(),
                    cr.consequenceSummary()
            ));
        }

        // Observer intervention summaries
        List<RunRecap.InterventionSummary> interventionSummaries = new ArrayList<>();
        for (InterventionRecord ir : inputs.interventions()) {
            interventionSummaries.add(new RunRecap.InterventionSummary(
                    ir.interventionId(),
                    ir.tick(),
                    ir.type().name(),
                    ir.targetDescription(),
                    ir.status().name(),
                    ir.reason()
            ));
        }

        // Chapter progression entries
        List<RunRecap.ChapterProgressionEntry> chapterProgression = new ArrayList<>();
        for (ChapterTransition ct : inputs.chapterTransitions()) {
            chapterProgression.add(new RunRecap.ChapterProgressionEntry(
                    ct.tick(),
                    ct.fromChapter().name(),
                    ct.toChapter().name(),
                    ct.triggerReason()
            ));
        }

        // Extract available evidence
        List<EvidenceItem> availableEvidence = evidenceService.extractAvailableEvidence(inputs.interventions());

        // Generate final recording
        FinalRecording finalRecording = recordingGenerator.generate(inputs);

        return new RunRecap(
                runIdStr,
                totalTicks,
                endingChapter,
                initialPop,
                finalAlive,
                deceasedCount,
                finalResources,
                survivorOutcomes,
                crisisTimeline,
                choiceSummaries,
                interventionSummaries,
                chapterProgression,
                availableEvidence,
                new ArrayList<>(discovered),
                finalRecording
        );
    }
}
