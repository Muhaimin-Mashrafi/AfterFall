package com.afterfall.action;

import com.afterfall.domain.RegionId;
import java.util.Objects;

/**
 * Command representing a survivor requesting to move to a different region.
 *
 * <p>Validates that the target destination is non-null and differs from the current region.
 * Proposes a destination RegionId without changing Survivor.location() or mutating settlement rosters.
 * Authoritative migration is performed exclusively in M4 at the tick boundary.
 */
public final class MoveAction implements Action {

    private final RegionId destination;

    public MoveAction(RegionId destination) {
        this.destination = Objects.requireNonNull(destination, "destination");
    }

    public RegionId destination() {
        return destination;
    }

    @Override
    public ActionResult evaluate(ActionContext context) {
        Objects.requireNonNull(context, "context");

        if (!context.survivor().isAlive()) {
            return ActionResult.rejected(ActionType.MOVE, "Cannot move: survivor is dead");
        }

        if (context.currentRegion().equals(destination)) {
            return ActionResult.rejected(ActionType.MOVE,
                    "Cannot move: destination " + destination + " is the current region");
        }

        return ActionResult.builder(ActionType.MOVE)
                .migrationRequest(destination)
                .addEvent(new ProposedEvent(
                        "SURVIVOR_MOVE_REQUEST",
                        context.survivor().id(),
                        context.survivor().name() + " requested migration from Region " +
                                context.currentRegion().value() + " to Region " + destination.value() + ".",
                        1
                ))
                .build();
    }

    @Override
    public ActionType type() {
        return ActionType.MOVE;
    }
}
