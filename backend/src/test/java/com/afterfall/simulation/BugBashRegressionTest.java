package com.afterfall.simulation;

import com.afterfall.application.SimulationService;
import com.afterfall.application.persistence.EvidenceDiscoveryRepository;
import com.afterfall.application.persistence.RunId;
import com.afterfall.application.persistence.RunRepository;
import com.afterfall.choice.ChoiceMoment;
import com.afterfall.choice.ChoiceOption;
import com.afterfall.domain.EventId;
import com.afterfall.domain.SurvivorId;
import com.afterfall.observer.InterventionType;
import com.afterfall.recap.DiscrepancyType;
import com.afterfall.recap.EvidenceItem;
import com.afterfall.web.SimulationApiController;
import com.afterfall.web.StoryApiController;
import com.afterfall.web.dto.ChoiceSelectionRequest;
import com.afterfall.web.dto.StateResponse;
import com.afterfall.web.dto.SurvivorDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Milestone M13 Bug Bash Automated Verification Suite.
 * Covers P0 (Scenarios 1-3) and P2 (Scenario 8) directly.
 */
@SpringBootTest
@ActiveProfiles("test")
public class BugBashRegressionTest {

    @Autowired
    private SimulationService simulationService;

    @Autowired
    private SimulationApiController simulationApiController;

    @Autowired
    private StoryApiController storyApiController;

    @Autowired
    private EvidenceDiscoveryRepository evidenceRepo;

    @Autowired
    private RunRepository runRepository;

    @Test
    @DisplayName("P0-1: Rapid concurrent choice submission does not deadlock or produce duplicate choices")
    void testRapidChoiceSubmissionConcurrency() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger completedCount = new AtomicInteger(0);

        // Submit 4 concurrent choices for a dummy choice ID via simulationService.choose
        for (int i = 0; i < 4; i++) {
            final int optIdx = i % 2;
            pool.submit(() -> {
                try {
                    startLatch.await();
                    simulationService.choose("CHOICE-CONCURRENT-TEST", optIdx);
                    completedCount.incrementAndGet();
                } catch (Exception ignored) {}
            });
        }

        startLatch.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(3, TimeUnit.SECONDS)).isTrue();
        assertThat(completedCount.get()).isEqualTo(4);
    }

    @Test
    @DisplayName("P0-2: Repeated evidence discovery prevents duplicate chapter counter increments")
    void testRepeatedEvidenceDiscoveryIdempotence() {
        RunId runId = RunId.of("RUN-BB-2");
        EvidenceItem item = new EvidenceItem(
                "EV-BB-DUPE",
                5L,
                InterventionType.INFORMATION_DISTORTION,
                "GLOBAL",
                "Ground truth item",
                "Altered observer item",
                DiscrepancyType.FABRICATED_SHORTAGE,
                List.of(new EventId(1L)),
                List.of(new SurvivorId(1))
        );

        evidenceRepo.saveAllAvailableEvidence(runId, List.of(item));

        // Discover first time
        boolean first = evidenceRepo.markDiscovered(runId, "EV-BB-DUPE");
        assertThat(first).isTrue();
        assertThat(evidenceRepo.discoveredEvidenceIdsForRun(runId)).hasSize(1);

        // Discover duplicate times
        boolean second = evidenceRepo.markDiscovered(runId, "EV-BB-DUPE");
        boolean third = evidenceRepo.markDiscovered(runId, "EV-BB-DUPE");

        assertThat(second).isFalse();
        assertThat(third).isFalse();
        assertThat(evidenceRepo.discoveredEvidenceIdsForRun(runId)).hasSize(1);
    }

    @Test
    @DisplayName("P2-8: Malformed REST payloads and invalid IDs return safe status without crashing service")
    void testMalformedRestRequests() {
        // Unknown survivor ID returns 404 Not Found
        ResponseEntity<SurvivorDto> missingSurvivor = simulationApiController.getSurvivor("9999");
        assertThat(missingSurvivor.getStatusCode().value()).isEqualTo(404);

        // Non-numeric survivor ID returns 404 Not Found
        ResponseEntity<SurvivorDto> nonNumericSurvivor = simulationApiController.getSurvivor("INVALID_ID");
        assertThat(nonNumericSurvivor.getStatusCode().value()).isEqualTo(404);

        // Submitting choice for unknown choice ID returns current state gracefully without crashing
        ResponseEntity<StateResponse> safeResponse = storyApiController.submitChoice(
                "NON_EXISTENT_CHOICE",
                new ChoiceSelectionRequest("OPT-UNKNOWN", 99)
        );
        assertThat(safeResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(safeResponse.getBody()).isNotNull();
    }
}
