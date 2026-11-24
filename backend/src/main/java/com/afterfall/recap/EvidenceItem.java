package com.afterfall.recap;

import com.afterfall.domain.EventId;
import com.afterfall.domain.SurvivorId;
import com.afterfall.observer.InterventionType;

import java.util.List;
import java.util.Objects;

/**
 * Immutable representation of a forensic evidence item extracted from an Observer intervention.
 *
 * <p>Separates evidence availability from player discovery: an intervention produces available
 * evidence, but it only contributes to Chapter III discovery thresholds once the player has
 * explicitly investigated/discovered it.
 */
public record EvidenceItem(
        String evidenceId,
        long tick,
        InterventionType policy,
        String target,
        String originalReport,
        String alteredReport,
        DiscrepancyType discrepancyType,
        List<EventId> sourceEventIds,
        List<SurvivorId> referencedSurvivorIds
) {
    public EvidenceItem {
        Objects.requireNonNull(evidenceId, "evidenceId");
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(originalReport, "originalReport");
        Objects.requireNonNull(alteredReport, "alteredReport");
        Objects.requireNonNull(discrepancyType, "discrepancyType");
        sourceEventIds = List.copyOf(Objects.requireNonNull(sourceEventIds, "sourceEventIds"));
        referencedSurvivorIds = List.copyOf(Objects.requireNonNull(referencedSurvivorIds, "referencedSurvivorIds"));
    }
}
