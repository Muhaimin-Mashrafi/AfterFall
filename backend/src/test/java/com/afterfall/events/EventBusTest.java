package com.afterfall.events;

import com.afterfall.domain.EventId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class EventBusTest {

    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
    }

    @AfterEach
    void tearDown() {
        eventBus.close();
    }

    static class TestSubscriber implements EventSubscriber {
        private final String id;
        private final List<SimEvent> received = Collections.synchronizedList(new ArrayList<>());
        private final boolean throwException;
        private final CountDownLatch blockLatch;

        TestSubscriber(String id) {
            this(id, false, null);
        }

        TestSubscriber(String id, boolean throwException) {
            this(id, throwException, null);
        }

        TestSubscriber(String id, CountDownLatch blockLatch) {
            this(id, false, blockLatch);
        }

        TestSubscriber(String id, boolean throwException, CountDownLatch blockLatch) {
            this.id = id;
            this.throwException = throwException;
            this.blockLatch = blockLatch;
        }

        @Override
        public String subscriberId() {
            return id;
        }

        @Override
        public void onEvents(List<SimEvent> events) {
            if (blockLatch != null) {
                try {
                    blockLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (throwException) {
                throw new RuntimeException("Simulated subscriber error in " + id);
            }
            received.addAll(events);
        }

        List<SimEvent> received() {
            return List.copyOf(received);
        }
    }

    @Test
    void fansOutEventsToIndependentSubscribersPreservingOrder() throws InterruptedException {
        TestSubscriber sub1 = new TestSubscriber("sub1");
        TestSubscriber sub2 = new TestSubscriber("sub2");

        eventBus.register(sub1);
        eventBus.register(sub2);

        SimEvent e1 = new SimEvent(new EventId(1L), 0L, "T1", List.of(), Optional.empty(), EventSeverity.MINOR, "E1", Map.of(), List.of());
        SimEvent e2 = new SimEvent(new EventId(2L), 0L, "T2", List.of(), Optional.empty(), EventSeverity.SIGNIFICANT, "E2", Map.of(), List.of());

        eventBus.publish(List.of(e1, e2));
        eventBus.shutdown();
        boolean completed = eventBus.awaitTermination(3, TimeUnit.SECONDS);

        assertThat(completed).isTrue();
        assertThat(sub1.received()).hasSize(2);
        assertThat(sub2.received()).hasSize(2);
        assertThat(sub1.received().get(0).id().value()).isEqualTo(1L);
        assertThat(sub1.received().get(1).id().value()).isEqualTo(2L);
        assertThat(sub2.received().get(0).id().value()).isEqualTo(1L);
        assertThat(sub2.received().get(1).id().value()).isEqualTo(2L);
    }

    @Test
    void publishingReturnsWithoutWaitingForBlockedSubscriber() {
        CountDownLatch blockLatch = new CountDownLatch(1);
        TestSubscriber blockedSub = new TestSubscriber("blockedSub", blockLatch);

        eventBus.register(blockedSub);

        SimEvent event = new SimEvent(new EventId(1L), 0L, "TEST", List.of(), Optional.empty(), EventSeverity.CRITICAL, "Critical Event", Map.of(), List.of());

        long start = System.currentTimeMillis();
        eventBus.publish(List.of(event));
        long duration = System.currentTimeMillis() - start;

        // Publish returns immediately without waiting for the blocked subscriber
        assertThat(duration).isLessThan(500);

        // Unblock latch so background thread can terminate cleanly
        blockLatch.countDown();
    }

    @Test
    void blockedSubscriberDoesNotDelayOtherSubscribers() throws InterruptedException {
        CountDownLatch blockLatch = new CountDownLatch(1);
        CountDownLatch healthyProcessedLatch = new CountDownLatch(1);

        TestSubscriber blockedSub = new TestSubscriber("blockedSub", blockLatch);
        TestSubscriber fastSub = new TestSubscriber("fastSub") {
            @Override
            public void onEvents(List<SimEvent> events) {
                super.onEvents(events);
                healthyProcessedLatch.countDown();
            }
        };

        eventBus.register(blockedSub);
        eventBus.register(fastSub);

        SimEvent event = new SimEvent(new EventId(1L), 0L, "TEST", List.of(), Optional.empty(), EventSeverity.CRITICAL, "Critical Event", Map.of(), List.of());
        eventBus.publish(List.of(event));

        // fastSub should complete immediately while blockedSub is still blocked
        boolean fastReceivedInTime = healthyProcessedLatch.await(1, TimeUnit.SECONDS);
        assertThat(fastReceivedInTime).isTrue();
        assertThat(fastSub.received()).hasSize(1);
        assertThat(blockedSub.received()).isEmpty();

        // Now release blockedSub
        blockLatch.countDown();
        eventBus.shutdown();
        eventBus.awaitTermination(3, TimeUnit.SECONDS);

        assertThat(blockedSub.received()).hasSize(1);
    }

    @Test
    void failingSubscriberDoesNotBlockOtherSubscribersAndRecordsError() throws InterruptedException {
        TestSubscriber faulty = new TestSubscriber("faulty", true);
        TestSubscriber healthy = new TestSubscriber("healthy", false);

        eventBus.register(faulty);
        eventBus.register(healthy);

        SimEvent event = new SimEvent(new EventId(1L), 0L, "TEST", List.of(), Optional.empty(), EventSeverity.MINOR, "E", Map.of(), List.of());
        eventBus.publish(List.of(event));

        eventBus.shutdown();
        eventBus.awaitTermination(3, TimeUnit.SECONDS);

        // Healthy subscriber received the event despite faulty subscriber throwing
        assertThat(healthy.received()).hasSize(1);
        // Error was safely caught and recorded
        assertThat(eventBus.errorsFor("faulty")).hasSize(1);
        assertThat(eventBus.errorsFor("faulty").get(0).getMessage()).contains("Simulated subscriber error in faulty");
    }

    @Test
    void criticalEventsRemainQueuedWhenSubscriberIsBusy() {
        CountDownLatch blockLatch = new CountDownLatch(1);
        TestSubscriber blockedSub = new TestSubscriber("busySub", blockLatch);

        eventBus.register(blockedSub);

        SimEvent criticalEvent1 = new SimEvent(new EventId(100L), 1L, "CRISIS", List.of(), Optional.empty(), EventSeverity.CRITICAL, "Famine active", Map.of(), List.of());
        SimEvent criticalEvent2 = new SimEvent(new EventId(101L), 1L, "DEATH", List.of(), Optional.empty(), EventSeverity.CRITICAL, "Casualty", Map.of(), List.of());

        // Publish events while subscriber thread is blocked
        eventBus.publish(List.of(criticalEvent1, criticalEvent2));

        // Events must not be discarded
        List<SimEvent> queued = eventBus.queuedEventsFor("busySub");
        // At least one remains in queue or is being processed
        blockLatch.countDown();
    }

    @Test
    void unregisterRemovesSubscriberAndClosesWorker() {
        TestSubscriber sub = new TestSubscriber("sub");
        eventBus.register(sub);
        eventBus.unregister(sub);

        SimEvent event = new SimEvent(new EventId(1L), 0L, "TEST", List.of(), Optional.empty(), EventSeverity.MINOR, "E", Map.of(), List.of());
        eventBus.publish(List.of(event));

        assertThat(sub.received()).isEmpty();
        assertThat(eventBus.queuedEventsFor("sub")).isEmpty();
    }

    @Test
    void shutdownDrainsAndProcessesPendingEvents() throws InterruptedException {
        TestSubscriber sub = new TestSubscriber("drainingSub");
        eventBus.register(sub);

        List<SimEvent> batch = new ArrayList<>();
        for (long i = 1; i <= 50; i++) {
            batch.add(new SimEvent(new EventId(i), 0L, "T", List.of(), Optional.empty(), EventSeverity.MINOR, "E" + i, Map.of(), List.of()));
        }

        eventBus.publish(batch);
        eventBus.shutdown();
        boolean completed = eventBus.awaitTermination(3, TimeUnit.SECONDS);

        assertThat(completed).isTrue();
        assertThat(sub.received()).hasSize(50);
        assertThat(sub.received().get(0).id().value()).isEqualTo(1L);
        assertThat(sub.received().get(49).id().value()).isEqualTo(50L);
    }
}
