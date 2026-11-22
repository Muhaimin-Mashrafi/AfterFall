package com.afterfall.action;

/**
 * Command Pattern interface for survivor actions.
 *
 * <p>In accordance with the deterministic evaluation contract, an {@code Action} evaluates
 * an immutable {@link ActionContext} and returns an immutable {@link ActionResult}. It never
 * mutates domain entities, personal inventories, or shared world state directly.
 */
public interface Action {

    /**
     * Evaluates this action against the provided immutable context.
     *
     * @param context the immutable action context
     * @return an immutable action result describing proposed changes and events
     */
    ActionResult evaluate(ActionContext context);

    /**
     * The type of this action.
     */
    ActionType type();
}
