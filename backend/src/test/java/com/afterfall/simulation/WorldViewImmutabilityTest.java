package com.afterfall.simulation;

import com.afterfall.domain.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class WorldViewImmutabilityTest {

    @Test
    void testWorldViewAndRegionViewImmutability() {
        RegionId r1 = RegionId.REGION_1;
        List<SurvivorId> mutableSurvivorIds = new ArrayList<>();
        mutableSurvivorIds.add(new SurvivorId(1));
        mutableSurvivorIds.add(new SurvivorId(2));

        RegionView regionView = new RegionView(r1, "Quadrant 1", mutableSurvivorIds);

        // 1. Modifying source list does not affect RegionView
        mutableSurvivorIds.add(new SurvivorId(3));
        assertEquals(2, regionView.survivorIds().size(), "RegionView must defensively copy survivorIds");

        // 2. Modifying exposed list from RegionView must throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> regionView.survivorIds().add(new SurvivorId(4)));

        // 3. WorldView maps immutability and defensive copying
        Map<RegionId, RegionView> regionMap = new HashMap<>();
        regionMap.put(r1, regionView);

        SurvivorView sView = new SurvivorView(
                new SurvivorId(1),
                "Survivor 1",
                Personality.neutral(),
                new Profession(ProfessionType.FARMER, 50),
                Map.of(Need.FOOD, 50, Need.WATER, 50, Need.REST, 50, Need.SAFETY, 50),
                80,
                HealthCondition.HEALTHY,
                Map.of(ItemType.FOOD, 2),
                20,
                Optional.of(r1),
                SurvivorStatus.ALIVE
        );

        Map<SurvivorId, SurvivorView> survMap = new HashMap<>();
        survMap.put(new SurvivorId(1), sView);

        Map<SurvivorId, RegionId> survRegionMap = new HashMap<>();
        survRegionMap.put(new SurvivorId(1), r1);

        WorldView worldView = new WorldView(1L, 1L, regionMap, survMap, survRegionMap);

        // Modifying source maps after construction
        regionMap.put(RegionId.REGION_2, new RegionView(RegionId.REGION_2, "Quadrant 2", List.of()));
        assertEquals(1, worldView.regions().size(), "WorldView must defensively copy regionMap");

        survMap.put(new SurvivorId(2), sView);
        assertEquals(1, worldView.survivors().size(), "WorldView must defensively copy survMap");

        // Exposed maps are unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> worldView.regions().remove(r1));
        assertThrows(UnsupportedOperationException.class, () -> worldView.survivors().remove(new SurvivorId(1)));
        assertThrows(UnsupportedOperationException.class, () -> worldView.survivorRegionMap().remove(new SurvivorId(1)));

        // Exposed nested maps in SurvivorView are unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> sView.needLevels().put(Need.FOOD, 100));
        assertThrows(UnsupportedOperationException.class, () -> sView.inventoryQuantities().put(ItemType.MEDICINE, 10));
    }
}
