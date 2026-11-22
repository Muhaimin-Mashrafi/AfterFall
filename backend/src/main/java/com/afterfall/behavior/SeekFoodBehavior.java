package com.afterfall.behavior;

import com.afterfall.action.Action;
import com.afterfall.action.EatAction;
import com.afterfall.domain.ItemType;
import com.afterfall.domain.Need;
import com.afterfall.domain.Trait;
import java.util.Objects;
import java.util.Optional;

/**
 * Strategy for seeking food when hungry.
 *
 * <p>Prioritizes eating when the FOOD need drops below the hunger threshold. Personality traits
 * (e.g. FEARFUL or SELF_INTERESTED) slightly increase sensitivity to hunger, while critical hunger
 * takes precedence regardless of personality.
 */
public final class SeekFoodBehavior implements Behavior {

    private final BehaviorConfig config;

    public SeekFoodBehavior() {
        this(BehaviorConfig.defaultConfig());
    }

    public SeekFoodBehavior(BehaviorConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public Optional<Action> decide(BehaviorContext context) {
        Objects.requireNonNull(context, "context");

        if (!context.survivor().isAlive()) {
            return Optional.empty();
        }

        int foodLevel = context.survivor().needLevel(Need.FOOD);

        // Personality influence: higher fearful/self-interested traits increase appetite threshold by up to 10
        int fearfulBonus = (context.survivor().personality().value(Trait.FEARFUL) > 60) ? 5 : 0;
        int selfInterestedBonus = (context.survivor().personality().value(Trait.SELF_INTERESTED) > 60) ? 5 : 0;
        int effectiveThreshold = config.hungerThreshold() + fearfulBonus + selfInterestedBonus;

        if (foodLevel <= effectiveThreshold) {
            boolean hasFood = context.survivor().hasItem(ItemType.FOOD, 1) || context.foodAvailable();
            if (hasFood) {
                return Optional.of(new EatAction());
            }
        }

        return Optional.empty();
    }

    @Override
    public BehaviorType type() {
        return BehaviorType.SEEK_FOOD;
    }
}
