package com.afterfall.infrastructure.persistence.async;

import com.afterfall.application.persistence.EventRecorder;
import com.afterfall.application.persistence.RunId;
import com.afterfall.domain.EventId;
import com.afterfall.domain.RegionId;
import com.afterfall.domain.SurvivorId;
import com.afterfall.events.EventSeverity;
import com.afterfall.events.SimEvent;
import com.afterfall.infrastructure.persistence.inmemory.InMemoryEventRecorder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AsyncRecorderReliabilityTest {

    private final RunId runId = RunId.of("run-reliability-test");

    private SimEvent event(long id, EventSeverity severity) {
        return new SimEvent(
                new EventId(id),
                id,
                "RELIABILITY_EVENT",
                List.of(new SurvivorId(1)),
                Optional.of(new RegionId(1)),
                severity,
                "Summary " + id,
                Map.of(),
                List.of()
        );
    }

    @Test
    void preservesStrictFIFOOrderingAcrossRetriesAndFallback() throws Exception {
        AtomicInteger failCount = new AtomicInteger(3);
        List<Long> primaryRecordedIds = Collections.synchronizedList(new ArrayList<>());
        InMemoryEventRecorder fallback = new InMemoryEventRecorder();

        EventRecorder failingPrimary = new EventRecorder() {
            @Override
            public void record(RunId runId, SimEvent event) {
                if (failCount.decrementAndGet() > 0) {
                    throw new RuntimeException("Transient DB failure");
                }
                primaryRecordedIds.add(event.id().value());
            }

            @Override
            public void recordAll(RunId runId, List<SimEvent> events) {
                for (SimEvent e : events) record(runId, e);
            }

            @Override
            public List<SimEvent> eventsForRun(RunId runId) {
                return Collections.emptyList();
            }
        };

        // When primary fails, fallback captures events in strict FIFO order
        try (AsyncEventRecorder recorder = new AsyncEventRecorder(failingPrimary, fallback, runId, 100, 10, 20)) {
            for (long i = 1; i <= 20; i++) {
                recorder.record(runId, event(i, EventSeverity.CRITICAL));
            }
            recorder.flush();

            assertTrue(recorder.isFallbackActive(), "Fallback should be active after primary failure");

            List<SimEvent> fallbackEvents = fallback.eventsForRun(runId);
            assertEquals(20, fallbackEvents.size(), "All 20 events must be saved into fallback");
            for (int i = 0; i < 20; i++) {
                assertEquals((long) (i + 1), fallbackEvents.get(i).id().value(),
                        "Event ordering must be strictly preserved at index " + i);
            }
        }
    }

    @Test
    void verifiesGracefulShutdownLeavesNoThreadLeaks() throws Exception {
        InMemoryEventRecorder primary = new InMemoryEventRecorder();
        InMemoryEventRecorder fallback = new InMemoryEventRecorder();

        AsyncEventRecorder recorder = new AsyncEventRecorder(primary, fallback, runId, 50, 10, 10);
        for (int i = 1; i <= 30; i++) {
            recorder.record(runId, event(i, EventSeverity.CRITICAL));
        }

        Thread writer = recorder.writerThread();
        assertTrue(writer.isAlive(), "Writer thread must be running initially");

        recorder.close(); // Graceful shutdown

        // Verify critical queue flushed on close
        List<SimEvent> events = primary.eventsForRun(runId);
        assertEquals(30, events.size(), "Close must have flushed all 30 critical events");

        // Verify writer thread terminated
        assertFalse(writer.isAlive(), "Async recorder writer thread must not be alive after close()");
    }
}
