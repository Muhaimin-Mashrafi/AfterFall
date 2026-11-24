package com.afterfall.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "run_checkpoints", indexes = {
        @Index(name = "idx_checkpoint_run_tick", columnList = "run_id, tick")
})
public class RunCheckpointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false, length = 64)
    private String runId;

    @Column(name = "tick", nullable = false)
    private long tick;

    @Column(name = "alive_survivors", nullable = false)
    private int aliveSurvivors;

    @Column(name = "deceased_survivors", nullable = false)
    private int deceasedSurvivors;

    @Column(name = "food_quantity", nullable = false)
    private int foodQuantity;

    @Column(name = "water_quantity", nullable = false)
    private int waterQuantity;

    @Column(name = "medicine_quantity", nullable = false)
    private int medicineQuantity;

    @Column(name = "current_chapter", nullable = false, length = 64)
    private String currentChapter;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public RunCheckpointEntity() {}

    public RunCheckpointEntity(String runId, long tick, int aliveSurvivors, int deceasedSurvivors, int foodQuantity, int waterQuantity, int medicineQuantity, String currentChapter, Instant recordedAt) {
        this.runId = runId;
        this.tick = tick;
        this.aliveSurvivors = aliveSurvivors;
        this.deceasedSurvivors = deceasedSurvivors;
        this.foodQuantity = foodQuantity;
        this.waterQuantity = waterQuantity;
        this.medicineQuantity = medicineQuantity;
        this.currentChapter = currentChapter;
        this.recordedAt = recordedAt;
    }

    public Long getId() { return id; }
    public String getRunId() { return runId; }
    public long getTick() { return tick; }
    public int getAliveSurvivors() { return aliveSurvivors; }
    public int getDeceasedSurvivors() { return deceasedSurvivors; }
    public int getFoodQuantity() { return foodQuantity; }
    public int getWaterQuantity() { return waterQuantity; }
    public int getMedicineQuantity() { return medicineQuantity; }
    public String getCurrentChapter() { return currentChapter; }
    public Instant getRecordedAt() { return recordedAt; }
}
