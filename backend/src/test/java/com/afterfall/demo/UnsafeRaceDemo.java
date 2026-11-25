package com.afterfall.demo;

import com.afterfall.domain.Resource;
import com.afterfall.domain.ResourceType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * M12 / Presentation Standalone Demo: Unsafe Race vs Deterministic Thread-Safe Resource Management.
 *
 * <p>Blueprint Reference: Section 17 &amp; 20.
 *
 * <p>Demonstrates what happens when multiple concurrent worker threads attempt to mutate
 * shared simulation state directly without synchronization or coordination, contrasted with
 * AfterFall's ReentrantLock-protected {@link Resource} and sole-writer {@code TickCoordinator} merge.
 *
 * <p>Usage:
 * Run as a standalone Java application:
 * {@code java -cp ... com.afterfall.demo.UnsafeRaceDemo}
 */
public class UnsafeRaceDemo {

    /**
     * Intentionally unsafe, un-synchronized resource counter illustrating race hazard.
     */
    static final class UnsafeResource {
        private int quantity;

        UnsafeResource(int initial) {
            this.quantity = initial;
        }

        // Intentionally missing synchronized / locks
        void add(int delta) {
            int current = quantity;
            // Artificial delay to widen thread-interleaving race window
            simulateWork();
            quantity = current + delta;
        }

        void consume(int delta) {
            int current = quantity;
            simulateWork();
            quantity = current - delta;
        }

        int quantity() {
            return quantity;
        }

        private void simulateWork() {
            int sum = 0;
            for (int i = 0; i < 50; i++) {
                sum += i;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("================================================================================");
        System.out.println("AfterFall — Concurrency Architecture Demonstration: Unsafe vs Safe Resource Race");
        System.out.println("================================================================================\n");

        int initialStock = 1000;
        int threadCount = 8;
        int operationsPerThread = 500;
        int expectedFinal = initialStock; // Each thread adds 1 and consumes 1 in paired operations

        // -------------------------------------------------------------------------
        // 1. UNPROTECTED RACE: Demonstrates data corruption & lost updates
        // -------------------------------------------------------------------------
        System.out.println("--- 1. Testing Unprotected Shared State (Naive Concurrent Mutation) ---");
        UnsafeResource unsafe = new UnsafeResource(initialStock);
        runUnsafeBenchmark(unsafe, threadCount, operationsPerThread);
        int unsafeFinal = unsafe.quantity();
        int unsafeLostUpdates = Math.abs(expectedFinal - unsafeFinal);

        System.out.printf("Initial Stock: %d\n", initialStock);
        System.out.printf("Total Concurrent Ops: %d threads x %d paired add/consume = %d total mutations\n",
                threadCount, operationsPerThread, threadCount * operationsPerThread * 2);
        System.out.printf("Expected Final Stock: %d\n", expectedFinal);
        System.out.printf("Actual Unsafe Stock:  %d\n", unsafeFinal);
        System.out.printf("Lost Updates (Data Corruption): %d %s\n\n",
                unsafeLostUpdates,
                unsafeLostUpdates > 0 ? "[RACE HAZARD CONFIRMED]" : "[NO CORRUPTION]");

        // -------------------------------------------------------------------------
        // 2. AFTERFALL PRODUCTION ARCHITECTURE: Lock-Protected & Merged State
        // -------------------------------------------------------------------------
        System.out.println("--- 2. Testing AfterFall Thread-Safe Resource (Production Lock Protection) ---");
        Resource safeResource = new Resource(ResourceType.FOOD, initialStock);
        runSafeBenchmark(safeResource, threadCount, operationsPerThread);
        int safeFinal = safeResource.quantity();

        System.out.printf("Initial Stock: %d\n", initialStock);
        System.out.printf("Total Concurrent Ops: %d threads x %d paired add/consume = %d total mutations\n",
                threadCount, operationsPerThread, threadCount * operationsPerThread * 2);
        System.out.printf("Expected Final Stock: %d\n", expectedFinal);
        System.out.printf("Actual Safe Stock:    %d\n", safeFinal);
        System.out.printf("Integrity Status:     %s\n",
                safeFinal == expectedFinal ? "PERFECT [0 LOST UPDATES - 100% INVARIANT PRESERVED]" : "FAILED");

        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("Architectural Takeaway for AOOP / Examination:");
        System.out.println("In AfterFall, RegionWorkers NEVER mutate shared state directly.");
        System.out.println("Workers propose claims in immutable RegionResults; TickCoordinator resolves");
        System.out.println("claims deterministically in Phase 5 as the SOLE WRITER.");
        System.out.println("================================================================================");
    }

    private static void runUnsafeBenchmark(UnsafeResource res, int threads, int ops) throws InterruptedException {
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            exec.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < ops; i++) {
                        res.add(1);
                        res.consume(1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        exec.shutdownNow();
    }

    private static void runSafeBenchmark(Resource res, int threads, int ops) throws InterruptedException {
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            exec.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < ops; i++) {
                        res.add(1);
                        res.consume(1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        exec.shutdownNow();
    }
}
