package com.afterfall.recap;

import com.afterfall.domain.EventId;
import com.afterfall.domain.SurvivorId;
import com.afterfall.observer.InterventionRecord;
import com.afterfall.observer.InterventionStatus;
import com.afterfall.observer.InterventionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EvidenceServiceTest {

    private final EvidenceService service = new EvidenceService();

    @Test
    @DisplayName("Classifies information distortion as SUPPRESSED_TRUTH")
    void testClassifyInformationDistortion() {
        InterventionRecord r = new InterventionRecord(
                1L, 5L, InterventionType.INFORMATION_DISTORTION, "Survivor #1",
                "Food is low", "Reduce safety", "Success",
                InterventionStatus.APPLIED,
                "Food is actually fine", "Food is critical shortage"
        );
        assertEquals(DiscrepancyType.SUPPRESSED_TRUTH, service.classifyDiscrepancy(r));
    }

    @Test
    @DisplayName("Classifies resource pressure as FABRICATED_SHORTAGE")
    void testClassifyResourcePressure() {
        InterventionRecord r = new InterventionRecord(
                2L, 10L, InterventionType.RESOURCE_PRESSURE, "Settlement",
                "Induce urgency", "Competition", "Success",
                InterventionStatus.APPLIED,
                "Reserves at 40", "Reserves failing immediately"
        );
        assertEquals(DiscrepancyType.FABRICATED_SHORTAGE, service.classifyDiscrepancy(r));
    }

    @Test
    @DisplayName("Classifies trust erosion as DIVISIVE_RUMOUR")
    void testClassifyTrustErosion() {
        InterventionRecord r = new InterventionRecord(
                3L, 15L, InterventionType.TRUST_EROSION, "Survivor #2",
                "Erode bond", "Tension", "Success",
                InterventionStatus.APPLIED,
                "Shared resources peacefully", "Rumour: hoarding stolen medicine"
        );
        assertEquals(DiscrepancyType.DIVISIVE_RUMOUR, service.classifyDiscrepancy(r));
    }

    @Test
    @DisplayName("Classifies selective disclosure as SELECTIVE_WARNING")
    void testClassifySelectiveDisclosure() {
        InterventionRecord r = new InterventionRecord(
                4L, 20L, InterventionType.SELECTIVE_DISCLOSURE, "Survivor #3",
                "Alarm contagion", "Fear", "Success",
                InterventionStatus.APPLIED,
                "Mild fever with cure available", "Lethal contagion spread warning"
        );
        assertEquals(DiscrepancyType.SELECTIVE_WARNING, service.classifyDiscrepancy(r));
    }

    @Test
    @DisplayName("Excludes rejected interventions from available evidence")
    void testExcludesRejectedInterventions() {
        InterventionRecord r = new InterventionRecord(
                5L, 25L, InterventionType.INFORMATION_DISTORTION, "Survivor #4",
                "Reason", "Effect", "Rejected by overseer",
                InterventionStatus.REJECTED,
                "Truth", "Distortion"
        );
        List<EvidenceItem> available = service.extractAvailableEvidence(List.of(r));
        assertTrue(available.isEmpty(), "Rejected interventions must not produce actionable evidence");
    }

    @Test
    @DisplayName("Preserves valid SurvivorIds and EventIds in extracted evidence")
    void testPreservesSurvivorAndEventIds() {
        InterventionRecord r = new InterventionRecord(
                42L, 30L, InterventionType.TRUST_EROSION, "Survivor #7",
                "Reason", "Effect", "Success",
                InterventionStatus.APPLIED,
                "Ground truth fact", "Observer distorted rumor"
        );
        List<EvidenceItem> available = service.extractAvailableEvidence(List.of(r));
        assertEquals(1, available.size());
        EvidenceItem item = available.get(0);
        assertEquals("EV-42", item.evidenceId());
        assertEquals(30L, item.tick());
        assertFalse(item.referencedSurvivorIds().isEmpty());
        assertEquals(7, item.referencedSurvivorIds().get(0).value());
        assertFalse(item.sourceEventIds().isEmpty());
        assertEquals(42L, item.sourceEventIds().get(0).value());
    }
}
