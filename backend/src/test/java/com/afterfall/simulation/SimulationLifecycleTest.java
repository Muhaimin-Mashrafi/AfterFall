package com.afterfall.simulation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationLifecycleTest {

    @Test
    void testInitialStateAndTransitions() {
        SimulationLifecycle state = SimulationLifecycle.PAUSED;
        assertFalse(state.canExecuteTick(), "PAUSED cannot execute ticks directly");
        assertFalse(state.isTerminal());

        // PAUSED -> RUNNING
        SimulationLifecycle running = state.transitionTo(SimulationLifecycle.RUNNING);
        assertEquals(SimulationLifecycle.RUNNING, running);
        assertTrue(running.canExecuteTick());

        // RUNNING -> PAUSE_REQUESTED
        SimulationLifecycle pauseReq = running.transitionTo(SimulationLifecycle.PAUSE_REQUESTED);
        assertEquals(SimulationLifecycle.PAUSE_REQUESTED, pauseReq);
        assertTrue(pauseReq.canExecuteTick(), "PAUSE_REQUESTED can finish current tick");

        // PAUSE_REQUESTED -> PAUSED
        SimulationLifecycle paused = pauseReq.transitionTo(SimulationLifecycle.PAUSED);
        assertEquals(SimulationLifecycle.PAUSED, paused);

        // PAUSED -> STOPPING -> STOPPED
        SimulationLifecycle stopping = paused.transitionTo(SimulationLifecycle.STOPPING);
        assertEquals(SimulationLifecycle.STOPPING, stopping);
        SimulationLifecycle stopped = stopping.transitionTo(SimulationLifecycle.STOPPED);
        assertEquals(SimulationLifecycle.STOPPED, stopped);
        assertTrue(stopped.isTerminal());
    }

    @Test
    void testInvalidTransitionsFromStopped() {
        SimulationLifecycle stopped = SimulationLifecycle.STOPPED;
        assertFalse(stopped.canTransitionTo(SimulationLifecycle.RUNNING));
        assertFalse(stopped.canTransitionTo(SimulationLifecycle.PAUSED));
        assertFalse(stopped.canTransitionTo(SimulationLifecycle.PAUSE_REQUESTED));
        assertFalse(stopped.canTransitionTo(SimulationLifecycle.STOPPING));

        assertThrows(IllegalStateException.class, () -> stopped.transitionTo(SimulationLifecycle.RUNNING));
        assertThrows(IllegalStateException.class, () -> stopped.transitionTo(SimulationLifecycle.PAUSED));
    }

    @Test
    void testIdempotentTransitions() {
        assertTrue(SimulationLifecycle.RUNNING.canTransitionTo(SimulationLifecycle.RUNNING));
        assertTrue(SimulationLifecycle.PAUSED.canTransitionTo(SimulationLifecycle.PAUSED));
        assertTrue(SimulationLifecycle.STOPPED.canTransitionTo(SimulationLifecycle.STOPPED));
    }
}
