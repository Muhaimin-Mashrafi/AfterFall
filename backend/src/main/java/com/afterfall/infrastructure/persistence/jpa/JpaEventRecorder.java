package com.afterfall.infrastructure.persistence.jpa;

import com.afterfall.application.persistence.EventRecorder;
import com.afterfall.application.persistence.RunId;
import com.afterfall.domain.EventId;
import com.afterfall.domain.RegionId;
import com.afterfall.domain.SurvivorId;
import com.afterfall.events.EventSeverity;
import com.afterfall.events.SimEvent;
import com.afterfall.infrastructure.persistence.jpa.entity.SimulationEventEntity;
import com.afterfall.infrastructure.persistence.jpa.repository.SpringDataSimulationEventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Spring Data JPA implementation of {@link EventRecorder} for PostgreSQL 17.
 *
 * <p>Persists simulation events into {@code simulation_events} idempotently by (run_id, event_id).
 */
@Component
public class JpaEventRecorder implements EventRecorder {

    private final SpringDataSimulationEventRepository repository;

    public JpaEventRecorder(SpringDataSimulationEventRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    @Transactional
    public void record(RunId runId, SimEvent event) {
        if (runId == null || event == null) {
            return;
        }
        SimulationEventEntity.EventCompositeKey key = new SimulationEventEntity.EventCompositeKey(runId.value(), event.id().value());
        if (repository.existsById(key)) {
            return;
        }
        repository.save(toEntity(runId.value(), event));
    }

    @Override
    @Transactional
    public void recordAll(RunId runId, List<SimEvent> events) {
        if (runId == null || events == null || events.isEmpty()) {
            return;
        }
        List<SimulationEventEntity> toSave = new ArrayList<>();
        for (SimEvent event : events) {
            if (event != null) {
                SimulationEventEntity.EventCompositeKey key = new SimulationEventEntity.EventCompositeKey(runId.value(), event.id().value());
                if (!repository.existsById(key)) {
                    toSave.add(toEntity(runId.value(), event));
                }
            }
        }
        if (!toSave.isEmpty()) {
            repository.saveAll(toSave);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimEvent> eventsForRun(RunId runId) {
        if (runId == null) {
            return Collections.emptyList();
        }
        List<SimulationEventEntity> entities = repository.findByRunIdOrderByEventIdAsc(runId.value());
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public void flush() {
        repository.flush();
    }

    private SimulationEventEntity toEntity(String runIdStr, SimEvent event) {
        String survivorsStr = event.targetSurvivors().stream()
                .map(s -> String.valueOf(s.value()))
                .collect(Collectors.joining(","));

        Integer loc = event.location().map(RegionId::value).orElse(null);

        String dataStr = event.data().entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(";"));

        String sourceRefsStr = event.sourceReferences().stream()
                .map(id -> String.valueOf(id.value()))
                .collect(Collectors.joining(","));

        return new SimulationEventEntity(
                runIdStr,
                event.id().value(),
                event.tick(),
                event.eventType(),
                survivorsStr,
                loc,
                event.severity().name(),
                event.summary(),
                dataStr,
                sourceRefsStr
        );
    }

    private SimEvent toDomain(SimulationEventEntity entity) {
        List<SurvivorId> survivors = Collections.emptyList();
        if (entity.getTargetSurvivors() != null && !entity.getTargetSurvivors().isBlank()) {
            survivors = Arrays.stream(entity.getTargetSurvivors().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> new SurvivorId(Integer.parseInt(s)))
                    .toList();
        }

        Optional<RegionId> loc = entity.getLocationRegion() != null
                ? Optional.of(new RegionId(entity.getLocationRegion()))
                : Optional.empty();

        Map<String, String> data = new HashMap<>();
        if (entity.getEventData() != null && !entity.getEventData().isBlank()) {
            for (String pair : entity.getEventData().split(";")) {
                int idx = pair.indexOf('=');
                if (idx > 0) {
                    data.put(pair.substring(0, idx), pair.substring(idx + 1));
                }
            }
        }

        List<EventId> sourceRefs = Collections.emptyList();
        if (entity.getSourceReferences() != null && !entity.getSourceReferences().isBlank()) {
            sourceRefs = Arrays.stream(entity.getSourceReferences().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> new EventId(Long.parseLong(s)))
                    .toList();
        }

        return new SimEvent(
                new EventId(entity.getEventId()),
                entity.getTick(),
                entity.getEventType(),
                survivors,
                loc,
                EventSeverity.valueOf(entity.getSeverity()),
                entity.getSummary(),
                data,
                sourceRefs
        );
    }
}
