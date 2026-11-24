package com.afterfall.infrastructure.persistence;

import com.afterfall.application.persistence.EventRecorder;
import com.afterfall.application.persistence.RunId;
import com.afterfall.domain.EventId;
import com.afterfall.domain.RegionId;
import com.afterfall.domain.SurvivorId;
import com.afterfall.events.EventSeverity;
import com.afterfall.events.SimEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reusable contract test suite for {@link EventRecorder} implementations.
 *
 * <p>Both {@code InMemoryEventRecorder} and {@code JpaEventRecorder} must pass all contract tests identically.
 */
public abstract class AbstractEventRecorderContractTest {

    protected abstract EventRecorder createRecorder();

    private EventRecorder recorder;
    private RunId runId;

    @BeforeEach
    void setUp() {
        recorder = createRecorder();
        runId = RunId.of("run-contract-" + System.nanoTime());
    }

    private SimEvent sampleEvent(long id, long tick, EventSeverity severity, String type) {
        return new SimEvent(
                new EventId(id),
                tick,
                type,
                List.of(new SurvivorId(1)),
                Optional.of(new RegionId(1)),
                severity,
                "Summary for event " + id,
                Map.of("key", "val-" + id),
                List.of(new EventId(id > 1 ? id - 1 : 1))
        );
    }

    @Test
    void recordsAndRetrievesInCanonicalOrder() {
        SimEvent e3 = sampleEvent(3, 10, EventSeverity.MINOR, "ROUTINE");
        SimEvent e1 = sampleEvent(1, 5, EventSeverity.CRITICAL, "CRISIS");
        SimEvent e2 = sampleEvent(2, 7, EventSeverity.SIGNIFICANT, "CONFLICT");

        // Record out of order
        recorder.record(runId, e3);
        recorder.record(runId, e1);
        recorder.record(runId, e2);
        recorder.flush();

        List<SimEvent> events = recorder.eventsForRun(runId);
        assertEquals(3, events.size());
        assertEquals(1L, events.get(0).id().value());
        assertEquals(2L, events.get(1).id().value());
        assertEquals(3L, events.get(2).id().value());
        assertEquals(EventSeverity.CRITICAL, events.get(0).severity());
        assertEquals("CRISIS", events.get(0).eventType());
    }

    @Test
    void isIdempotentByEventId() {
        SimEvent e1 = sampleEvent(1, 5, EventSeverity.MINOR, "ROUTINE");
        SimEvent duplicate = sampleEvent(1, 5, EventSeverity.CRITICAL, "UPDATED");

        recorder.record(runId, e1);
        recorder.record(runId, duplicate); // duplicate event ID
        recorder.flush();

        List<SimEvent> events = recorder.eventsForRun(runId);
        assertEquals(1, events.size());
        assertEquals(1L, events.get(0).id().value());
        assertEquals(EventSeverity.MINOR, events.get(0).severity(), "First recorded event must be preserved");
    }

    @Test
    void recordAllMaintainsOrderAndIdempotency() {
        SimEvent e1 = sampleEvent(1, 1, EventSeverity.MINOR, "A");
        SimEvent e2 = sampleEvent(2, 2, EventSeverity.MINOR, "B");
        SimEvent e3 = sampleEvent(3, 3, EventSeverity.MINOR, "C");

        recorder.recordAll(runId, List.of(e1, e2, e3, e2));
        recorder.flush();

        List<SimEvent> events = recorder.eventsForRun(runId);
        assertEquals(3, events.size());
        assertEquals(List.of(1L, 2L, 3L), events.stream().map(e -> e.id().value()).toList());
    }

    @Test
    void isolatesDifferentRuns() {
        RunId run2 = RunId.of("run-other");
        SimEvent e1 = sampleEvent(1, 1, EventSeverity.MINOR, "A");
        SimEvent e2 = sampleEvent(2, 2, EventSeverity.MINOR, "B");

        recorder.record(runId, e1);
        recorder.record(run2, e2);
        recorder.flush();

        List<SimEvent> r1Events = recorder.eventsForRun(runId);
        List<SimEvent> r2Events = recorder.eventsForRun(run2);

        assertEquals(1, r1Events.size());
        assertEquals(1L, r1Events.get(0).id().value());
        assertEquals(1, r2Events.size());
        assertEquals(2L, r2Events.get(0).id().value());
    }
}
