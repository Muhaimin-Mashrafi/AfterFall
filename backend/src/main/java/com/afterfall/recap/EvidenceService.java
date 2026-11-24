package com.afterfall.recap;

import com.afterfall.domain.EventId;
import com.afterfall.domain.SurvivorId;
import com.afterfall.observer.InterventionRecord;
import com.afterfall.observer.InterventionStatus;
import com.afterfall.observer.InterventionType;

import java.util.*;

/**
 * Domain service analyzing actual {@link InterventionRecord}s to extract verified evidence items.
 *
 * <p>Requirements:
 * <ul>
 *   <li>Compares original and altered reports to identify genuine discrepancies.</li>
 *   <li>Classifies discrepancies into explicit types (SUPPRESSED_TRUTH, FABRICATED_SHORTAGE, etc.).</li>
 *   <li>Preserves source EventIds and SurvivorIds.</li>
 *   <li>Distinguishes verified simulation facts from Observer claims.</li>
 *   <li>Treats only applied interventions as executed manipulations. Rejected interventions remain distinguishable.</li>
 *   <li>Zero framework dependencies.</li>
 * </ul>
 */
public class EvidenceService {

    /**
     * Extracts available evidence items from real intervention records.
     * Pure and deterministic: identical records produce identical evidence items.
     */
    public List<EvidenceItem> extractAvailableEvidence(List<InterventionRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        List<EvidenceItem> items = new ArrayList<>();
        for (InterventionRecord r : records) {
            // Only applied interventions constitute actionable evidence of social manipulation
            if (r.status() != InterventionStatus.APPLIED) {
                continue;
            }

            DiscrepancyType type = classifyDiscrepancy(r);
            if (type == null) {
                continue;
            }

            List<SurvivorId> survivors = extractSurvivorIds(r.targetDescription());
            List<EventId> events = List.of(new EventId(r.interventionId()));

            EvidenceItem item = new EvidenceItem(
                    "EV-" + r.interventionId(),
                    r.tick(),
                    r.type(),
                    r.targetDescription(),
                    r.originalReport(),
                    r.alteredReport(),
                    type,
                    events,
                    survivors
            );
            items.add(item);
        }

        return Collections.unmodifiableList(items);
    }

    /**
     * Classifies the discrepancy between ground truth (originalReport) and altered broadcast (alteredReport).
     */
    public DiscrepancyType classifyDiscrepancy(InterventionRecord record) {
        if (record == null) return null;

        // If reports are identical, no discrepancy occurred
        if (Objects.equals(record.originalReport(), record.alteredReport())) {
            return null;
        }

        InterventionType policy = record.type();
        if (policy == InterventionType.INFORMATION_DISTORTION) {
            return DiscrepancyType.SUPPRESSED_TRUTH;
        } else if (policy == InterventionType.RESOURCE_PRESSURE) {
            return DiscrepancyType.FABRICATED_SHORTAGE;
        } else if (policy == InterventionType.TRUST_EROSION) {
            return DiscrepancyType.DIVISIVE_RUMOUR;
        } else if (policy == InterventionType.SELECTIVE_DISCLOSURE) {
            return DiscrepancyType.SELECTIVE_WARNING;
        }

        return DiscrepancyType.SUPPRESSED_TRUTH;
    }

    private List<SurvivorId> extractSurvivorIds(String targetDescription) {
        if (targetDescription == null || targetDescription.isBlank()) {
            return Collections.emptyList();
        }
        // Pattern: "Survivor #X"
        if (targetDescription.contains("#")) {
            try {
                String numStr = targetDescription.substring(targetDescription.indexOf("#") + 1).trim();
                // Take contiguous digits
                StringBuilder sb = new StringBuilder();
                for (char c : numStr.toCharArray()) {
                    if (Character.isDigit(c)) sb.append(c);
                    else break;
                }
                if (sb.length() > 0) {
                    return List.of(new SurvivorId(Integer.parseInt(sb.toString())));
                }
            } catch (Exception ignored) {}
        }
        return Collections.emptyList();
    }
}
