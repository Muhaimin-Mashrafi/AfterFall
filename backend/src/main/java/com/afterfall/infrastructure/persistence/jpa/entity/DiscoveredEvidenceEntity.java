package com.afterfall.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "discovered_evidence", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"run_id", "evidence_id"})
})
public class DiscoveredEvidenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private String runId;

    @Column(name = "evidence_id", nullable = false)
    private String evidenceId;

    @Column(name = "tick", nullable = false)
    private Long tick;

    @Column(name = "policy", nullable = false)
    private String policy;

    @Column(name = "target", nullable = false)
    private String target;

    @Column(name = "original_report", nullable = false, length = 2000)
    private String originalReport;

    @Column(name = "altered_report", nullable = false, length = 2000)
    private String alteredReport;

    @Column(name = "discrepancy_type", nullable = false)
    private String discrepancyType;

    @Column(name = "source_event_ids")
    private String sourceEventIds;

    @Column(name = "referenced_survivors")
    private String referencedSurvivors;

    @Column(name = "is_discovered", nullable = false)
    private Boolean isDiscovered;

    @Column(name = "discovered_at")
    private Instant discoveredAt;

    public DiscoveredEvidenceEntity() {}

    public DiscoveredEvidenceEntity(String runId, String evidenceId, Long tick, String policy, String target,
                                  String originalReport, String alteredReport, String discrepancyType,
                                  String sourceEventIds, String referencedSurvivors, Boolean isDiscovered,
                                  Instant discoveredAt) {
        this.runId = runId;
        this.evidenceId = evidenceId;
        this.tick = tick;
        this.policy = policy;
        this.target = target;
        this.originalReport = originalReport;
        this.alteredReport = alteredReport;
        this.discrepancyType = discrepancyType;
        this.sourceEventIds = sourceEventIds;
        this.referencedSurvivors = referencedSurvivors;
        this.isDiscovered = isDiscovered;
        this.discoveredAt = discoveredAt;
    }

    public Long getId() { return id; }
    public String getRunId() { return runId; }
    public String getEvidenceId() { return evidenceId; }
    public Long getTick() { return tick; }
    public String getPolicy() { return policy; }
    public String getTarget() { return target; }
    public String getOriginalReport() { return originalReport; }
    public String getAlteredReport() { return alteredReport; }
    public String getDiscrepancyType() { return discrepancyType; }
    public String getSourceEventIds() { return sourceEventIds; }
    public String getReferencedSurvivors() { return referencedSurvivors; }
    public Boolean getIsDiscovered() { return isDiscovered; }
    public void setIsDiscovered(Boolean isDiscovered) { this.isDiscovered = isDiscovered; }
    public Instant getDiscoveredAt() { return discoveredAt; }
    public void setDiscoveredAt(Instant discoveredAt) { this.discoveredAt = discoveredAt; }
}
