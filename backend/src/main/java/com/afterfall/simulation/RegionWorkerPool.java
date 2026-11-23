package com.afterfall.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dedicated thread pool owning exactly four worker threads for parallel regional evaluation.
 *
 * <p>Invariants:
 * <ul>
 *   <li>Exactly 4 threads, named {@code afterfall-region-1} through {@code afterfall-region-4}.</li>
 *   <li>Fixed-size ExecutorService; no common pool or ad-hoc threads.</li>
 *   <li>Clean shutdown and cancellation support.</li>
 * </ul>
 */
public final class RegionWorkerPool implements AutoCloseable {

    public static final int WORKER_COUNT = 4;
    private final ExecutorService executor;

    public RegionWorkerPool() {
        AtomicInteger threadIndex = new AtomicInteger(1);
        ThreadFactory factory = runnable -> {
            int idx = threadIndex.getAndIncrement();
            Thread t = new Thread(runnable, "afterfall-region-" + idx);
            t.setDaemon(true);
            return t;
        };
        this.executor = Executors.newFixedThreadPool(WORKER_COUNT, factory);
    }

    public int poolSize() {
        return WORKER_COUNT;
    }

    /**
     * Executes exactly four region tasks concurrently.
     *
     * @param tasks the 4 region tasks
     * @return the list of completed Future objects
     * @throws InterruptedException if interrupted while waiting
     */
    public List<Future<RegionResult>> invokeAll(List<RegionTask> tasks) throws InterruptedException {
        Objects.requireNonNull(tasks, "tasks");
        if (tasks.size() != WORKER_COUNT) {
            throw new IllegalArgumentException("Expected exactly " + WORKER_COUNT + " tasks, but got " + tasks.size());
        }
        return executor.invokeAll(tasks);
    }

    /**
     * Executes tasks with a timeout bound.
     */
    public List<Future<RegionResult>> invokeAll(List<RegionTask> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(tasks, "tasks");
        return executor.invokeAll(tasks, timeout, unit);
    }

    public boolean isShutdown() {
        return executor.isShutdown();
    }

    public boolean isTerminated() {
        return executor.isTerminated();
    }

    /**
     * Initiates a graceful shutdown of the worker pool.
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * Forces immediate shutdown and cancels running tasks.
     */
    public List<Runnable> shutdownNow() {
        return executor.shutdownNow();
    }

    /**
     * Awaits termination up to the given timeout.
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    @Override
    public void close() {
        shutdown();
        try {
            if (!awaitTermination(2, TimeUnit.SECONDS)) {
                shutdownNow();
            }
        } catch (InterruptedException e) {
            shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
