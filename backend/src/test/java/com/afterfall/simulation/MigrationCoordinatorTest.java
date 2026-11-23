package com.afterfall.simulation;

import com.afterfall.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MigrationCoordinatorTest {

    private Settlement settlement;
    private MigrationCoordinator coordinator;

    @BeforeEach
    void setUp() {
        settlement = new Settlement("Test Settlement");
        // Add 4 survivors across regions
        settlement.registerSurvivor(createSurvivor(1, "Alice"), RegionId.REGION_1);
        settlement.registerSurvivor(createSurvivor(2, "Bob"), RegionId.REGION_1);
        settlement.registerSurvivor(createSurvivor(3, "Charlie"), RegionId.REGION_2);
        settlement.registerSurvivor(createSurvivor(4, "Diana"), RegionId.REGION_3);

        coordinator = new MigrationCoordinator(settlement);
    }

    private Survivor createSurvivor(int idVal, String name) {
        Needs needs = new Needs(50, 50, 50, 50);
        Health health = new Health(80, HealthCondition.HEALTHY);
        Inventory inventory = new Inventory(20);
        return new Survivor(
                new SurvivorId(idVal),
                name,
                Personality.neutral(),
                new Profession(ProfessionType.FARMER, 50),
                needs,
                health,
                inventory
        );
    }

    @Test
    void testSuccessfulMigrationHandoffUpdatesSettlementAndSurvivor() {
        MigrationRequest req = new MigrationRequest(new SurvivorId(1), RegionId.REGION_1, RegionId.REGION_2, 1);

        coordinator.applyMigrations(List.of(req));

        // Alice is now in Region 2
        assertEquals(Optional.of(RegionId.REGION_2), settlement.survivor(new SurvivorId(1)).flatMap(Survivor::location));
        assertTrue(settlement.region(RegionId.REGION_2).map(r -> r.survivorIds().contains(new SurvivorId(1))).orElse(false));
        assertFalse(settlement.region(RegionId.REGION_1).map(r -> r.survivorIds().contains(new SurvivorId(1))).orElse(true));
    }

    @Test
    void testMultipleValidMigrationsInDifferentRegions() {
        MigrationRequest req1 = new MigrationRequest(new SurvivorId(1), RegionId.REGION_1, RegionId.REGION_2, 1);
        MigrationRequest req3 = new MigrationRequest(new SurvivorId(3), RegionId.REGION_2, RegionId.REGION_3, 1);
        MigrationRequest req4 = new MigrationRequest(new SurvivorId(4), RegionId.REGION_3, RegionId.REGION_4, 1);

        coordinator.applyMigrations(List.of(req1, req3, req4));

        assertEquals(Optional.of(RegionId.REGION_2), settlement.survivor(new SurvivorId(1)).flatMap(Survivor::location));
        assertEquals(Optional.of(RegionId.REGION_3), settlement.survivor(new SurvivorId(3)).flatMap(Survivor::location));
        assertEquals(Optional.of(RegionId.REGION_4), settlement.survivor(new SurvivorId(4)).flatMap(Survivor::location));
    }

    @Test
    void testRejectsDuplicateMigrationRequestForSameSurvivor() {
        MigrationRequest req1 = new MigrationRequest(new SurvivorId(1), RegionId.REGION_1, RegionId.REGION_2, 1);
        MigrationRequest req2 = new MigrationRequest(new SurvivorId(1), RegionId.REGION_1, RegionId.REGION_3, 2);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                coordinator.applyMigrations(List.of(req1, req2)));
        assertTrue(ex.getMessage().contains("Duplicate or conflicting migration request"));

        // Location must remain unchanged
        assertEquals(Optional.of(RegionId.REGION_1), settlement.survivor(new SurvivorId(1)).flatMap(Survivor::location));
    }

    @Test
    void testRejectsSameSourceAndDestination() {
        MigrationRequest req = new MigrationRequest(new SurvivorId(1), RegionId.REGION_1, RegionId.REGION_1, 1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                coordinator.applyMigrations(List.of(req)));
        assertTrue(ex.getMessage().contains("Migration source and destination cannot be identical"));
    }

    @Test
    void testRejectsMismatchedSourceRegion() {
        // Survivor 1 is in Region 1, but request claims source is Region 2
        MigrationRequest req = new MigrationRequest(new SurvivorId(1), RegionId.REGION_2, RegionId.REGION_3, 1);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                coordinator.applyMigrations(List.of(req)));
        assertTrue(ex.getMessage().contains("does not match actual survivor region"));
    }

    @Test
    void testRejectsUnknownSurvivor() {
        MigrationRequest req = new MigrationRequest(new SurvivorId(999), RegionId.REGION_1, RegionId.REGION_2, 1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                coordinator.applyMigrations(List.of(req)));
        assertTrue(ex.getMessage().contains("Unknown migrating survivor"));
    }

    @Test
    void testAtomicRollbackOfMigrations() {
        MigrationRequest req1 = new MigrationRequest(new SurvivorId(1), RegionId.REGION_1, RegionId.REGION_2, 1);
        MigrationRequest req2 = new MigrationRequest(new SurvivorId(2), RegionId.REGION_1, RegionId.REGION_3, 1);

        List<MigrationCoordinator.AppliedMigration> applied = coordinator.applyMigrations(List.of(req1, req2));
        assertEquals(2, applied.size());

        // Both moved
        assertEquals(Optional.of(RegionId.REGION_2), settlement.survivor(new SurvivorId(1)).flatMap(Survivor::location));
        assertEquals(Optional.of(RegionId.REGION_3), settlement.survivor(new SurvivorId(2)).flatMap(Survivor::location));

        // Roll back
        coordinator.rollbackMigrations(applied);

        // Both restored to Region 1
        assertEquals(Optional.of(RegionId.REGION_1), settlement.survivor(new SurvivorId(1)).flatMap(Survivor::location));
        assertEquals(Optional.of(RegionId.REGION_1), settlement.survivor(new SurvivorId(2)).flatMap(Survivor::location));
        assertTrue(settlement.region(RegionId.REGION_1).map(r -> r.survivorIds().contains(new SurvivorId(1))).orElse(false));
        assertTrue(settlement.region(RegionId.REGION_1).map(r -> r.survivorIds().contains(new SurvivorId(2))).orElse(false));
        assertFalse(settlement.region(RegionId.REGION_2).map(r -> r.survivorIds().contains(new SurvivorId(1))).orElse(true));
        assertFalse(settlement.region(RegionId.REGION_3).map(r -> r.survivorIds().contains(new SurvivorId(2))).orElse(true));
    }
}
