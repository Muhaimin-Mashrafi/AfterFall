package com.afterfall.persistence;

import com.afterfall.application.persistence.EvidenceDiscoveryRepository;
import com.afterfall.application.persistence.RunId;
import com.afterfall.domain.EventId;
import com.afterfall.domain.SurvivorId;
import com.afterfall.infrastructure.persistence.inmemory.InMemoryEvidenceDiscoveryRepository;
import com.afterfall.observer.InterventionType;
import com.afterfall.recap.DiscrepancyType;
import com.afterfall.recap.EvidenceItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EvidenceDiscoveryRepositoryTest {

    private final EvidenceDiscoveryRepository repo = new InMemoryEvidenceDiscoveryRepository();
    private final RunId runId = RunId.of("ev-test-run");

    @Test
    @DisplayName("Saves available evidence and prevents duplicate discovery increments")
    void testAvailableAndDiscoveredSeparation() {
        EvidenceItem item1 = new EvidenceItem(
                "EV-1", 10L, InterventionType.INFORMATION_DISTORTION, "Settlement",
                "Original", "Altered", DiscrepancyType.SUPPRESSED_TRUTH,
                List.of(new EventId(1L)), List.of(new SurvivorId(1))
        );
        EvidenceItem item2 = new EvidenceItem(
                "EV-2", 20L, InterventionType.RESOURCE_PRESSURE, "Settlement",
                "Original 2", "Altered 2", DiscrepancyType.FABRICATED_SHORTAGE,
                List.of(new EventId(2L)), List.of(new SurvivorId(2))
        );

        repo.saveAllAvailableEvidence(runId, List.of(item1, item2));

        // Both available, zero discovered
        assertEquals(2, repo.availableEvidenceForRun(runId).size());
        assertTrue(repo.discoveredEvidenceIdsForRun(runId).isEmpty());
        assertFalse(repo.isDiscovered(runId, "EV-1"));

        // Discover EV-1
        boolean newlyDiscovered = repo.markDiscovered(runId, "EV-1");
        assertTrue(newlyDiscovered);
        assertTrue(repo.isDiscovered(runId, "EV-1"));
        assertEquals(Set.of("EV-1"), repo.discoveredEvidenceIdsForRun(runId));
        assertEquals(1, repo.discoveredEvidenceForRun(runId).size());

        // Attempt duplicate discovery
        boolean duplicate = repo.markDiscovered(runId, "EV-1");
        assertFalse(duplicate, "Duplicate discovery must return false");
        assertEquals(1, repo.discoveredEvidenceIdsForRun(runId).size(), "Duplicate discovery must not increment counter");
    }
}
