package com.afterfall.simulation;

import com.afterfall.action.ProposedEvent;
import com.afterfall.domain.HealthCondition;
import com.afterfall.domain.Need;
import com.afterfall.domain.ResourceStock;
import com.afterfall.domain.ResourceType;
import com.afterfall.domain.Settlement;
import com.afterfall.domain.Survivor;
import com.afterfall.domain.Trait;

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

    /** The settlement forages one unit of food every this many ticks, whoever is working. */
    static final int FORAGE_INTERVAL_TICKS = 3;

    /** Recently fed: FOOD need at or above this level counts as fed for recovery. */
    static final int RECOVERY_FOOD_NEED = 60;
    /** Vitality regained per tick by a fed survivor. */
    static final int RECOVERY_VITALITY = 2;
    /** An ILL survivor recovers once vitality is back at this level and they are fed. */
    static final int RECOVERY_ILL_VITALITY = 50;

    /** RESILIENT trait at or below which a survivor gets no resilience relief. */
    static final int RESILIENCE_FLOOR = 30;
    /** RESILIENT trait at or above which a survivor gets relief every tick. */
    static final int RESILIENCE_CEILING = 100;

    /** A healthy survivor whose vitality falls to this level or below may fall ill. */
    static final int ILLNESS_VITALITY = 65;

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

    /**
     * Malnutrition illness: the lowest-id HEALTHY survivor at or below {@link #ILLNESS_VITALITY}
     * becomes ILL. At most one survivor per tick, so illness spreads gradually and can reach
     * the Disease Outbreak threshold only after sustained hardship.
     */
    static void applyMalnutritionIllness(Settlement settlement) {
        for (Survivor survivor : settlement.survivors()) {
            if (survivor.isAlive()
                    && survivor.health().condition() == HealthCondition.HEALTHY
                    && survivor.health().vitality() <= ILLNESS_VITALITY) {
                survivor.health().setCondition(HealthCondition.ILL);
                return;
            }
        }
    }

    /**
     * Natural recovery: a survivor whose FOOD need is at least {@link #RECOVERY_FOOD_NEED}
     * regains {@link #RECOVERY_VITALITY} vitality, and an ILL survivor at or above
     * {@link #RECOVERY_ILL_VITALITY} recovers. Hardship is therefore reversible while food lasts.
     */
    static void applyRecovery(Settlement settlement) {
        for (Survivor survivor : settlement.survivors()) {
            if (!survivor.isAlive() || survivor.needs().level(Need.FOOD) < RECOVERY_FOOD_NEED) {
                continue;
            }
            survivor.health().adjustVitality(RECOVERY_VITALITY);
            if (survivor.health().condition() == HealthCondition.ILL
                    && survivor.health().vitality() >= RECOVERY_ILL_VITALITY) {
                survivor.health().setCondition(HealthCondition.HEALTHY);
            }
        }
    }

    /**
     * Resilience: a survivor regains one vitality on a deterministic share of ticks that grows with
     * the RESILIENT trait (no relief at {@value #RESILIENCE_FLOOR} or below, every tick at
     * {@value #RESILIENCE_CEILING} or above). Hardship therefore thins the settlement from its
     * weakest members first instead of removing everyone in the same few ticks.
     */
    static void applyResilience(Settlement settlement, long tick) {
        for (Survivor survivor : settlement.survivors()) {
            if (!survivor.isAlive()) {
                continue;
            }
            int resilient = survivor.personality().value(Trait.RESILIENT);
            int share = Math.max(0, Math.min(100, (resilient - RESILIENCE_FLOOR) * 100 / (RESILIENCE_CEILING - RESILIENCE_FLOOR)));
            long slot = Math.floorMod(tick * 37 + survivor.id().value() * 53L, 100L);
            if (slot < share) {
                survivor.health().adjustVitality(1);
            }
        }
    }

    /** Converts the tick's work contribution (plus steady foraging) into food stock. Returns the units produced. */
    static int produceFood(ResourceStock stock, int totalWorkContribution, long tick) {
        int units = totalWorkContribution / WORK_PER_FOOD_UNIT + (tick % FORAGE_INTERVAL_TICKS == 0 ? 1 : 0);
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
