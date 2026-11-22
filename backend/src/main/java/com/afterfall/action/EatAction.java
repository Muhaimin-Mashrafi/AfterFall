package com.afterfall.action;

import com.afterfall.domain.ItemType;
import com.afterfall.domain.ResourceType;
import com.afterfall.domain.Need;
import java.util.Objects;

/**
 * Command representing a survivor consuming food to alleviate hunger.
 *
 * <p>Requires valid food availability (in survivor inventory or region context).
 * Proposes food consumption (-1 FOOD item) and hunger satisfaction (+30 FOOD need).
 * Does not mutate inventory or survivor state directly.
 */
public final class EatAction implements Action {

    public static final int HUNGER_REDUCTION = 30;

    @Override
    public ActionResult evaluate(ActionContext context) {
        Objects.requireNonNull(context, "context");

        if (!context.survivor().isAlive()) {
            return ActionResult.rejected(ActionType.EAT, "Cannot eat: survivor is dead");
        }

        boolean hasPersonalFood = context.survivor().hasItem(ItemType.FOOD, 1);
        if (!hasPersonalFood && !context.foodAvailable()) {
            return ActionResult.rejected(ActionType.EAT, "No food available to consume");
        }

        ActionResult.Builder builder = ActionResult.builder(ActionType.EAT)
                .needDelta(Need.FOOD, HUNGER_REDUCTION);

        if (hasPersonalFood) {
            builder.inventoryDelta(ItemType.FOOD, -1);
        } else {
            // Propose a shared resource claim for one FOOD unit
            builder.addResourceClaim(new ResourceClaim(ResourceType.FOOD, 1));
        }

        builder.addEvent(new ProposedEvent(
                "SURVIVOR_EAT",
                context.survivor().id(),
                context.survivor().name() + " ate a meal to satisfy hunger.",
                1
        ));

        return builder.build();
    }

    @Override
    public ActionType type() {
        return ActionType.EAT;
    }
}
