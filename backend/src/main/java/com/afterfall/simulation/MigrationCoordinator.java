package com.afterfall.simulation;

import com.afterfall.domain.MigrationHandoff;
import com.afterfall.domain.RegionId;
import com.afterfall.domain.Settlement;
import com.afterfall.domain.Survivor;
import com.afterfall.domain.SurvivorId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Handles cross-region migration requests at the tick boundary.
 *
 * <p>Validates:
 * <ul>
 *   <li>Survivor existence in Settlement.</li>
 *   <li>Source region matches the survivor's current assigned region.</li>
 *   <li>Destination region exists and differs from source region.</li>
 *   <li>No conflicting or duplicate migration requests for the same survivor in the same tick.</li>
 * </ul>
 *
 * <p>Supports atomic rollback if subsequent tick operations fail.
 */
public final class MigrationCoordinator {

    private final Settlement settlement;

    public MigrationCoordinator(Settlement settlement) {
        this.settlement = Objects.requireNonNull(settlement, "settlement");
    }

    /**
     * Represents a single executed migration that can be rolled back.
     */
    public record AppliedMigration(Survivor survivor, RegionId originalSource, RegionId destination) {}

    /**
     * Validates and applies migration requests atomically.
     *
     * @param requests ordered list of migration requests
     * @return list of successfully applied migrations (for potential rollback)
     * @throws IllegalStateException or IllegalArgumentException if any migration request fails validation
     */
    public List<AppliedMigration> applyMigrations(List<MigrationRequest> requests) {
        Objects.requireNonNull(requests, "requests");
        List<AppliedMigration> applied = new ArrayList<>();
        Set<SurvivorId> migratingSurvivors = new HashSet<>();

        try {
            for (MigrationRequest req : requests) {
                // Reject duplicate requests for the same survivor in the same tick
                if (!migratingSurvivors.add(req.survivorId())) {
                    throw new IllegalStateException("Duplicate or conflicting migration request for survivor: " + req.survivorId());
                }

                // Verify survivor exists
                Survivor survivor = settlement.survivor(req.survivorId())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown migrating survivor: " + req.survivorId()));

                // Verify source region matches
                RegionId actualSource = settlement.regionOf(req.survivorId())
                        .orElseThrow(() -> new IllegalStateException("Survivor " + req.survivorId() + " has no assigned region"));

                if (!actualSource.equals(req.sourceRegion())) {
                    throw new IllegalStateException("Migration request source " + req.sourceRegion() +
                            " does not match actual survivor region " + actualSource + " for survivor " + req.survivorId());
                }

                // Destination region must differ from source
                if (req.sourceRegion().equals(req.destinationRegion())) {
                    throw new IllegalArgumentException("Migration source and destination cannot be identical: " + req.destinationRegion());
                }

                // Destination region must exist
                if (settlement.region(req.destinationRegion()).isEmpty()) {
                    throw new IllegalArgumentException("Unknown destination region: " + req.destinationRegion());
                }

                // Execute transfer via domain handoff
                MigrationHandoff.executeMigration(settlement, survivor, req.sourceRegion(), req.destinationRegion());
                applied.add(new AppliedMigration(survivor, req.sourceRegion(), req.destinationRegion()));
            }
            return applied;
        } catch (RuntimeException e) {
            // Roll back any migrations already performed in this batch
            rollbackMigrations(applied);
            throw e;
        }
    }

    /**
     * Rolls back previously applied migrations to their original source regions.
     */
    public void rollbackMigrations(List<AppliedMigration> applied) {
        if (applied == null || applied.isEmpty()) {
            return;
        }
        for (int i = applied.size() - 1; i >= 0; i--) {
            AppliedMigration m = applied.get(i);
            try {
                MigrationHandoff.executeMigration(settlement, m.survivor(), m.destination(), m.originalSource());
            } catch (RuntimeException rbEx) {
                // Best-effort rollback
            }
        }
    }
}
