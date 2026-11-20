package com.afterfall.domain;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * One of the four processing regions in the settlement.
 *
 * <p>In the MUST scope, a Region maintains an ordered set of {@link SurvivorId}s and minimal
 * region-local state. It performs no graphical rendering or coordinate math. Iteration over
 * survivor IDs is deterministic (sorted by {@link SurvivorId} natural order).
 */
public final class Region {

    private final RegionId id;
    private final String name;
    private final Set<SurvivorId> survivorIds = new TreeSet<>();

    public Region(RegionId id, String name) {
        this.id = Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.name = name.strip();
    }

    public RegionId id() {
        return id;
    }

    public String name() {
        return name;
    }

    /** Unmodifiable view of survivor IDs, sorted in natural deterministic order. */
    public Set<SurvivorId> survivorIds() {
        return Collections.unmodifiableSet(survivorIds);
    }

    public int population() {
        return survivorIds.size();
    }

    public boolean contains(SurvivorId survivorId) {
        return survivorIds.contains(Objects.requireNonNull(survivorId, "survivorId"));
    }

    /** Package-private: membership is managed exclusively through Settlement. */
    void addSurvivor(SurvivorId survivorId) {
        Objects.requireNonNull(survivorId, "survivorId");
        if (!survivorIds.add(survivorId)) {
            throw new IllegalStateException(
                    "Survivor " + survivorId + " is already present in region " + id);
        }
    }

    /** Package-private: membership is managed exclusively through Settlement. */
    void removeSurvivor(SurvivorId survivorId) {
        Objects.requireNonNull(survivorId, "survivorId");
        if (!survivorIds.remove(survivorId)) {
            throw new IllegalStateException(
                    "Survivor " + survivorId + " is not in region " + id);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Region other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Region[" + id.value() + ": " + name + ", pop=" + survivorIds.size() + "]";
    }
}
