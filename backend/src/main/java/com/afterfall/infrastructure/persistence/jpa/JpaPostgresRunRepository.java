package com.afterfall.infrastructure.persistence.jpa;

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
import com.afterfall.infrastructure.persistence.jpa.entity.CaptionEntryEntity;
import com.afterfall.infrastructure.persistence.jpa.entity.ChoiceRecordEntity;
import com.afterfall.infrastructure.persistence.jpa.entity.InterventionRecordEntity;
import com.afterfall.infrastructure.persistence.jpa.entity.RunCheckpointEntity;
import com.afterfall.infrastructure.persistence.jpa.entity.SimulationRunEntity;
import com.afterfall.infrastructure.persistence.jpa.entity.StoryChapterEntity;
import com.afterfall.infrastructure.persistence.jpa.repository.SpringDataCaptionEntryRepository;
import com.afterfall.infrastructure.persistence.jpa.repository.SpringDataChoiceRecordRepository;
import com.afterfall.infrastructure.persistence.jpa.repository.SpringDataInterventionRecordRepository;
import com.afterfall.infrastructure.persistence.jpa.repository.SpringDataRunCheckpointRepository;
import com.afterfall.infrastructure.persistence.jpa.repository.SpringDataSimulationRunRepository;
import com.afterfall.infrastructure.persistence.jpa.repository.SpringDataStoryChapterRepository;
import com.afterfall.narrator.StoryCaption;
import com.afterfall.observer.InterventionRecord;
import com.afterfall.observer.InterventionStatus;
import com.afterfall.observer.InterventionType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Spring Data JPA implementation of {@link RunRepository} for PostgreSQL 17.
 */
@Component
public class JpaPostgresRunRepository implements RunRepository {

    private final SpringDataSimulationRunRepository runRepo;
    private final SpringDataRunCheckpointRepository checkpointRepo;
    private final SpringDataChoiceRecordRepository choiceRepo;
    private final SpringDataInterventionRecordRepository interventionRepo;
    private final SpringDataStoryChapterRepository chapterRepo;
    private final SpringDataCaptionEntryRepository captionRepo;

    public JpaPostgresRunRepository(SpringDataSimulationRunRepository runRepo,
                                    SpringDataRunCheckpointRepository checkpointRepo,
                                    SpringDataChoiceRecordRepository choiceRepo,
                                    SpringDataInterventionRecordRepository interventionRepo,
                                    SpringDataStoryChapterRepository chapterRepo,
                                    SpringDataCaptionEntryRepository captionRepo) {
        this.runRepo = Objects.requireNonNull(runRepo, "runRepo");
        this.checkpointRepo = Objects.requireNonNull(checkpointRepo, "checkpointRepo");
        this.choiceRepo = Objects.requireNonNull(choiceRepo, "choiceRepo");
        this.interventionRepo = Objects.requireNonNull(interventionRepo, "interventionRepo");
        this.chapterRepo = Objects.requireNonNull(chapterRepo, "chapterRepo");
        this.captionRepo = Objects.requireNonNull(captionRepo, "captionRepo");
    }

    @Override
    @Transactional
    public void saveRun(RunMetadata metadata) {
        if (metadata == null) return;
        SimulationRunEntity entity = new SimulationRunEntity(
                metadata.runId().value(),
                metadata.seed(),
                metadata.totalSurvivors(),
                metadata.startedAt(),
                metadata.endedAt(),
                metadata.endingChapter(),
                metadata.status()
        );
        runRepo.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RunMetadata> findRun(RunId runId) {
        if (runId == null) return Optional.empty();
        return runRepo.findById(runId.value()).map(e -> new RunMetadata(
                RunId.of(e.getRunId()),
                e.getSeed(),
                e.getTotalSurvivors(),
                e.getStartedAt(),
                e.getEndedAt(),
                e.getEndingChapter(),
                e.getStatus()
        ));
    }

    @Override
    @Transactional
    public void saveCheckpoint(RunCheckpoint cp) {
        if (cp == null) return;
        RunCheckpointEntity entity = new RunCheckpointEntity(
                cp.runId().value(),
                cp.tick(),
                cp.aliveSurvivors(),
                cp.deceasedSurvivors(),
                cp.foodQuantity(),
                cp.waterQuantity(),
                cp.medicineQuantity(),
                cp.currentChapter(),
                cp.recordedAt()
        );
        checkpointRepo.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RunCheckpoint> checkpointsForRun(RunId runId) {
        if (runId == null) return Collections.emptyList();
        return checkpointRepo.findByRunIdOrderByTickAsc(runId.value()).stream().map(e ->
                new RunCheckpoint(
                        RunId.of(e.getRunId()),
                        e.getTick(),
                        e.getAliveSurvivors(),
                        e.getDeceasedSurvivors(),
                        e.getFoodQuantity(),
                        e.getWaterQuantity(),
                        e.getMedicineQuantity(),
                        e.getCurrentChapter(),
                        e.getRecordedAt()
                )
        ).toList();
    }

    @Override
    @Transactional
    public void recordChoice(RunId runId, ChoiceRecord choice) {
        if (runId == null || choice == null) return;
        ChoiceRecordEntity.ChoiceCompositeKey key = new ChoiceRecordEntity.ChoiceCompositeKey(runId.value(), choice.choiceId());
        if (choiceRepo.existsById(key)) {
            return;
        }
        choiceRepo.save(new ChoiceRecordEntity(
                runId.value(),
                choice.choiceId(),
                choice.tick(),
                choice.chapterId(),
                choice.selectedOptionId(),
                choice.optionLabel(),
                choice.consequenceSummary(),
                choice.usedDefault()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChoiceRecord> choicesForRun(RunId runId) {
        if (runId == null) return Collections.emptyList();
        return choiceRepo.findByRunIdOrderByTickAsc(runId.value()).stream().map(e ->
                new ChoiceRecord(
                        e.getChoiceId(),
                        e.getTick(),
                        e.getChapterId(),
                        e.getSelectedOptionId(),
                        e.getOptionLabel(),
                        e.getConsequenceSummary(),
                        e.isUsedDefault()
                )
        ).toList();
    }

    @Override
    @Transactional
    public void recordIntervention(RunId runId, InterventionRecord ir) {
        if (runId == null || ir == null) return;
        InterventionRecordEntity.InterventionCompositeKey key = new InterventionRecordEntity.InterventionCompositeKey(runId.value(), ir.interventionId());
        if (interventionRepo.existsById(key)) {
            return;
        }
        interventionRepo.save(new InterventionRecordEntity(
                runId.value(),
                ir.interventionId(),
                ir.tick(),
                ir.type().name(),
                ir.targetDescription(),
                ir.reason(),
                ir.intendedEffect(),
                ir.actualResult(),
                ir.status().name(),
                ir.originalReport(),
                ir.alteredReport()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterventionRecord> interventionsForRun(RunId runId) {
        if (runId == null) return Collections.emptyList();
        return interventionRepo.findByRunIdOrderByTickAsc(runId.value()).stream().map(e ->
                new InterventionRecord(
                        e.getInterventionId(),
                        e.getTick(),
                        InterventionType.valueOf(e.getInterventionType()),
                        e.getTargetDescription(),
                        e.getReason(),
                        e.getIntendedEffect(),
                        e.getActualResult(),
                        InterventionStatus.valueOf(e.getStatus()),
                        e.getOriginalReport(),
                        e.getAlteredReport()
                )
        ).toList();
    }

    @Override
    @Transactional
    public void recordChapterTransition(RunId runId, ChapterTransition ct) {
        if (runId == null || ct == null) return;
        chapterRepo.save(new StoryChapterEntity(
                runId.value(),
                ct.tick(),
                ct.fromChapter().name(),
                ct.toChapter().name(),
                ct.triggerReason(),
                ct.isForced()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChapterTransition> chapterTransitionsForRun(RunId runId) {
        if (runId == null) return Collections.emptyList();
        return chapterRepo.findByRunIdOrderByTickAsc(runId.value()).stream().map(e ->
                new ChapterTransition(
                        e.getTick(),
                        ChapterType.valueOf(e.getFromChapter()),
                        ChapterType.valueOf(e.getToChapter()),
                        e.getTriggerReason(),
                        e.isForced()
                )
        ).toList();
    }

    @Override
    @Transactional
    public void recordCaption(RunId runId, StoryCaption sc) {
        if (runId == null || sc == null) return;
        CaptionEntryEntity.CaptionCompositeKey key = new CaptionEntryEntity.CaptionCompositeKey(runId.value(), sc.captionId());
        if (captionRepo.existsById(key)) {
            return;
        }
        String sourceRefs = sc.sourceEventIds().stream().map(id -> String.valueOf(id.value())).collect(Collectors.joining(","));
        String survivors = sc.referencedSurvivorIds().stream().map(id -> String.valueOf(id.value())).collect(Collectors.joining(","));

        captionRepo.save(new CaptionEntryEntity(
                runId.value(),
                sc.captionId(),
                sc.tick(),
                sc.text(),
                sc.severity().name(),
                sourceRefs,
                survivors
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryCaption> captionsForRun(RunId runId) {
        if (runId == null) return Collections.emptyList();
        return captionRepo.findByRunIdOrderByTickAsc(runId.value()).stream().map(e -> {
            List<EventId> sourceRefs = Collections.emptyList();
            if (e.getSourceEventIds() != null && !e.getSourceEventIds().isBlank()) {
                sourceRefs = Arrays.stream(e.getSourceEventIds().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(s -> new EventId(Long.parseLong(s)))
                        .toList();
            }
            List<SurvivorId> survivors = Collections.emptyList();
            if (e.getReferencedSurvivorIds() != null && !e.getReferencedSurvivorIds().isBlank()) {
                survivors = Arrays.stream(e.getReferencedSurvivorIds().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(s -> new SurvivorId(Integer.parseInt(s)))
                        .toList();
            }
            return new StoryCaption(
                    e.getCaptionId(),
                    e.getTick(),
                    e.getCaptionText(),
                    EventSeverity.valueOf(e.getSeverity()),
                    sourceRefs,
                    survivors
            );
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RunRecapInputs loadRecapInputs(RunId runId) {
        RunMetadata meta = findRun(runId).orElse(null);
        return new RunRecapInputs(
                runId,
                meta,
                chapterTransitionsForRun(runId),
                choicesForRun(runId),
                interventionsForRun(runId),
                captionsForRun(runId),
                checkpointsForRun(runId)
        );
    }
}
