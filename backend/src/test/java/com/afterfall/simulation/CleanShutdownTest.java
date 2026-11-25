package com.afterfall.simulation;

import com.afterfall.application.persistence.RunId;
import com.afterfall.domain.EventId;
import com.afterfall.domain.RegionId;
import com.afterfall.domain.ResourceType;
import com.afterfall.domain.Settlement;
import com.afterfall.domain.SurvivorId;
import com.afterfall.domain.World;
import com.afterfall.events.EventBus;
import com.afterfall.events.EventSeverity;
import com.afterfall.events.EventSubscriber;
import com.afterfall.events.SimEvent;
import com.afterfall.infrastructure.persistence.async.AsyncEventRecorder;
import com.afterfall.infrastructure.persistence.inmemory.InMemoryEventRecorder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M12 Verification Test: Clean Shutdown & Resource Termination (Blueprint 17.3).
 *
 * <p>Verifies that:
 * <ul>
 *   <li>RegionWorkerPool cleanly terminates all 4 worker threads.</li>
 *   <li>EventBus terminates all dedicated asynchronous subscriber threads.</li>
 *   <li>AsyncEventRecorder drains pending events and stops the async writer thread.</li>
 *   <li>SimulationEngine cleanly terminates without leaking application-owned threads.</li>
 * </ul>
 */
class CleanShutdownTest {

    @Test
    @DisplayName("RegionWorkerPool cleanly terminates without leaked worker threads")
    void testRegionWorkerPoolCleanShutdown() throws InterruptedException {
        // Capture live threads before pool instantiation
        Set<Thread> initialThreads = getLiveThreadsMatching("afterfall-region-");

        RegionWorkerPool pool = new RegionWorkerPool();
        assertEquals(4, pool.poolSize());
        assertFalse(pool.isShutdown());
        assertFalse(pool.isTerminated());

        // Pool closes cleanly
        pool.close();
        assertTrue(pool.isShutdown());
        assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS));
        assertTrue(pool.isTerminated());

        // Verify no newly spawned worker threads leaked
        Set<Thread> remainingThreads = getLiveThreadsMatching("afterfall-region-");
        remainingThreads.removeAll(initialThreads);
        assertTrue(remainingThreads.isEmpty(), "Leaked worker threads: " + remainingThreads);
    }

    @Test
    @DisplayName("EventBus cleanly drains and terminates all subscriber channels")
    void testEventBusCleanShutdown() throws InterruptedException {
        EventBus bus = new EventBus();
        CountDownLatch latch = new CountDownLatch(5);

        EventSubscriber subscriber = new EventSubscriber() {
            @Override
            public String subscriberId() {
                return "test-clean-sub";
            }

            @Override
            public void onEvents(List<SimEvent> events) {
                for (SimEvent e : events) {
                    latch.countDown();
                }
            }
        };

        bus.register(subscriber);

        for (int i = 1; i <= 5; i++) {
            SimEvent ev = new SimEvent(
                    new EventId(i),
                    1L,
                    "TEST_EVENT",
                    Collections.emptyList(),
                    Optional.of(RegionId.REGION_1),
                    EventSeverity.SIGNIFICANT,
                    "Clean test event " + i,
                    Map.of(),
                    Collections.emptyList()
            );
            bus.publish(List.of(ev));
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS), "Subscriber did not receive all events");

        // Close EventBus and await thread termination
        bus.close();
        assertTrue(bus.awaitTermination(2, TimeUnit.SECONDS));

        // Verify afterfall-subscriber thread terminated
        Set<Thread> liveThreads = getLiveThreadsMatching("afterfall-subscriber-test-clean-sub");
        assertTrue(liveThreads.isEmpty(), "Leaked subscriber threads: " + liveThreads);
    }

    @Test
    @DisplayName("AsyncEventRecorder flushes queues and terminates background writer thread")
    void testAsyncEventRecorderCleanShutdown() throws InterruptedException {
        InMemoryEventRecorder target = new InMemoryEventRecorder();
        RunId runId = RunId.of("clean-shutdown-run");
        AsyncEventRecorder recorder = new AsyncEventRecorder(target, target, runId, 50, 10, 20);
        Thread writer = recorder.writerThread();

        for (int i = 1; i <= 20; i++) {
            SimEvent ev = new SimEvent(
                    new EventId(i),
                    1L,
                    "METRIC_EVENT",
                    Collections.emptyList(),
                    Optional.of(RegionId.REGION_1),
                    i % 2 == 0 ? EventSeverity.CRITICAL : EventSeverity.MINOR,
                    "Data " + i,
                    Map.of(),
                    Collections.emptyList()
            );
            recorder.record(runId, ev);
        }

        recorder.close();

        // Target received all events
        List<SimEvent> recorded = target.eventsForRun(runId);
        assertEquals(20, recorded.size());

        // Writer thread is no longer alive
        assertFalse(writer.isAlive(), "Async recorder writer thread did not terminate");
    }

    @Test
    @DisplayName("Full SimulationEngine teardown stops all thread pools cleanly")
    void testSimulationEngineTeardown() throws InterruptedException {
        Settlement settlement = new Settlement("Shutdown Settlement");
        World world = new World(settlement);
        WorldClock clock = new WorldClock(4);
        RegionWorkerPool pool = new RegionWorkerPool();
        TickCoordinator coordinator = new TickCoordinator(
                world,
                clock,
                ctx -> null,
                pool,
                true,
                new SeededRandomFactory(42L)
        );

        SimulationEngine engine = new SimulationEngine(world, clock, coordinator);
        assertEquals(SimulationLifecycle.PAUSED, engine.getStatus());

        engine.start();
        assertEquals(SimulationLifecycle.RUNNING, engine.getStatus());

        engine.stop();
        assertEquals(SimulationLifecycle.STOPPED, engine.getStatus());

        coordinator.shutdown();
        assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS));
        assertTrue(pool.isTerminated());
    }

    private Set<Thread> getLiveThreadsMatching(String namePrefix) {
        Set<Thread> matching = ConcurrentHashMap.newKeySet();
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.isAlive() && t.getName() != null && t.getName().startsWith(namePrefix)) {
                matching.add(t);
            }
        }
        return matching;
    }
}
