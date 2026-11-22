package com.afterfall.simulation;

import com.afterfall.domain.RegionId;
import java.util.List;
import java.util.Objects;

/**
 * Immutable view of a Region for a specific tick.
 * Contains the region id, name and an ordered, defensively copied list of resident survivor IDs.
 */
public record RegionView(
        RegionId id,
        String name,
        List<com.afterfall.domain.SurvivorId> survivorIds
) {
    public RegionView {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(survivorIds, "survivorIds");
        survivorIds = List.copyOf(survivorIds);
    }
}
