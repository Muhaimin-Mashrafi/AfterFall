package com.afterfall.application.persistence;

import java.time.Instant;
import java.util.Objects;

/**
 * Lightweight periodic simulation state checkpoint.
 *
 * <p>Persists macro metrics (tick, population alive, resource stockpiles, current chapter)
 * without persisting every survivor mutation on every tick.
 */
public record RunCheckpoint(
        RunId runId,
        long tick,
        int aliveSurvivors,
        int deceasedSurvivors,
        int foodQuantity,
        int waterQuantity,
        int medicineQuantity,
        String currentChapter,
        Instant recordedAt
) {
    public RunCheckpoint {
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(currentChapter, "currentChapter");
        Objects.requireNonNull(recordedAt, "recordedAt");
    }
}
