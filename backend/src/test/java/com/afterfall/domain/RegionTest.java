package com.afterfall.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RegionTest {

    @Test
    void createsRegionWithDeterministicOrdering() {
        Region r1 = new Region(RegionId.REGION_1, "North");
        Region r2 = new Region(RegionId.REGION_2, "East");
        assertEquals(RegionId.REGION_1, r1.id());
        assertEquals("North", r1.name());
        assertEquals(0, r1.population());
        assertTrue(r1.id().compareTo(r2.id()) < 0);
    }

    @Test
    void validatesRegionIdBounds() {
        assertEquals(1, RegionId.REGION_1.value());
        assertEquals(4, RegionId.REGION_4.value());
        assertEquals(4, RegionId.ALL.size());
        assertThrows(IllegalArgumentException.class, () -> new RegionId(0));
        assertThrows(IllegalArgumentException.class, () -> new RegionId(5));
    }

    @Test
    void validatesRegionName() {
        assertThrows(IllegalArgumentException.class, () -> new Region(RegionId.REGION_1, ""));
        assertThrows(IllegalArgumentException.class, () -> new Region(RegionId.REGION_1, "   "));
        assertThrows(NullPointerException.class, () -> new Region(RegionId.REGION_1, null));
        assertThrows(NullPointerException.class, () -> new Region(null, "Valid"));
    }

    @Test
    void encapsulatesSurvivorIdsCollection() {
        Region region = new Region(RegionId.REGION_1, "North");
        region.addSurvivor(new SurvivorId(2));
        region.addSurvivor(new SurvivorId(1));

        assertEquals(2, region.population());
        assertTrue(region.contains(new SurvivorId(1)));
        assertFalse(region.contains(new SurvivorId(3)));

        // Natural order deterministic iteration
        List<SurvivorId> ids = List.copyOf(region.survivorIds());
        assertEquals(new SurvivorId(1), ids.get(0));
        assertEquals(new SurvivorId(2), ids.get(1));

        // Unmodifiable
        assertThrows(UnsupportedOperationException.class,
                () -> region.survivorIds().add(new SurvivorId(3)));
    }

    @Test
    void duplicateAndUnknownSurvivorOperationsThrow() {
        Region region = new Region(RegionId.REGION_1, "North");
        region.addSurvivor(new SurvivorId(1));
        assertThrows(IllegalStateException.class, () -> region.addSurvivor(new SurvivorId(1)));
        assertThrows(IllegalStateException.class, () -> region.removeSurvivor(new SurvivorId(2)));
    }
}
