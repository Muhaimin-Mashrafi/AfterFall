package com.afterfall.observer;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable audit record representing an Observer observation or intervention.
 *
 * <p>Captures tick, target, policy type, reason, intended effect, actual result,
 * status, and retains both the original and altered reports for Recovered Records (M11).
 */
public record InterventionRecord(
        long interventionId,
        long tick,
        InterventionType type,
        String targetDescription,
        String reason,
        String intendedEffect,
        String actualResult,
        InterventionStatus status,
        String originalReport,
        String alteredReport
) {
    public InterventionRecord {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(targetDescription, "targetDescription");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(intendedEffect, "intendedEffect");
        Objects.requireNonNull(actualResult, "actualResult");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(originalReport, "originalReport");
        Objects.requireNonNull(alteredReport, "alteredReport");
    }

    public static InterventionRecord from(Intervention intervention, InterventionStatus status, String actualResult) {
        String targetDesc = intervention.targetSurvivor()
                .map(s -> "Survivor #" + s.value())
                .or(() -> intervention.targetRegion().map(r -> "Region " + r.value()))
                .orElse("Settlement");

        return new InterventionRecord(
                intervention.id(),
                intervention.tick(),
                intervention.type(),
                targetDesc,
                intervention.reason(),
                intervention.intendedEffect(),
                actualResult,
                status,
                intervention.originalReport(),
                intervention.alteredReport()
        );
    }
}
