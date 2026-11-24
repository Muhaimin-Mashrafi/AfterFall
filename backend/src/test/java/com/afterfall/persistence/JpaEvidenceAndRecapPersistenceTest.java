package com.afterfall.persistence;

import com.afterfall.application.persistence.EvidenceDiscoveryRepository;
import com.afterfall.application.persistence.RunId;
import com.afterfall.application.persistence.RunRecapInputs;
import com.afterfall.application.persistence.RunRepository;
import com.afterfall.domain.EventId;
import com.afterfall.domain.SurvivorId;
import com.afterfall.observer.InterventionType;
import com.afterfall.recap.DiscrepancyType;
import com.afterfall.recap.EvidenceItem;
import com.afterfall.recap.RecapGenerator;
import com.afterfall.recap.RunRecap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaEvidenceAndRecapPersistenceTest {

    @Autowired
    private EvidenceDiscoveryRepository evidenceRepo;

    @Autowired
    private RunRepository runRepo;

    @Test
    @DisplayName("JPA persists and retrieves evidence items and discovery status across sessions")
    void testJpaEvidencePersistence() {
        RunId runId = RunId.of("jpa-ev-test");
        EvidenceItem item = new EvidenceItem(
                "EV-JPA-1", 15L, InterventionType.SELECTIVE_DISCLOSURE, "Survivor #5",
                "Full facts", "Selective omission", DiscrepancyType.SELECTIVE_WARNING,
                List.of(new EventId(15L)), List.of(new SurvivorId(5))
        );

        evidenceRepo.saveAvailableEvidence(runId, item);
        List<EvidenceItem> available = evidenceRepo.availableEvidenceForRun(runId);
        assertFalse(available.isEmpty());
        assertEquals("EV-JPA-1", available.get(0).evidenceId());

        boolean newlyDiscovered = evidenceRepo.markDiscovered(runId, "EV-JPA-1");
        assertTrue(newlyDiscovered);
        assertTrue(evidenceRepo.isDiscovered(runId, "EV-JPA-1"));

        // Verify duplicate prevention
        assertFalse(evidenceRepo.markDiscovered(runId, "EV-JPA-1"));

        // Verify recap input loading from persistence
        RunRecapInputs inputs = runRepo.loadRecapInputs(runId);
        assertNotNull(inputs);
        assertEquals(runId, inputs.runId());

        RecapGenerator generator = new RecapGenerator();
        RunRecap recap = generator.generate(inputs, Set.of("EV-JPA-1"));
        assertNotNull(recap);
        assertEquals("The settlement fell silent. The Observer remains active.", recap.finalRecording().closingLine());
    }
}
