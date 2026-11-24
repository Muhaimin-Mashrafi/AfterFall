package com.afterfall.application.persistence;

import com.afterfall.events.SimEvent;

import java.util.List;

/**
 * Persistence port for recording canonical simulation events.
 *
 * <p>Implemented by both {@code JpaEventRecorder} (production) and {@code InMemoryEventRecorder}
 * (tests and live fallback).
 */
public interface EventRecorder {

    /**
     * Records a single canonical simulation event.
     * Implementations must be idempotent based on {@code event.id()}.
     *
     * @param runId the simulation run identifier
     * @param event the immutable simulation event
     */
    void record(RunId runId, SimEvent event);

    /**
     * Records a batch of canonical simulation events in order.
     * Implementations must be idempotent based on {@code event.id()}.
     *
     * @param runId  the simulation run identifier
     * @param events the ordered list of simulation events
     */
    void recordAll(RunId runId, List<SimEvent> events);

    /**
     * Retrieves all recorded simulation events for a run in canonical sequential order.
     *
     * @param runId the simulation run identifier
     * @return ordered list of simulation events
     */
    List<SimEvent> eventsForRun(RunId runId);

    /**
     * Clears or flushes internal buffers if applicable.
     */
    default void flush() {}
}
