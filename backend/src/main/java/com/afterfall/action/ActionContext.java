package com.afterfall.action;

import com.afterfall.domain.RegionId;
import com.afterfall.domain.SurvivorSnapshot;
import java.util.Objects;

/**
 * Immutable input context for evaluating an {@link Action} command.
 * Does not expose mutable domain references.
 */
public record ActionContext(
        SurvivorSnapshot survivor,
        RegionId currentRegion,
        boolean foodAvailable,
        boolean medicineAvailable
) {
    public ActionContext {
        Objects.requireNonNull(survivor, "survivor");
        Objects.requireNonNull(currentRegion, "currentRegion");
    }

    public static ActionContext withDestination(SurvivorSnapshot survivor, RegionId currentRegion,
                                   boolean foodAvailable, boolean medicineAvailable,
                                   RegionId destinationRegion) {
        // Destination is no longer stored; this method exists for backward compatibility.
        // The destinationRegion parameter is ignored because ActionContext no longer carries it.
        return new ActionContext(survivor, currentRegion, foodAvailable, medicineAvailable);
    }
}
