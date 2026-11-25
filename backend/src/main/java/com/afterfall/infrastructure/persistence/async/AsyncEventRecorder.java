package com.afterfall.infrastructure.persistence.async;

import com.afterfall.application.persistence.EventRecorder;
import com.afterfall.application.persistence.RunId;
import com.afterfall.events.EventSeverity;
import com.afterfall.events.EventSubscriber;
import com.afterfall.events.SimEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Decorating {@link EventRecorder} and {@link EventSubscriber} that asynchronously
 * drains events using separate queues:
 *
 * <ul>
 *   <li><b>Critical Queue:</b> Lossless queue for {@link EventSeverity#CRITICAL} and
 *       {@link EventSeverity#SIGNIFICANT} events. Never intentionally drops records.</li>
 *   <li><b>Minor Queue:</b> Bounded queue for {@link EventSeverity#MINOR} routine events.
 *       Drops and counts minor events on overflow to protect simulation performance.</li>
 * </ul>
 *
 * <p>Supports non-blocking event submission, batch draining, in-memory fallback on database
 * errors, and graceful shutdown flushing all critical records.
 */
public final class AsyncEventRecorder implements EventRecorder, EventSubscriber, AutoCloseable {

    private record QueuedItem(RunId runId, SimEvent event) {}

    private final EventRecorder primaryTarget;
    private final EventRecorder fallbackTarget;
    private final RunId defaultRunId;

    // Queues
    private final Queue<QueuedItem> criticalQueue = new ConcurrentLinkedQueue<>();
    private final BlockingQueue<QueuedItem> minorQueue;

    // Metrics & State
    private final AtomicLong droppedMinorCount = new AtomicLong(0);
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final AtomicBoolean fallbackActive = new AtomicBoolean(false);
    private final AtomicLong totalRecordedCount = new AtomicLong(0);

    private final Thread writerThread;
    private final int batchSize;
    private final long flushIntervalMillis;

    private static final java.util.concurrent.atomic.AtomicInteger INSTANCE_COUNTER = new java.util.concurrent.atomic.AtomicInteger(0);

    public AsyncEventRecorder(EventRecorder primaryTarget,
                              EventRecorder fallbackTarget,
                              RunId defaultRunId,
                              int minorQueueCapacity,
                              int batchSize,
                              long flushIntervalMillis) {
        this.primaryTarget = Objects.requireNonNull(primaryTarget, "primaryTarget");
        this.fallbackTarget = Objects.requireNonNull(fallbackTarget, "fallbackTarget");
        this.defaultRunId = Objects.requireNonNull(defaultRunId, "defaultRunId");
        this.minorQueue = new LinkedBlockingQueue<>(Math.max(10, minorQueueCapacity));
        this.batchSize = Math.max(1, batchSize);
        this.flushIntervalMillis = Math.max(1, flushIntervalMillis);

        String threadName = "afterfall-async-recorder-" + INSTANCE_COUNTER.incrementAndGet();
        this.writerThread = new Thread(this::drainLoop, threadName);
        this.writerThread.setDaemon(true);
        this.writerThread.start();
    }

    @Override
    public String subscriberId() {
        return "async-recorder";
    }

    @Override
    public void onEvents(List<SimEvent> events) {
        recordAll(defaultRunId, events);
    }

    @Override
    public void record(RunId runId, SimEvent event) {
        if (event == null || !isRunning.get()) {
            return;
        }
        RunId targetRun = runId != null ? runId : defaultRunId;
        QueuedItem item = new QueuedItem(targetRun, event);

        if (event.severity() == EventSeverity.CRITICAL || event.severity() == EventSeverity.SIGNIFICANT) {
            criticalQueue.offer(item);
        } else {
            // Minor event: bounded queue
            if (!minorQueue.offer(item)) {
                droppedMinorCount.incrementAndGet();
            }
        }
    }

    @Override
    public void recordAll(RunId runId, List<SimEvent> events) {
        if (events == null || !isRunning.get()) {
            return;
        }
        for (SimEvent e : events) {
            record(runId, e);
        }
    }

    @Override
    public List<SimEvent> eventsForRun(RunId runId) {
        // Delegate reading to active target
        EventRecorder current = fallbackActive.get() ? fallbackTarget : primaryTarget;
        return current.eventsForRun(runId);
    }

    private final Object writeLock = new Object();

    @Override
    public void flush() {
        drainAllPending();
    }

    private void drainLoop() {
        while (isRunning.get() || !criticalQueue.isEmpty() || !minorQueue.isEmpty()) {
            try {
                int written = drainBatch();
                if (written == 0 && isRunning.get()) {
                    Thread.sleep(flushIntervalMillis);
                }
            } catch (InterruptedException e) {
                // Check if closing
                if (!isRunning.get()) {
                    break;
                }
            }
        }
        // Final drain of critical events on shutdown
        drainAllPending();
    }

    private int drainBatch() {
        synchronized (writeLock) {
            List<QueuedItem> batch = new ArrayList<>(batchSize);

            // Always drain critical queue first
            QueuedItem item;
            while (batch.size() < batchSize && (item = criticalQueue.poll()) != null) {
                batch.add(item);
            }

            // Fill remainder with minor queue
            while (batch.size() < batchSize && (item = minorQueue.poll()) != null) {
                batch.add(item);
            }

            if (batch.isEmpty()) {
                return 0;
            }

            persistBatch(batch);
            return batch.size();
        }
    }

    private void drainAllPending() {
        synchronized (writeLock) {
            List<QueuedItem> batch = new ArrayList<>();
            QueuedItem item;
            while ((item = criticalQueue.poll()) != null) {
                batch.add(item);
            }
            while ((item = minorQueue.poll()) != null) {
                batch.add(item);
            }
            if (!batch.isEmpty()) {
                persistBatch(batch);
            }
        }
    }

    private void persistBatch(List<QueuedItem> batch) {
        EventRecorder target = fallbackActive.get() ? fallbackTarget : primaryTarget;
        try {
            for (QueuedItem item : batch) {
                target.record(item.runId(), item.event());
                totalRecordedCount.incrementAndGet();
            }
            target.flush();
        } catch (Throwable t) {
            // Target write failed - activate fallback if not already active
            if (!fallbackActive.get()) {
                fallbackActive.set(true);
            }
            // Retry batch in fallback target
            for (QueuedItem item : batch) {
                fallbackTarget.record(item.runId(), item.event());
                totalRecordedCount.incrementAndGet();
            }
            fallbackTarget.flush();
        }
    }

    public long droppedMinorCount() {
        return droppedMinorCount.get();
    }

    public long totalRecordedCount() {
        return totalRecordedCount.get();
    }

    public boolean isFallbackActive() {
        return fallbackActive.get();
    }

    public int queueDepth() {
        return criticalQueue.size() + minorQueue.size();
    }

    public Thread writerThread() {
        return writerThread;
    }

    @Override
    public void close() throws InterruptedException {
        isRunning.set(false);
        writerThread.interrupt();
        writerThread.join(5000);
        drainAllPending();
    }
}
