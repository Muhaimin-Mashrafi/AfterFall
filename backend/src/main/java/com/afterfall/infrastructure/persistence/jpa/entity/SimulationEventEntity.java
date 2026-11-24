package com.afterfall.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "simulation_events", indexes = {
        @Index(name = "idx_event_run_id", columnList = "run_id, event_id")
})
@IdClass(SimulationEventEntity.EventCompositeKey.class)
public class SimulationEventEntity {

    public static class EventCompositeKey implements Serializable {
        private String runId;
        private Long eventId;

        public EventCompositeKey() {}
        public EventCompositeKey(String runId, Long eventId) {
            this.runId = runId;
            this.eventId = eventId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EventCompositeKey that = (EventCompositeKey) o;
            return Objects.equals(runId, that.runId) && Objects.equals(eventId, that.eventId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(runId, eventId);
        }
    }

    @Id
    @Column(name = "run_id", nullable = false, length = 64)
    private String runId;

    @Id
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "tick", nullable = false)
    private long tick;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "target_survivors", columnDefinition = "TEXT")
    private String targetSurvivors;

    @Column(name = "location_region")
    private Integer locationRegion;

    @Column(name = "severity", nullable = false, length = 32)
    private String severity;

    @Column(name = "summary", nullable = false, length = 512)
    private String summary;

    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;

    @Column(name = "source_references", columnDefinition = "TEXT")
    private String sourceReferences;

    public SimulationEventEntity() {}

    public SimulationEventEntity(String runId, Long eventId, long tick, String eventType, String targetSurvivors, Integer locationRegion, String severity, String summary, String eventData, String sourceReferences) {
        this.runId = runId;
        this.eventId = eventId;
        this.tick = tick;
        this.eventType = eventType;
        this.targetSurvivors = targetSurvivors;
        this.locationRegion = locationRegion;
        this.severity = severity;
        this.summary = summary;
        this.eventData = eventData;
        this.sourceReferences = sourceReferences;
    }

    public String getRunId() { return runId; }
    public Long getEventId() { return eventId; }
    public long getTick() { return tick; }
    public String getEventType() { return eventType; }
    public String getTargetSurvivors() { return targetSurvivors; }
    public Integer getLocationRegion() { return locationRegion; }
    public String getSeverity() { return severity; }
    public String getSummary() { return summary; }
    public String getEventData() { return eventData; }
    public String getSourceReferences() { return sourceReferences; }
}
