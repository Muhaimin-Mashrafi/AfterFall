package com.afterfall.domain;

/** Stable survivor identity. Natural order (by value) is the deterministic merge order. */
public record SurvivorId(int value) implements Comparable<SurvivorId> {

    public SurvivorId {
        if (value <= 0) {
            throw new IllegalArgumentException("SurvivorId must be positive but was " + value);
        }
    }

    @Override
    public int compareTo(SurvivorId other) {
        return Integer.compare(value, other.value);
    }
}
