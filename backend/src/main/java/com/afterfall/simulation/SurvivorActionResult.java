package com.afterfall.simulation;

import com.afterfall.action.ActionResult;
import com.afterfall.domain.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable result of evaluating a single survivor within a tick.
 */
public record SurvivorActionResult(
        SurvivorId survivorId,
        int sequenceNumber,
        ActionResult actionResult
) {
    public SurvivorActionResult {
        Objects.requireNonNull(survivorId, "survivorId");
        Objects.requireNonNull(actionResult, "actionResult");
    }
}
