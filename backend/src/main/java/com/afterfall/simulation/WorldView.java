package com.afterfall.simulation;

import com.afterfall.domain.*;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Immutable snapshot of the world at a specific tick.
 * Contains deterministic, defensively copied read-only views of regions, survivors,
 * resource quantities, and an explicit simulation world version.
 */
public record WorldView(
        long tick,
        long version,
        Map<RegionId, RegionView> regions,
        Map<SurvivorId, SurvivorView> survivors,
        Map<SurvivorId, RegionId> survivorRegionMap,
        Map<ResourceType, Integer> resourceQuantities
) {
    public WorldView {
        Objects.requireNonNull(regions, "regions");
        Objects.requireNonNull(survivors, "survivors");
        Objects.requireNonNull(survivorRegionMap, "survivorRegionMap");
        Objects.requireNonNull(resourceQuantities, "resourceQuantities");

        // Defensively copy maps into explicitly ordered, immutable TreeMaps
        regions = Collections.unmodifiableMap(new TreeMap<>(regions));
        survivors = Collections.unmodifiableMap(new TreeMap<>(survivors));
        survivorRegionMap = Collections.unmodifiableMap(new TreeMap<>(survivorRegionMap));
        resourceQuantities = Collections.unmodifiableMap(new EnumMap<>(resourceQuantities));
    }

    /**
     * Backward-compatible constructor without resource quantities (zero for all types).
     */
    public WorldView(long tick, long version,
                     Map<RegionId, RegionView> regions,
                     Map<SurvivorId, SurvivorView> survivors,
                     Map<SurvivorId, RegionId> survivorRegionMap) {
        this(tick, version, regions, survivors, survivorRegionMap, emptyResourceMap());
    }

    /**
     * Backward-compatible constructor defaulting version to tick.
     */
    public WorldView(long tick, Map<RegionId, RegionView> regions,
                     Map<SurvivorId, SurvivorView> survivors,
                     Map<SurvivorId, RegionId> survivorRegionMap) {
        this(tick, tick, regions, survivors, survivorRegionMap);
    }

    /** Whether any food is in the settlement resource pool. */
    public boolean isFoodAvailable() {
        return resourceQuantities.getOrDefault(ResourceType.FOOD, 0) > 0;
    }

    /** Whether any medicine is in the settlement resource pool. */
    public boolean isMedicineAvailable() {
        return resourceQuantities.getOrDefault(ResourceType.MEDICINE, 0) > 0;
    }

    private static Map<ResourceType, Integer> emptyResourceMap() {
        Map<ResourceType, Integer> map = new EnumMap<>(ResourceType.class);
        for (ResourceType t : ResourceType.values()) {
            map.put(t, 0);
        }
        return map;
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
        // Snapshot resource quantities
        ResourceStock stock = settlement.resourceStock();
        Map<ResourceType, Integer> resMap = new EnumMap<>(ResourceType.class);
        for (ResourceType t : ResourceType.values()) {
            resMap.put(t, stock.quantity(t));
        }
        return new WorldView(tick, version, regionMap, survMap, survRegion, resMap);
    }
}

