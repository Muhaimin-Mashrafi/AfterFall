package com.afterfall.behavior;

import com.afterfall.domain.RegionId;
import com.afterfall.domain.SurvivorSnapshot;
import java.util.List;
import java.util.Objects;

/**
 * Immutable decision context passed to {@link Behavior} strategies.
 * Provides read-only snapshot data and local environmental indicators.
 */
public record BehaviorContext(
        SurvivorSnapshot survivor,
        RegionId currentRegion,
        boolean foodAvailable,
        boolean medicineAvailable,
        List<RegionId> candidateRegions
) {

    public BehaviorContext {
        Objects.requireNonNull(survivor, "survivor");
        Objects.requireNonNull(currentRegion, "currentRegion");
        Objects.requireNonNull(candidateRegions, "candidateRegions");
        candidateRegions = List.copyOf(candidateRegions);
    }

    public static BehaviorContext of(SurvivorSnapshot survivor, RegionId currentRegion,
                                     boolean foodAvailable, boolean medicineAvailable) {
        return new BehaviorContext(survivor, currentRegion, foodAvailable, medicineAvailable, List.of());
    }

    public static BehaviorContext withCandidates(SurvivorSnapshot survivor, RegionId currentRegion,
                                                 boolean foodAvailable, boolean medicineAvailable,
                                                 List<RegionId> candidateRegions) {
        return new BehaviorContext(survivor, currentRegion, foodAvailable, medicineAvailable, candidateRegions);
    }
}
