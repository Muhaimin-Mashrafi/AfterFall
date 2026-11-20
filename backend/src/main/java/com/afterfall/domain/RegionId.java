package com.afterfall.domain;

import java.util.List;

/**
 * Stable identifier for one of the four processing regions.
 * Natural ordering by integer value (1 to 4) defines deterministic processing order.
 */
public record RegionId(int value) implements Comparable<RegionId> {

    public static final int MIN_REGION = 1;
    public static final int MAX_REGION = 4;

    public static final RegionId REGION_1 = new RegionId(1);
    public static final RegionId REGION_2 = new RegionId(2);
    public static final RegionId REGION_3 = new RegionId(3);
    public static final RegionId REGION_4 = new RegionId(4);

    public static final List<RegionId> ALL = List.of(REGION_1, REGION_2, REGION_3, REGION_4);

    public RegionId {
        if (value < MIN_REGION || value > MAX_REGION) {
            throw new IllegalArgumentException(
                    "RegionId must be between " + MIN_REGION + " and " + MAX_REGION + " but was " + value);
        }
    }

    @Override
    public int compareTo(RegionId other) {
        return Integer.compare(value, other.value);
    }
}
