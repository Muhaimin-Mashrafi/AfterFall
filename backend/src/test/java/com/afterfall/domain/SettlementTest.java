package com.afterfall.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SettlementTest {

    private Survivor sampleSurvivor(int id) {
        return new Survivor(
                new SurvivorId(id),
                "Survivor " + id,
                Personality.neutral(),
                new Profession(ProfessionType.FARMER, 50),
                Needs.fullySatisfied(),
                Health.healthy(),
                new Inventory(5)
        );
    }

    @Test
    void requiresExactlyFourRegions() {
        Settlement settlement = new Settlement("Haven");
        assertEquals(4, settlement.regions().size());

        List<Region> tooFew = List.of(
                new Region(RegionId.REGION_1, "Q1"),
                new Region(RegionId.REGION_2, "Q2")
        );
        assertThrows(IllegalArgumentException.class, () -> new Settlement("Haven", tooFew));
    }

    @Test
    void registersSurvivorToExactlyOneRegion() {
        Settlement settlement = new Settlement("Haven");
        Survivor s1 = sampleSurvivor(1);
        settlement.registerSurvivor(s1, RegionId.REGION_1);

        assertEquals(1, settlement.population());
        assertTrue(settlement.contains(new SurvivorId(1)));
        assertEquals(RegionId.REGION_1, settlement.regionOf(new SurvivorId(1)).orElseThrow());
        assertEquals(RegionId.REGION_1, s1.location().orElseThrow());

        // In region 1, not in region 2
        assertTrue(settlement.survivorsIn(RegionId.REGION_1).contains(new SurvivorId(1)));
        assertFalse(settlement.survivorsIn(RegionId.REGION_2).contains(new SurvivorId(1)));
    }

    @Test
    void rejectsDuplicateSurvivorRegistration() {
        Settlement settlement = new Settlement("Haven");
        Survivor s1 = sampleSurvivor(1);
        settlement.registerSurvivor(s1, RegionId.REGION_1);

        // Attempting to register again in same or different region throws
        assertThrows(IllegalStateException.class,
                () -> settlement.registerSurvivor(s1, RegionId.REGION_1));
        assertThrows(IllegalStateException.class,
                () -> settlement.registerSurvivor(s1, RegionId.REGION_2));

        Survivor s1Duplicate = sampleSurvivor(1);
        assertThrows(IllegalStateException.class,
                () -> settlement.registerSurvivor(s1Duplicate, RegionId.REGION_2));
    }

    @Test
    void rejectsSurvivorAlreadyHavingLocationAndPreventsDualSettlementRegistration() {
        Settlement s1 = new Settlement("Haven Alpha");
        Settlement s2 = new Settlement("Haven Beta");
        Survivor survivor = sampleSurvivor(1);

        // Initially has no location
        assertTrue(survivor.location().isEmpty());

        // Successfully registered in first settlement
        s1.registerSurvivor(survivor, RegionId.REGION_1);
        assertEquals(RegionId.REGION_1, survivor.location().orElseThrow());

        // Attempting to register in another settlement (even with identical region) throws IllegalStateException
        assertThrows(IllegalStateException.class,
                () -> s2.registerSurvivor(survivor, RegionId.REGION_1));
        assertThrows(IllegalStateException.class,
                () -> s2.registerSurvivor(survivor, RegionId.REGION_2));

        // s2 remains clean and unaffected
        assertEquals(0, s2.population());
        assertFalse(s2.contains(survivor.id()));
        assertFalse(s2.survivorsIn(RegionId.REGION_1).contains(survivor.id()));
    }

    @Test
    void rejectsPreExistingRegionRosterEntryWithoutMutatingState() {
        Settlement settlement = new Settlement("Haven");
        // Access region directly to simulate pre-existing roster entry
        Region region = settlement.region(RegionId.REGION_1).orElseThrow();
        region.addSurvivor(new SurvivorId(1));

        Survivor survivor = sampleSurvivor(1);
        assertThrows(IllegalStateException.class,
                () -> settlement.registerSurvivor(survivor, RegionId.REGION_1));

        // Survivor location, settlement survivors map and region index remain unmutated
        assertTrue(survivor.location().isEmpty());
        assertEquals(0, settlement.population());
        assertFalse(settlement.contains(new SurvivorId(1)));
        assertTrue(settlement.regionOf(new SurvivorId(1)).isEmpty());
    }

    @Test
    void rejectsUnknownRegionOrNulls() {
        Settlement settlement = new Settlement("Haven");
        Survivor s = sampleSurvivor(1);
        assertThrows(NullPointerException.class, () -> settlement.registerSurvivor(null, RegionId.REGION_1));
        assertThrows(NullPointerException.class, () -> settlement.registerSurvivor(s, null));
        assertThrows(NullPointerException.class, () -> settlement.contains(null));
        assertThrows(NullPointerException.class, () -> settlement.region(null));
        assertThrows(NullPointerException.class, () -> settlement.survivor(null));
        assertThrows(NullPointerException.class, () -> settlement.regionOf(null));
        assertThrows(NullPointerException.class, () -> settlement.survivorsIn(null));
    }

    @Test
    void maintainsDeterministicOrderAndEncapsulation() {
        Settlement settlement = new Settlement("Haven");
        settlement.registerSurvivor(sampleSurvivor(5), RegionId.REGION_2);
        settlement.registerSurvivor(sampleSurvivor(2), RegionId.REGION_1);
        settlement.registerSurvivor(sampleSurvivor(8), RegionId.REGION_1);

        List<SurvivorId> ordered = settlement.survivors().stream().map(Survivor::id).toList();
        assertEquals(List.of(new SurvivorId(2), new SurvivorId(5), new SurvivorId(8)), ordered);

        assertThrows(UnsupportedOperationException.class, () -> settlement.survivors().clear());
        assertThrows(UnsupportedOperationException.class, () -> settlement.regions().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> settlement.survivorsIn(RegionId.REGION_1).clear());
    }
}
