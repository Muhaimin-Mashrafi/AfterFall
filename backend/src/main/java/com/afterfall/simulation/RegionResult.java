package com.afterfall.simulation;

import com.afterfall.action.ActionResult;
import com.afterfall.domain.RegionId;
import com.afterfall.domain.SurvivorId;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Immutable result of processing a region during a tick.
 * Contains ordered survivor action evaluations and deferred migration requests.
 */
public record RegionResult(
        RegionId regionId,
        List<SurvivorActionResult> survivorResults,
        List<MigrationRequest> migrationRequests
) {
    public RegionResult {
        Objects.requireNonNull(regionId, "regionId");
        Objects.requireNonNull(survivorResults, "survivorResults");
        Objects.requireNonNull(migrationRequests, "migrationRequests");
        survivorResults = List.copyOf(survivorResults);
        migrationRequests = List.copyOf(migrationRequests);
    }

    /**
     * Backward-compatible constructor defaulting migrationRequests to empty list.
     */
    public RegionResult(RegionId regionId, List<SurvivorActionResult> survivorResults) {
        this(regionId, survivorResults, List.of());
    }

    /** Returns a flat list of all ActionResults in evaluation order. */
    public List<ActionResult> actionResults() {
        return survivorResults.stream()
                .map(SurvivorActionResult::actionResult)
                .collect(Collectors.toList());
    }
}
