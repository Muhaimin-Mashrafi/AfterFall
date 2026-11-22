package com.afterfall.behavior;

import com.afterfall.action.Action;
import com.afterfall.action.RestAction;
import com.afterfall.domain.Need;
import com.afterfall.domain.Trait;
import java.util.Objects;
import java.util.Optional;

/**
 * Strategy for resting to recover energy.
 *
 * <p>Triggers when the REST need is below the fatigue threshold. Resilient survivors can endure
 * slightly more fatigue before requiring rest, but critical exhaustion cannot be ignored.
 */
public final class RestBehavior implements Behavior {

    private final BehaviorConfig config;

    public RestBehavior() {
        this(BehaviorConfig.defaultConfig());
    }

    public RestBehavior(BehaviorConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public Optional<Action> decide(BehaviorContext context) {
        Objects.requireNonNull(context, "context");

        if (!context.survivor().isAlive()) {
            return Optional.empty();
        }

        int restLevel = context.survivor().needLevel(Need.REST);

        // Personality: resilient survivors have a slightly lower threshold (endure more fatigue)
        int resilienceReduction = (context.survivor().personality().value(Trait.RESILIENT) > 70) ? 8 : 0;
        int effectiveThreshold = Math.max(config.criticalRestThreshold(),
                config.fatigueThreshold() - resilienceReduction);

        if (restLevel <= effectiveThreshold) {
            return Optional.of(new RestAction());
        }

        return Optional.empty();
    }

    @Override
    public BehaviorType type() {
        return BehaviorType.REST;
    }
}
