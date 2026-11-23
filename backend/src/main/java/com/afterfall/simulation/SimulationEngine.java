package com.afterfall.simulation;

import com.afterfall.domain.World;

import java.util.Objects;
import java.util.Optional;

/**
 * Facade providing the unified public API for controlling simulation execution in M3.
 *
 * <p>Coordinates:
 * <ul>
 *   <li>Lifecycle transitions via {@link SimulationLifecycle}.</li>
 *   <li>Time progression via {@link WorldClock}.</li>
 *   <li>Tick execution and evaluation via {@link TickCoordinator}.</li>
 *   <li>Publication of immutable {@link WorldView} snapshots with explicit version.</li>
 * </ul>
 *
 * <p>Strictly single-threaded: no background threads, executors, or timers.
 * In M3, lifecycle operations and ticks are intended to be invoked sequentially.
 * A pause requested via {@link #requestPause()} is honoured cleanly at the tick boundary.
 */
public final class SimulationEngine {

    private final World world;
    private final WorldClock clock;
    private final TickCoordinator coordinator;
    private SimulationLifecycle lifecycle;
    private WorldView latestSnapshot;

    /**
     * Creates a new simulation engine.
     * The engine starts in the {@link SimulationLifecycle#PAUSED} state and captures an initial baseline snapshot at tick 0 and version 0.
     */
    public SimulationEngine(World world, WorldClock clock) {
        this(world, clock, new TickCoordinator(world, clock));
    }

    public SimulationEngine(World world, WorldClock clock, TickCoordinator coordinator) {
        this.world = Objects.requireNonNull(world, "world");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
        this.lifecycle = SimulationLifecycle.PAUSED;
        // Capture initial immutable baseline snapshot at tick 0 and initial world version
        this.latestSnapshot = WorldView.snapshot(world, clock.tick(), coordinator.worldVersion());
    }

    /**
     * Starts the simulation engine, transitioning from {@link SimulationLifecycle#PAUSED} to {@link SimulationLifecycle#RUNNING}.
     *
     * @throws IllegalStateException if the engine is stopped or not in a startable state.
     */
    public synchronized void start() {
        if (lifecycle == SimulationLifecycle.RUNNING) {
            return; // Idempotent
        }
        if (lifecycle == SimulationLifecycle.STOPPED) {
            throw new IllegalStateException("Cannot start engine: simulation is STOPPED permanently");
        }
        this.lifecycle = lifecycle.transitionTo(SimulationLifecycle.RUNNING);
    }

    /**
     * Resumes the simulation from {@link SimulationLifecycle#PAUSED} to {@link SimulationLifecycle#RUNNING}.
     *
     * @throws IllegalStateException if the engine is stopped.
     */
    public synchronized void resume() {
        if (lifecycle == SimulationLifecycle.RUNNING) {
            return; // Already running
        }
        if (lifecycle == SimulationLifecycle.STOPPED) {
            throw new IllegalStateException("Cannot resume engine: simulation is STOPPED permanently");
        }
        this.lifecycle = lifecycle.transitionTo(SimulationLifecycle.RUNNING);
    }

    /**
     * Pauses the simulation engine immediately if called outside a tick.
     * If the engine is already {@link SimulationLifecycle#PAUSED}, this call is idempotent.
     *
     * @throws IllegalStateException if the engine is stopped.
     */
    public synchronized void pause() {
        if (lifecycle == SimulationLifecycle.PAUSED) {
            return; // Already paused
        }
        if (lifecycle == SimulationLifecycle.STOPPED) {
            throw new IllegalStateException("Cannot pause engine: simulation is STOPPED permanently");
        }
        this.lifecycle = SimulationLifecycle.PAUSED;
    }

    /**
     * Requests that the simulation pause at the next safe tick boundary.
     * If currently {@link SimulationLifecycle#RUNNING}, sets state to {@link SimulationLifecycle#PAUSE_REQUESTED}.
     * If already {@link SimulationLifecycle#PAUSED}, this call is idempotent.
     *
     * @throws IllegalStateException if the engine is stopped.
     */
    public synchronized void requestPause() {
        if (lifecycle == SimulationLifecycle.PAUSED) {
            return; // Already paused
        }
        if (lifecycle == SimulationLifecycle.STOPPED) {
            throw new IllegalStateException("Cannot request pause: simulation is STOPPED permanently");
        }
        if (lifecycle == SimulationLifecycle.RUNNING) {
            this.lifecycle = SimulationLifecycle.PAUSE_REQUESTED;
        }
    }

    /**
     * Stops the simulation engine permanently.
     * Transitions through STOPPING to STOPPED. Once stopped, the engine cannot resume or execute ticks,
     * and the underlying worker pool is cleanly closed.
     */
    public synchronized void stop() {
        if (lifecycle == SimulationLifecycle.STOPPED) {
            return; // Already stopped
        }
        this.lifecycle = SimulationLifecycle.STOPPING;
        coordinator.close();
        this.lifecycle = SimulationLifecycle.STOPPED;
    }

    /**
     * Executes a single simulation tick.
     *
     * @return the immutable {@link TickResult} produced by the tick coordinator.
     * @throws IllegalStateException if the engine is not in a runnable state (e.g. PAUSED or STOPPED).
     */
    public synchronized TickResult tick() {
        if (!lifecycle.canExecuteTick()) {
            throw new IllegalStateException("Cannot execute tick: engine is in state " + lifecycle);
        }

        try {
            TickResult result = coordinator.executeTick();
            if (result.isSuccess()) {
                // Update latest snapshot upon successful tick completion with updated clock and world version
                this.latestSnapshot = WorldView.snapshot(world, clock.tick(), coordinator.worldVersion());
            }
            return result;
        } finally {
            // Honour pause requests or choice moments at the tick boundary
            if (coordinator.choiceCoordinator().hasPendingPause() || lifecycle == SimulationLifecycle.PAUSE_REQUESTED) {
                if (lifecycle == SimulationLifecycle.RUNNING || lifecycle == SimulationLifecycle.PAUSE_REQUESTED) {
                    this.lifecycle = SimulationLifecycle.PAUSED;
                }
            }
        }
    }

    /**
     * Returns the latest successfully published immutable world snapshot.
     * If a tick fails, the previous successful snapshot remains intact.
     */
    public synchronized WorldView getLatestSnapshot() {
        return latestSnapshot;
    }

    /**
     * Returns the current lifecycle status of the simulation engine.
     */
    public synchronized SimulationLifecycle getStatus() {
        return lifecycle;
    }

    /**
     * Helper to inspect clock.
     */
    public WorldClock clock() {
        return clock;
    }

    /**
     * Helper to inspect coordinator.
     */
    public TickCoordinator coordinator() {
        return coordinator;
    }
}
