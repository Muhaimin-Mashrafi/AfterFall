package com.afterfall.application.persistence;

import com.afterfall.recap.EvidenceItem;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Persistence port for available and discovered evidence items.
 *
 * <p>Requirements:
 * <ul>
 *   <li>Saves discovered evidence idempotently without duplicating counts or items.</li>
 *   <li>Retrieves available and discovered evidence.</li>
 *   <li>Preserves discovery state across application restarts.</li>
 *   <li>Free of Spring/JPA framework dependencies.</li>
 * </ul>
 */
public interface EvidenceDiscoveryRepository {

    /**
     * Saves an available evidence item for the given run. Idempotent by evidenceId.
     */
    void saveAvailableEvidence(RunId runId, EvidenceItem evidence);

    /**
     * Saves a list of available evidence items.
     */
    void saveAllAvailableEvidence(RunId runId, List<EvidenceItem> evidenceList);

    /**
     * Retrieves all available evidence items for the given run.
     */
    List<EvidenceItem> availableEvidenceForRun(RunId runId);

    /**
     * Retrieves a specific evidence item by ID.
     */
    Optional<EvidenceItem> findEvidence(RunId runId, String evidenceId);

    /**
     * Marks an evidence item as discovered by the player.
     * Idempotent: returns {@code true} if newly discovered, {@code false} if already discovered.
     */
    boolean markDiscovered(RunId runId, String evidenceId);

    /**
     * Checks if a specific evidence item has already been discovered.
     */
    boolean isDiscovered(RunId runId, String evidenceId);

    /**
     * Retrieves the set of all discovered evidence IDs for the given run.
     */
    Set<String> discoveredEvidenceIdsForRun(RunId runId);

    /**
     * Retrieves the list of discovered {@link EvidenceItem}s for the given run.
     */
    List<EvidenceItem> discoveredEvidenceForRun(RunId runId);
}
