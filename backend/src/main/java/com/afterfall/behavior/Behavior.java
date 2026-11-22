package com.afterfall.behavior;

import com.afterfall.action.Action;
import java.util.Optional;

/**
 * Strategy Pattern interface for autonomous survivor decision-making.
 *
 * <p>Each behavior inspects an immutable {@link BehaviorContext} and selects an appropriate
 * {@link Action} to execute. Behaviors are deterministic, stateless, and do not mutate domain entities.
 */
public interface Behavior {

    /**
     * Determines whether this behavior is applicable and selects an appropriate action.
     *
     * @param context the immutable decision context
     * @return an optional containing the selected action, or empty if this behavior is inapplicable
     */
    Optional<Action> decide(BehaviorContext context);

    /**
     * The type of this behavior strategy.
     */
    BehaviorType type();
}
