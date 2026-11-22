package com.afterfall.simulation;

import com.afterfall.domain.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Immutable snapshot of the world at a specific tick.
 * Contains deterministic, defensively copied read-only views of regions, survivors,
 * and an explicit simulation world version.
 */
public record WorldView(
        long tick,
        long version,
        Map<RegionId, RegionView> regions,
        Map<SurvivorId, SurvivorView> survivors,
        Map<SurvivorId, RegionId> survivorRegionMap
) {
    public WorldView {
        Objects.requireNonNull(regions, "regions");
        Objects.requireNonNull(survivors, "survivors");
        Objects.requireNonNull(survivorRegionMap, "survivorRegionMap");

        // Defensively copy maps into explicitly ordered, immutable TreeMaps
        regions = Collections.unmodifiableMap(new TreeMap<>(regions));
        survivors = Collections.unmodifiableMap(new TreeMap<>(survivors));
        survivorRegionMap = Collections.unmodifiableMap(new TreeMap<>(survivorRegionMap));
    }

    /**
     * Backward-compatible constructor defaulting version to tick.
     */
    public WorldView(long tick, Map<RegionId, RegionView> regions, Map<SurvivorId, SurvivorView> survivors, Map<SurvivorId, RegionId> survivorRegionMap) {
        this(tick, tick, regions, survivors, survivorRegionMap);
    }

    /**
     * Creates an immutable view from the live {@link World} with default version = tick.
     */
    public static WorldView snapshot(World world, long tick) {
        return snapshot(world, tick, tick);
    }

    /**
     * Creates an immutable view from the live {@link World} with an explicit simulation version.
     */
    public static WorldView snapshot(World world, long tick, long version) {
        Settlement settlement = Objects.requireNonNull(world, "world").settlement();
        // Build region views in RegionId order
        Map<RegionId, RegionView> regionMap = new TreeMap<>();
        for (Region r : settlement.regions()) {
            regionMap.put(r.id(), new RegionView(r.id(), r.name(), List.copyOf(r.survivorIds())));
        }
        // Build survivor views and region index in SurvivorId order
        Map<SurvivorId, SurvivorView> survMap = new TreeMap<>();
        Map<SurvivorId, RegionId> survRegion = new TreeMap<>();
        for (Survivor s : settlement.survivors()) {
            survMap.put(s.id(), SurvivorView.from(s));
            survRegion.put(s.id(), s.location().orElseThrow(() -> new IllegalStateException("Survivor has no location: " + s.id())));
        }
        return new WorldView(tick, version, regionMap, survMap, survRegion);
    }
}
