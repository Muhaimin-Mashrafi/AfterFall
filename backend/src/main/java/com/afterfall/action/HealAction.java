package com.afterfall.action;

import com.afterfall.domain.HealthCondition;
import com.afterfall.domain.ItemType;
import com.afterfall.domain.ResourceType;
import java.util.Objects;

/**
 * Command representing a survivor undergoing medical treatment.
 *
 * <p>Requires a health condition warranting treatment (ILL or INJURED) and medicine availability.
 * Proposes condition recovery to HEALTHY, vitality restoration (+20), and -1 MEDICINE item consumption.
 * Does not directly mutate Health or Inventory.
 */
public final class HealAction implements Action {

    public static final int VITALITY_RECOVERY = 20;

    @Override
    public ActionResult evaluate(ActionContext context) {
        Objects.requireNonNull(context, "context");

        if (!context.survivor().isAlive()) {
            return ActionResult.rejected(ActionType.HEAL, "Cannot heal: survivor is dead");
        }

        HealthCondition condition = context.survivor().condition();
        if (condition == HealthCondition.HEALTHY) {
            return ActionResult.rejected(ActionType.HEAL, "Survivor is already healthy; no treatment needed");
        }

        boolean hasPersonalMedicine = context.survivor().hasItem(ItemType.MEDICINE, 1);
        if (!hasPersonalMedicine && !context.medicineAvailable()) {
            return ActionResult.rejected(ActionType.HEAL, "No medicine available for treatment");
        }

        ActionResult.Builder builder = ActionResult.builder(ActionType.HEAL)
                .vitalityDelta(VITALITY_RECOVERY)
                .newCondition(HealthCondition.HEALTHY);

        if (hasPersonalMedicine) {
            builder.inventoryDelta(ItemType.MEDICINE, -1);
        } else {
            // Propose a shared resource claim for one MEDICINE unit
            builder.addResourceClaim(new ResourceClaim(ResourceType.MEDICINE, 1));
        }

        builder.addEvent(new ProposedEvent(
                "SURVIVOR_HEAL",
                context.survivor().id(),
                context.survivor().name() + " received medical treatment for " + condition + ".",
                2
        ));

        return builder.build();
    }

    @Override
    public ActionType type() {
        return ActionType.HEAL;
    }
}
