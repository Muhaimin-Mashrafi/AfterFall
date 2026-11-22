package com.afterfall.behavior;

import com.afterfall.action.Action;
import com.afterfall.action.RestAction;
import com.afterfall.domain.HealthCondition;
import com.afterfall.domain.Need;
import java.util.Objects;
import java.util.Optional;

/**
 * Deterministic selector that evaluates behavior strategies using explicit, stable priority.
 *
 * <p>Priority hierarchy:
 * <ol>
 *   <li>Critical health / medical condition ({@link SeekMedicalHelpBehavior})</li>
 *   <li>Critical hunger ({@link SeekFoodBehavior})</li>
 *   <li>Severe exhaustion ({@link RestBehavior})</li>
 *   <li>Secondary hunger ({@link SeekFoodBehavior})</li>
 *   <li>Productive labor ({@link WorkBehavior})</li>
 *   <li>Default recovery / rest ({@link RestBehavior})</li>
 * </ol>
 *
 * <p>Zero randomness is used; identical contexts always produce identical selected actions.
 */
public final class BehaviorSelector {

    private final BehaviorConfig config;
    private final SeekMedicalHelpBehavior medicalBehavior;
    private final SeekFoodBehavior foodBehavior;
    private final RestBehavior restBehavior;
    private final WorkBehavior workBehavior;

    public BehaviorSelector() {
        this(BehaviorConfig.defaultConfig());
    }

    public BehaviorSelector(BehaviorConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.medicalBehavior = new SeekMedicalHelpBehavior(config);
        this.foodBehavior = new SeekFoodBehavior(config);
        this.restBehavior = new RestBehavior(config);
        this.workBehavior = new WorkBehavior(config);
    }

    /**
     * Evaluates strategies in deterministic priority order and returns the chosen Action.
     */
    public Action selectAction(BehaviorContext context) {
        Objects.requireNonNull(context, "context");

        if (!context.survivor().isAlive()) {
            throw new IllegalArgumentException("Cannot select behavior for deceased survivor: " + context.survivor().id());
        }

        // 1. Critical health condition or acute low vitality
        if (context.survivor().condition() != HealthCondition.HEALTHY
                || context.survivor().vitality() <= config.criticalVitalityThreshold()) {
            Optional<Action> medicalAction = medicalBehavior.decide(context);
            if (medicalAction.isPresent()) {
                return medicalAction.get();
            }
        }

        // 2. Critical starvation
        if (context.survivor().needLevel(Need.FOOD) <= config.criticalFoodThreshold()) {
            Optional<Action> foodAction = foodBehavior.decide(context);
            if (foodAction.isPresent()) {
                return foodAction.get();
            }
        }

        // 3. Critical exhaustion
        if (context.survivor().needLevel(Need.REST) <= config.criticalRestThreshold()) {
            Optional<Action> restAction = restBehavior.decide(context);
            if (restAction.isPresent()) {
                return restAction.get();
            }
        }

        // 4. Secondary hunger (if below hunger threshold)
        if (context.survivor().needLevel(Need.FOOD) <= config.hungerThreshold()) {
            Optional<Action> foodAction = foodBehavior.decide(context);
            if (foodAction.isPresent()) {
                return foodAction.get();
            }
        }

        // 5. Work (if healthy and reasonably rested)
        Optional<Action> workAction = workBehavior.decide(context);
        if (workAction.isPresent()) {
            return workAction.get();
        }

        // 6. Secondary fatigue
        Optional<Action> restAction = restBehavior.decide(context);
        if (restAction.isPresent()) {
            return restAction.get();
        }

        // 7. Deterministic default: restful recovery
        return new RestAction();
    }
}
