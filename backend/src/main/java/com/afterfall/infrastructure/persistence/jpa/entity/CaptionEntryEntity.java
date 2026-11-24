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
@Table(name = "caption_entries", indexes = {
        @Index(name = "idx_caption_run_tick", columnList = "run_id, tick")
})
@IdClass(CaptionEntryEntity.CaptionCompositeKey.class)
public class CaptionEntryEntity {

    public static class CaptionCompositeKey implements Serializable {
        private String runId;
        private Long captionId;

        public CaptionCompositeKey() {}
        public CaptionCompositeKey(String runId, Long captionId) {
            this.runId = runId;
            this.captionId = captionId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CaptionCompositeKey that = (CaptionCompositeKey) o;
            return Objects.equals(runId, that.runId) && Objects.equals(captionId, that.captionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(runId, captionId);
        }
    }

    @Id
    @Column(name = "run_id", nullable = false, length = 64)
    private String runId;

    @Id
    @Column(name = "caption_id", nullable = false)
    private Long captionId;

    @Column(name = "tick", nullable = false)
    private long tick;

    @Column(name = "caption_text", nullable = false, length = 1024)
    private String captionText;

    @Column(name = "severity", nullable = false, length = 32)
    private String severity;

    @Column(name = "source_event_ids", columnDefinition = "TEXT")
    private String sourceEventIds;

    @Column(name = "referenced_survivor_ids", columnDefinition = "TEXT")
    private String referencedSurvivorIds;

    public CaptionEntryEntity() {}

    public CaptionEntryEntity(String runId, Long captionId, long tick, String captionText, String severity, String sourceEventIds, String referencedSurvivorIds) {
        this.runId = runId;
        this.captionId = captionId;
        this.tick = tick;
        this.captionText = captionText;
        this.severity = severity;
        this.sourceEventIds = sourceEventIds;
        this.referencedSurvivorIds = referencedSurvivorIds;
    }

    public String getRunId() { return runId; }
    public Long getCaptionId() { return captionId; }
    public long getTick() { return tick; }
    public String getCaptionText() { return captionText; }
    public String getSeverity() { return severity; }
    public String getSourceEventIds() { return sourceEventIds; }
    public String getReferencedSurvivorIds() { return referencedSurvivorIds; }
}
