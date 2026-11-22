package com.afterfall.action;

import com.afterfall.domain.Need;
import java.util.Objects;

/**
 * Command representing a survivor performing labor according to their profession and skill.
 *
 * <p>Proposes energy expenditure (-15 REST need) and produces work contribution proportional to
 * the survivor's skill. Does not directly mutate Needs or modify settlement resources.
 */
public final class WorkAction implements Action {

    public static final int BASE_REST_COST = -15;

    @Override
    public ActionResult evaluate(ActionContext context) {
        Objects.requireNonNull(context, "context");

        if (!context.survivor().isAlive()) {
            return ActionResult.rejected(ActionType.WORK, "Cannot work: survivor is dead");
        }

        if (context.survivor().needLevel(Need.REST) < Math.abs(BASE_REST_COST)) {
            return ActionResult.rejected(ActionType.WORK, "Cannot work: too exhausted");
        }

        int skill = context.survivor().profession().skill();
        int workOutput = Math.max(1, (skill / 10));

        return ActionResult.builder(ActionType.WORK)
                .needDelta(Need.REST, BASE_REST_COST)
                .workContribution(workOutput)
                .addEvent(new ProposedEvent(
                        "SURVIVOR_WORK",
                        context.survivor().id(),
                        context.survivor().name() + " worked as a " +
                                context.survivor().profession().displayName() + ".",
                        1
                ))
                .build();
    }

    @Override
    public ActionType type() {
        return ActionType.WORK;
    }
}
