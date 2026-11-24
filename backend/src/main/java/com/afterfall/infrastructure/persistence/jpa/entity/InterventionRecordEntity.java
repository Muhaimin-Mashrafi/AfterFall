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
@Table(name = "ai_interventions", indexes = {
        @Index(name = "idx_intervention_run_tick", columnList = "run_id, tick")
})
@IdClass(InterventionRecordEntity.InterventionCompositeKey.class)
public class InterventionRecordEntity {

    public static class InterventionCompositeKey implements Serializable {
        private String runId;
        private Long interventionId;

        public InterventionCompositeKey() {}
        public InterventionCompositeKey(String runId, Long interventionId) {
            this.runId = runId;
            this.interventionId = interventionId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InterventionCompositeKey that = (InterventionCompositeKey) o;
            return Objects.equals(runId, that.runId) && Objects.equals(interventionId, that.interventionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(runId, interventionId);
        }
    }

    @Id
    @Column(name = "run_id", nullable = false, length = 64)
    private String runId;

    @Id
    @Column(name = "intervention_id", nullable = false)
    private Long interventionId;

    @Column(name = "tick", nullable = false)
    private long tick;

    @Column(name = "intervention_type", nullable = false, length = 64)
    private String interventionType;

    @Column(name = "target_description", nullable = false, length = 128)
    private String targetDescription;

    @Column(name = "reason", nullable = false, length = 512)
    private String reason;

    @Column(name = "intended_effect", nullable = false, length = 512)
    private String intendedEffect;

    @Column(name = "actual_result", nullable = false, length = 512)
    private String actualResult;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "original_report", columnDefinition = "TEXT")
    private String originalReport;

    @Column(name = "altered_report", columnDefinition = "TEXT")
    private String alteredReport;

    public InterventionRecordEntity() {}

    public InterventionRecordEntity(String runId, Long interventionId, long tick, String interventionType, String targetDescription, String reason, String intendedEffect, String actualResult, String status, String originalReport, String alteredReport) {
        this.runId = runId;
        this.interventionId = interventionId;
        this.tick = tick;
        this.interventionType = interventionType;
        this.targetDescription = targetDescription;
        this.reason = reason;
        this.intendedEffect = intendedEffect;
        this.actualResult = actualResult;
        this.status = status;
        this.originalReport = originalReport;
        this.alteredReport = alteredReport;
    }

    public String getRunId() { return runId; }
    public Long getInterventionId() { return interventionId; }
    public long getTick() { return tick; }
    public String getInterventionType() { return interventionType; }
    public String getTargetDescription() { return targetDescription; }
    public String getReason() { return reason; }
    public String getIntendedEffect() { return intendedEffect; }
    public String getActualResult() { return actualResult; }
    public String getStatus() { return status; }
    public String getOriginalReport() { return originalReport; }
    public String getAlteredReport() { return alteredReport; }
}
