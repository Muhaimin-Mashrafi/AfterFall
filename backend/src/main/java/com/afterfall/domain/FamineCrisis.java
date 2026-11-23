package com.afterfall.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Famine crisis: triggers when settlement food stock falls below a configurable threshold.
 *
 * <p>Effects each active tick:
 * <ul>
 *   <li>Every alive survivor loses {@value #FOOD_NEED_PENALTY} food-need points.</li>
 *   <li>Every alive survivor loses {@value #VITALITY_PENALTY} vitality.</li>
 * </ul>
 *
 * <p>Resolution: food stock recovers to or above the trigger threshold.
 *
 * <p>Extends {@link Crisis} and overrides only the hook methods, demonstrating the
 * Template Method pattern and Liskov Substitution Principle.
 */
public final class FamineCrisis extends Crisis {

    /** Food stock quantity at or below which famine triggers. */
    public static final int DEFAULT_THRESHOLD = 10;

    /** Food-need penalty per active tick. */
    static final int FOOD_NEED_PENALTY = -15;

    /** Vitality penalty per active tick. */
    static final int VITALITY_PENALTY = -5;

    private final int threshold;

    public FamineCrisis() {
        this(DEFAULT_THRESHOLD);
    }

    public FamineCrisis(int threshold) {
        super("Famine");
        if (threshold < 0) {
            throw new IllegalArgumentException("threshold must not be negative");
        }
        this.threshold = threshold;
    }

    public int threshold() {
        return threshold;
    }

    @Override
    protected boolean checkTrigger(Settlement settlement, ResourceStock stock) {
        return stock.quantity(ResourceType.FOOD) <= threshold;
    }

    @Override
    protected boolean checkResolution(Settlement settlement, ResourceStock stock) {
        return stock.quantity(ResourceType.FOOD) > threshold;
    }

    @Override
    protected List<CrisisEffect> collectEffects(Settlement settlement, ResourceStock stock) {
        List<CrisisEffect> effects = new ArrayList<>();
        for (Survivor survivor : settlement.survivors()) {
            if (survivor.isAlive()) {
                effects.add(
                        CrisisEffect.forSurvivor(survivor.id())
                                .needDelta(Need.FOOD, FOOD_NEED_PENALTY)
                                .vitalityDelta(VITALITY_PENALTY)
                                .build()
                );
            }
        }
        return effects;
    }
}
