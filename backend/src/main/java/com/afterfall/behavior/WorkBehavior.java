package com.afterfall.behavior;

import com.afterfall.action.Action;
import com.afterfall.action.WorkAction;
import com.afterfall.domain.HealthCondition;
import com.afterfall.domain.Need;
import com.afterfall.domain.Trait;
import java.util.Objects;
import java.util.Optional;

/**
 * Strategy for contributing labor according to profession.
 *
 * <p>Applicable when basic needs (food, rest, safety) and health are above critical levels.
 * Cooperative and authoritative personalities are more inclined toward work, while high skill
 * reinforces profession alignment.
 */
public final class WorkBehavior implements Behavior {

    private final BehaviorConfig config;

    public WorkBehavior() {
        this(BehaviorConfig.defaultConfig());
    }

    public WorkBehavior(BehaviorConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public Optional<Action> decide(BehaviorContext context) {
        Objects.requireNonNull(context, "context");

        if (!context.survivor().isAlive()) {
            return Optional.empty();
        }

        // Work cannot proceed under acute distress
        if (context.survivor().condition() != HealthCondition.HEALTHY
                || context.survivor().needLevel(Need.FOOD) <= config.criticalFoodThreshold()
                || context.survivor().needLevel(Need.REST) <= config.criticalRestThreshold()
                || context.survivor().vitality() <= config.criticalVitalityThreshold()) {
            return Optional.empty();
        }

        // Trait and skill influence willingness to work
        int skill = context.survivor().profession().skill();
        int cooperative = context.survivor().personality().value(Trait.COOPERATIVE);
        int restLevel = context.survivor().needLevel(Need.REST);

        // Required rest for work depends slightly on cooperativeness
        int requiredRest = (cooperative > 60) ? 35 : 45;
        if (restLevel >= requiredRest && skill > 0) {
            return Optional.of(new WorkAction());
        }

        return Optional.empty();
    }

    @Override
    public BehaviorType type() {
        return BehaviorType.WORK;
    }
}
