package com.afterfall.action;

import com.afterfall.domain.Need;
import java.util.Objects;

/**
 * Command representing a survivor resting to recover energy and relieve fatigue.
 *
 * <p>Proposes energy restoration (+25 REST need) and an event proposal.
 * Does not mutate Needs directly.
 */
public final class RestAction implements Action {

    public static final int REST_RESTORE = 25;

    @Override
    public ActionResult evaluate(ActionContext context) {
        Objects.requireNonNull(context, "context");

        if (!context.survivor().isAlive()) {
            return ActionResult.rejected(ActionType.REST, "Cannot rest: survivor is dead");
        }

        return ActionResult.builder(ActionType.REST)
                .needDelta(Need.REST, REST_RESTORE)
                .addEvent(new ProposedEvent(
                        "SURVIVOR_REST",
                        context.survivor().id(),
                        context.survivor().name() + " rested to regain strength.",
                        1
                ))
                .build();
    }

    @Override
    public ActionType type() {
        return ActionType.REST;
    }
}
