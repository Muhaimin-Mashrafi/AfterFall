package com.afterfall.simulation;

/**
 * State machine representing the lifecycle of the simulation engine.
 *
 * <p>Supported states:
 * <ul>
 *   <li>{@link #RUNNING}: The engine is actively executing simulation ticks.</li>
 *   <li>{@link #PAUSE_REQUESTED}: A pause has been requested; will transition to {@link #PAUSED} at the next tick boundary.</li>
 *   <li>{@link #PAUSED}: The simulation is paused at a safe tick boundary; tick executions are rejected.</li>
 *   <li>{@link #STOPPING}: The engine is terminating; finalizing current operations.</li>
 *   <li>{@link #STOPPED}: The simulation has ended permanently; cannot be resumed or restarted.</li>
 * </ul>
 *
 * <p>Initial state of a simulation engine is {@link #PAUSED} (ready to be started/resumed or run).
 */
public enum SimulationLifecycle {
    RUNNING,
    PAUSE_REQUESTED,
    PAUSED,
    STOPPING,
    STOPPED;

    /**
     * Validates whether transition from this state to the target state is permissible.
     *
     * @param target the desired next lifecycle state
     * @return true if valid according to the state machine rules
     */
    public boolean canTransitionTo(SimulationLifecycle target) {
        if (target == null) {
            return false;
        }
        if (this == target) {
            return true; // Idempotent transitions are handled gracefully
        }
        return switch (this) {
            case PAUSED -> target == RUNNING || target == STOPPING || target == STOPPED;
            case RUNNING -> target == PAUSE_REQUESTED || target == PAUSED || target == STOPPING || target == STOPPED;
            case PAUSE_REQUESTED -> target == PAUSED || target == RUNNING || target == STOPPING || target == STOPPED;
            case STOPPING -> target == STOPPED;
            case STOPPED -> false; // STOPPED is terminal
        };
    }

    /**
     * Validates and returns the target state, throwing {@link IllegalStateException} if invalid.
     *
     * @param target the desired next lifecycle state
     * @return target state if permissible
     * @throws IllegalStateException if transition is not allowed
     */
    public SimulationLifecycle transitionTo(SimulationLifecycle target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException("Illegal lifecycle transition from " + this + " to " + target);
        }
        return target;
    }

    public boolean isTerminal() {
        return this == STOPPED;
    }

    public boolean canExecuteTick() {
        return this == RUNNING || this == PAUSE_REQUESTED;
    }
}
