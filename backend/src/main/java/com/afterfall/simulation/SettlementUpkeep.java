package com.afterfall.simulation;

import com.afterfall.action.ProposedEvent;
import com.afterfall.domain.Need;
import com.afterfall.domain.ResourceStock;
import com.afterfall.domain.ResourceType;
import com.afterfall.domain.Settlement;
import com.afterfall.domain.Survivor;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-tick settlement upkeep rules applied by the {@link TickCoordinator} (the sole writer)
 * during the post-merge phase: hunger drift, food production from work, and death resolution.
 *
 * <p>Pure functions of world state and the tick number: no RNG, no clock, no shared mutable state,
 * so the same seed and decision log always give the same result. Without these rules the settlement
 * is static (needs never fall, stock never changes), so no crisis, death or organic chapter
 * change could ever occur.
 */
final class SettlementUpkeep {

    /** Hunger drifts down every {@value} tick(s). */
    static final int HUNGER_INTERVAL_TICKS = 1;

    /** FOOD need lost by every living survivor at each hunger interval. */
    static final int HUNGER_DRIFT = 2;

    /** Total work contribution needed to produce one unit of settlement food. */
    static final int WORK_PER_FOOD_UNIT = 180;

    private SettlementUpkeep() {
    }

    /** Lowers every living survivor's FOOD need on hunger-interval ticks. */
    static void applyHungerDrift(Settlement settlement, long tick) {
        if (tick % HUNGER_INTERVAL_TICKS != 0) {
            return;
        }
        for (Survivor survivor : settlement.survivors()) {
            if (survivor.isAlive()) {
                survivor.needs().adjust(Need.FOOD, -HUNGER_DRIFT);
            }
        }
    }

    /** Converts the tick's total work contribution into food stock. Returns the units produced. */
    static int produceFood(ResourceStock stock, int totalWorkContribution) {
        int units = totalWorkContribution / WORK_PER_FOOD_UNIT;
        if (units > 0) {
            stock.resource(ResourceType.FOOD).add(units);
        }
        return units;
    }

    /**
     * Marks every living survivor whose vitality has reached zero as dead and returns one
     * CRITICAL death event per survivor, in survivor-id order.
     */
    static List<ProposedEvent> resolveDeaths(Settlement settlement) {
        List<ProposedEvent> events = new ArrayList<>();
        for (Survivor survivor : settlement.survivors()) {
            if (survivor.isAlive() && survivor.health().vitality() <= 0) {
                survivor.markDead();
                events.add(new ProposedEvent(
                        "SURVIVOR_DEATH",
                        survivor.id(),
                        survivor.name() + " did not survive.",
                        3));
            }
        }
        return events;
    }
}
