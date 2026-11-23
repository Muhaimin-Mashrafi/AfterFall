package com.afterfall.domain;

import java.util.Objects;

/**
 * Encapsulates mutation operations for region migration at the tick boundary.
 *
 * <p>Located in {@code com.afterfall.domain} to safely access package-private
 * {@link Region#addSurvivor(SurvivorId)}, {@link Region#removeSurvivor(SurvivorId)},
 * and {@link Survivor#setLocation(RegionId)} without exposing them publicly.
 */
public final class MigrationHandoff {

    private MigrationHandoff() {}

    /**
     * Executes atomic migration handoff for one survivor from sourceRegion to destinationRegion.
     * Enforces that the survivor belongs to exactly the destination region upon completion.
     */
    public static void executeMigration(Settlement settlement, Survivor survivor, RegionId sourceRegion, RegionId destinationRegion) {
        Objects.requireNonNull(settlement, "settlement");
        Objects.requireNonNull(survivor, "survivor");
        Objects.requireNonNull(sourceRegion, "sourceRegion");
        Objects.requireNonNull(destinationRegion, "destinationRegion");

        Region source = settlement.region(sourceRegion)
                .orElseThrow(() -> new IllegalArgumentException("Unknown source region: " + sourceRegion));
        Region destination = settlement.region(destinationRegion)
                .orElseThrow(() -> new IllegalArgumentException("Unknown destination region: " + destinationRegion));

        if (!source.contains(survivor.id())) {
            throw new IllegalStateException("Survivor " + survivor.id() + " is not in source region " + sourceRegion);
        }
        if (destination.contains(survivor.id())) {
            throw new IllegalStateException("Survivor " + survivor.id() + " is already in destination region " + destinationRegion);
        }

        // Transfer membership
        source.removeSurvivor(survivor.id());
        destination.addSurvivor(survivor.id());
        survivor.setLocation(destinationRegion);
        settlement.updateSurvivorRegionIndex(survivor.id(), destinationRegion);
    }
}
