package com.afterfall.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "simulation_runs")
public class SimulationRunEntity {

    @Id
    @Column(name = "run_id", nullable = false, length = 64)
    private String runId;

    @Column(name = "seed", nullable = false)
    private long seed;

    @Column(name = "total_survivors", nullable = false)
    private int totalSurvivors;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "ending_chapter", length = 64)
    private String endingChapter;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    public SimulationRunEntity() {}

    public SimulationRunEntity(String runId, long seed, int totalSurvivors, Instant startedAt, Instant endedAt, String endingChapter, String status) {
        this.runId = runId;
        this.seed = seed;
        this.totalSurvivors = totalSurvivors;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.endingChapter = endingChapter;
        this.status = status;
    }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }
    public int getTotalSurvivors() { return totalSurvivors; }
    public void setTotalSurvivors(int totalSurvivors) { this.totalSurvivors = totalSurvivors; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public String getEndingChapter() { return endingChapter; }
    public void setEndingChapter(String endingChapter) { this.endingChapter = endingChapter; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
