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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class AsyncEventRecorderTest {

    private final RunId runId = RunId.of("test-async-run");

    private SimEvent makeEvent(long id, EventSeverity severity) {
        return new SimEvent(
                new EventId(id),
                id,
                "TEST_EVENT",
                List.of(new SurvivorId(1)),
                Optional.of(new RegionId(1)),
                severity,
                "Summary " + id,
                Map.of(),
                List.of()
        );
    }

    @Test
    void nonBlockingSubmissionUnderSlowTarget() throws Exception {
        // Slow target recorder that sleeps 500 ms per write
        EventRecorder slowTarget = new EventRecorder() {
            @Override
            public void record(RunId runId, SimEvent event) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {}
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

        InMemoryEventRecorder fallback = new InMemoryEventRecorder();
        try (AsyncEventRecorder asyncRecorder = new AsyncEventRecorder(slowTarget, fallback, runId, 100, 10, 20)) {
            long start = System.currentTimeMillis();
            // Submit 10 critical events
            for (int i = 1; i <= 10; i++) {
                asyncRecorder.record(runId, makeEvent(i, EventSeverity.CRITICAL));
            }
            long elapsed = System.currentTimeMillis() - start;

            // Must complete immediately without waiting for 500ms writes (total slow write would be 5000ms)
            assertTrue(elapsed < 200, "Submission should return immediately in < 200ms, was " + elapsed + "ms");
        }
    }

    @Test
    void criticalEventsAreLossless() throws Exception {
        InMemoryEventRecorder primary = new InMemoryEventRecorder();
        InMemoryEventRecorder fallback = new InMemoryEventRecorder();

        try (AsyncEventRecorder asyncRecorder = new AsyncEventRecorder(primary, fallback, runId, 50, 10, 10)) {
            int count = 500;
            for (int i = 1; i <= count; i++) {
                asyncRecorder.record(runId, makeEvent(i, EventSeverity.CRITICAL));
            }
            asyncRecorder.flush();

            List<SimEvent> recorded = primary.eventsForRun(runId);
            assertEquals(count, recorded.size(), "Zero critical events should be lost");
            assertEquals(0, asyncRecorder.droppedMinorCount());
        }
    }

    @Test
    void minorEventsDropOnQueueOverflowAndIncrementCounter() throws Exception {
        // Target that pauses write until released
        CountDownLatch pauseLatch = new CountDownLatch(1);
        EventRecorder pausedTarget = new EventRecorder() {
            @Override
            public void record(RunId runId, SimEvent event) {
                try {
                    pauseLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {}
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

        InMemoryEventRecorder fallback = new InMemoryEventRecorder();
        int capacity = 20;

        try (AsyncEventRecorder asyncRecorder = new AsyncEventRecorder(pausedTarget, fallback, runId, capacity, 10, 10)) {
            // Offer 100 minor events while writer is blocked
            for (int i = 1; i <= 100; i++) {
                asyncRecorder.record(runId, makeEvent(i, EventSeverity.MINOR));
            }

            assertTrue(asyncRecorder.droppedMinorCount() > 0, "Minor events exceeding capacity must be counted as dropped");
            pauseLatch.countDown();
        }
    }

    @Test
    void switchesToFallbackOnDatabaseFailure() throws Exception {
        AtomicBoolean fail = new AtomicBoolean(true);
        EventRecorder failingPrimary = new EventRecorder() {
            @Override
            public void record(RunId runId, SimEvent event) {
                if (fail.get()) {
                    throw new RuntimeException("PostgreSQL connection refused");
                }
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

        InMemoryEventRecorder fallback = new InMemoryEventRecorder();
        try (AsyncEventRecorder asyncRecorder = new AsyncEventRecorder(failingPrimary, fallback, runId, 100, 10, 10)) {
            asyncRecorder.record(runId, makeEvent(1, EventSeverity.CRITICAL));
            asyncRecorder.record(runId, makeEvent(2, EventSeverity.SIGNIFICANT));

            // Wait a moment for drain loop
            Thread.sleep(100);
            asyncRecorder.flush();

            assertTrue(asyncRecorder.isFallbackActive(), "Fallback must be activated on failure");
            List<SimEvent> fallbackEvents = fallback.eventsForRun(runId);
            assertEquals(2, fallbackEvents.size());
        }
    }

    @Test
    void cleanShutdownFlushesPendingCriticalEvents() throws Exception {
        InMemoryEventRecorder primary = new InMemoryEventRecorder();
        InMemoryEventRecorder fallback = new InMemoryEventRecorder();

        AsyncEventRecorder asyncRecorder = new AsyncEventRecorder(primary, fallback, runId, 100, 10, 50);
        for (int i = 1; i <= 50; i++) {
            asyncRecorder.record(runId, makeEvent(i, EventSeverity.CRITICAL));
        }

        // Close immediately without manual flush
        asyncRecorder.close();

        List<SimEvent> recorded = primary.eventsForRun(runId);
        assertEquals(50, recorded.size(), "Close must flush all pending critical events");
    }
}
