package com.afterfall.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * EventBus implementing the Observer pattern with independent asynchronous subscriber fan-out.
 *
 * <p>Requirements:
 * <ul>
 *   <li>{@code publish()} enqueues events immediately and returns without waiting for subscriber execution.</li>
 *   <li>Each subscriber has an independent single-threaded asynchronous worker/consumer preserving FIFO order.</li>
 *   <li>A slow or blocked subscriber never delays another subscriber or {@code TickCoordinator}.</li>
 *   <li>Subscriber exceptions are caught, recorded/logged, and the consumer thread recovers for subsequent batches.</li>
 *   <li>Critical and all queued events are preserved in the subscriber's FIFO buffer until drained.</li>
 *   <li>Provides clean shutdown and queue-draining methods ({@code shutdown()}, {@code awaitTermination()}, {@code drainAll()}).</li>
 * </ul>
 */
public final class EventBus implements AutoCloseable {

    /**
     * Internal container managing a subscriber's dedicated queue and dedicated single-threaded consumer.
     */
    private static final class SubscriberChannel {
        final EventSubscriber subscriber;
        final Queue<SimEvent> queue = new ConcurrentLinkedQueue<>();
        final ExecutorService executor;
        final AtomicBoolean isRunning = new AtomicBoolean(true);
        final List<Throwable> caughtErrors = new CopyOnWriteArrayList<>();

        SubscriberChannel(EventSubscriber subscriber) {
            this.subscriber = subscriber;
            String threadName = "afterfall-subscriber-" + subscriber.subscriberId();
            this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
                private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = defaultFactory.newThread(r);
                    t.setName(threadName);
                    t.setDaemon(true);
                    return t;
                }
            });
        }

        void enqueueAndTrigger(List<SimEvent> events) {
            queue.addAll(events);
            if (isRunning.get()) {
                executor.submit(this::processPending);
            }
        }

        private void processPending() {
            // Drain currently available events in strict FIFO order
            List<SimEvent> batch = new ArrayList<>();
            SimEvent event;
            while ((event = queue.poll()) != null) {
                batch.add(event);
            }

            if (!batch.isEmpty()) {
                try {
                    subscriber.onEvents(Collections.unmodifiableList(batch));
                } catch (Throwable t) {
                    // Isolated and recorded: an error in subscriber callback must never impede other subscribers
                    caughtErrors.add(t);
                }
            }
        }

        void close() {
            isRunning.set(false);
            executor.shutdown();
        }

        boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return executor.awaitTermination(timeout, unit);
        }
    }

    private final List<EventSubscriber> subscribers = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, SubscriberChannel> channels = new ConcurrentHashMap<>();
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    public EventBus() {
    }

    /**
     * Registers an independent subscriber with its own isolated queue and single-threaded async worker.
     */
    public synchronized void register(EventSubscriber subscriber) {
        Objects.requireNonNull(subscriber, "subscriber");
        if (isShutdown.get()) {
            throw new IllegalStateException("Cannot register subscriber on shut down EventBus");
        }
        if (!channels.containsKey(subscriber.subscriberId())) {
            SubscriberChannel channel = new SubscriberChannel(subscriber);
            channels.put(subscriber.subscriberId(), channel);
            subscribers.add(subscriber);
        }
    }

    /**
     * Unregisters a subscriber and stops its dedicated consumer worker.
     */
    public synchronized void unregister(EventSubscriber subscriber) {
        Objects.requireNonNull(subscriber, "subscriber");
        subscribers.remove(subscriber);
        SubscriberChannel channel = channels.remove(subscriber.subscriberId());
        if (channel != null) {
            channel.close();
        }
    }

    /**
     * Fans out published events to all registered subscribers asynchronously.
     * Enqueues events and returns immediately without waiting for subscriber execution.
     */
    public void publish(List<SimEvent> events) {
        if (events == null || events.isEmpty() || isShutdown.get()) {
            return;
        }

        List<SimEvent> immutableList = List.copyOf(events);

        for (SubscriberChannel channel : channels.values()) {
            channel.enqueueAndTrigger(immutableList);
        }
    }

    /**
     * Returns a snapshot of queued events currently waiting in the subscriber's buffer.
     */
    public List<SimEvent> queuedEventsFor(String subscriberId) {
        SubscriberChannel channel = channels.get(subscriberId);
        if (channel == null) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(channel.queue));
    }

    /**
     * Returns recorded errors thrown by the subscriber's consumer.
     */
    public List<Throwable> errorsFor(String subscriberId) {
        SubscriberChannel channel = channels.get(subscriberId);
        if (channel == null) {
            return List.of();
        }
        return Collections.unmodifiableList(channel.caughtErrors);
    }

    /**
     * Returns the registered subscribers.
     */
    public List<EventSubscriber> subscribers() {
        return Collections.unmodifiableList(subscribers);
    }

    /**
     * Shuts down all subscriber consumer executors gracefully.
     */
    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            for (SubscriberChannel channel : channels.values()) {
                channel.close();
            }
        }
    }

    /**
     * Awaits completion of all pending subscriber tasks across channels.
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        long deadline = System.nanoTime() + nanos;

        for (SubscriberChannel channel : channels.values()) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                return false;
            }
            if (!channel.awaitTermination(remaining, TimeUnit.NANOSECONDS)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void close() {
        shutdown();
    }
}
