package com.afterfall.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Aggregate root for the human population and regional structure of the settlement.
 *
 * <p>Invariants:
 * <ul>
 *   <li>Maintains exactly four processing regions ({@link RegionId#ALL}).</li>
 *   <li>Every registered survivor belongs to <em>exactly one</em> region.</li>
 *   <li>Rejects duplicate survivor registration, duplicate region membership, unknown survivor IDs,
 *       and invalid region assignments.</li>
 *   <li>Maintains deterministic iteration order: survivors ordered by {@link SurvivorId},
 *       regions ordered by {@link RegionId}.</li>
 * </ul>
 */
public final class Settlement {

    private final String name;
    private final Map<RegionId, Region> regions = new TreeMap<>();
    private final Map<SurvivorId, Survivor> survivors = new TreeMap<>();
    private final Map<SurvivorId, RegionId> survivorRegionIndex = new HashMap<>();

    public Settlement(String name) {
        this(name, defaultRegions());
    }

    public Settlement(String name, Collection<Region> initialRegions) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.name = name.strip();
        Objects.requireNonNull(initialRegions, "initialRegions");

        for (Region r : initialRegions) {
            Objects.requireNonNull(r, "region");
            if (regions.put(r.id(), r) != null) {
                throw new IllegalArgumentException("Duplicate region id: " + r.id());
            }
        }

        // Must define exactly the four MUST regions
        if (regions.size() != RegionId.MAX_REGION) {
            throw new IllegalArgumentException(
                    "Settlement must have exactly " + RegionId.MAX_REGION + " regions, but had " + regions.size());
        }
        for (RegionId required : RegionId.ALL) {
            if (!regions.containsKey(required)) {
                throw new IllegalArgumentException("Missing required region: " + required);
            }
        }
    }

    public static Collection<Region> defaultRegions() {
        return RegionId.ALL.stream()
                .map(id -> new Region(id, "Quadrant " + id.value()))
                .toList();
    }

    public String name() {
        return name;
    }

    public int population() {
        return survivors.size();
    }

    /**
     * Registers a survivor and assigns them to the specified region.
     * Enforces that the survivor belongs to exactly this region.
     */
    public void registerSurvivor(Survivor survivor, RegionId regionId) {
        Objects.requireNonNull(survivor, "survivor");
        Objects.requireNonNull(regionId, "regionId");

        Region region = regions.get(regionId);
        if (region == null) {
            throw new IllegalArgumentException("Unknown region: " + regionId);
        }

        SurvivorId sId = survivor.id();
        if (survivors.containsKey(sId) || survivorRegionIndex.containsKey(sId)) {
            throw new IllegalStateException("Survivor " + sId + " is already registered in settlement");
        }

        // Survivor must not already have a location (rejects survivors already registered in a settlement)
        if (survivor.location().isPresent()) {
            throw new IllegalStateException(
                    "Survivor " + sId + " already has an assigned location: " + survivor.location().get());
        }

        // Reject if region roster already contains this SurvivorId
        if (region.contains(sId)) {
            throw new IllegalStateException(
                    "Region " + regionId + " roster already contains survivor ID " + sId);
        }

        // State mutations only after all validations pass
        survivors.put(sId, survivor);
        survivorRegionIndex.put(sId, regionId);
        region.addSurvivor(sId);
        survivor.setLocation(regionId);
    }

    public Optional<Survivor> survivor(SurvivorId id) {
        return Optional.ofNullable(survivors.get(Objects.requireNonNull(id, "id")));
    }

    public boolean contains(SurvivorId id) {
        return survivors.containsKey(Objects.requireNonNull(id, "id"));
    }

    /** Unmodifiable view of registered survivors, ordered deterministically by SurvivorId. */
    public Collection<Survivor> survivors() {
        return Collections.unmodifiableCollection(survivors.values());
    }

    public Optional<Region> region(RegionId id) {
        return Optional.ofNullable(regions.get(Objects.requireNonNull(id, "id")));
    }

    /** Unmodifiable view of regions, ordered deterministically by RegionId. */
    public Collection<Region> regions() {
        return Collections.unmodifiableCollection(regions.values());
    }

    public Optional<RegionId> regionOf(SurvivorId survivorId) {
        return Optional.ofNullable(survivorRegionIndex.get(Objects.requireNonNull(survivorId, "survivorId")));
    }

    /** Package-private: updates the location index when a survivor is migrated centrally. */
    void updateSurvivorRegionIndex(SurvivorId survivorId, RegionId newRegionId) {
        Objects.requireNonNull(survivorId, "survivorId");
        Objects.requireNonNull(newRegionId, "newRegionId");
        survivorRegionIndex.put(survivorId, newRegionId);
    }

    /** Returns unmodifiable set of SurvivorIds in the given region. */
    public Set<SurvivorId> survivorsIn(RegionId regionId) {
        Region region = regions.get(Objects.requireNonNull(regionId, "regionId"));
        if (region == null) {
            throw new IllegalArgumentException("Unknown region: " + regionId);
        }
        return region.survivorIds();
    }
}
