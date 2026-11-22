package com.afterfall.simulation;

import com.afterfall.domain.RegionId;
import com.afterfall.domain.SurvivorId;
import java.util.Objects;

/**
 * Immutable migration request proposed by a survivor action during tick evaluation.
 *
 * <p>Preserves the survivor ID, originating source region, destination region,
 * and deterministic action sequence number within the tick.
 * Authoritative migration and roster mutation are deferred to M4.
 */
public record MigrationRequest(
        SurvivorId survivorId,
        RegionId sourceRegion,
        RegionId destinationRegion,
        int actionSequence
) {
    public MigrationRequest {
        Objects.requireNonNull(survivorId, "survivorId");
        Objects.requireNonNull(sourceRegion, "sourceRegion");
        Objects.requireNonNull(destinationRegion, "destinationRegion");
        if (actionSequence <= 0) {
            throw new IllegalArgumentException("actionSequence must be positive");
        }
    }
}
