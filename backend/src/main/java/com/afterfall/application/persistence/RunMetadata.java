package com.afterfall.application.persistence;

import java.time.Instant;
import java.util.Objects;

/**
 * Audit record of simulation run metadata.
 */
public record RunMetadata(
        RunId runId,
        long seed,
        int totalSurvivors,
        Instant startedAt,
        Instant endedAt,
        String endingChapter,
        String status
) {
    public RunMetadata {
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(status, "status");
    }

    public static RunMetadata started(RunId runId, long seed, int totalSurvivors, Instant startedAt) {
        return new RunMetadata(runId, seed, totalSurvivors, startedAt, null, null, "RUNNING");
    }

    public RunMetadata completed(Instant endedAt, String endingChapter, String status) {
        return new RunMetadata(this.runId, this.seed, this.totalSurvivors, this.startedAt, endedAt, endingChapter, status);
    }
}
