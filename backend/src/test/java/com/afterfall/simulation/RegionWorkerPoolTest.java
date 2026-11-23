package com.afterfall.simulation;

import com.afterfall.action.Action;
import com.afterfall.action.ActionContext;
import com.afterfall.action.ActionResult;
import com.afterfall.action.ActionType;
import com.afterfall.domain.*;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class RegionWorkerPoolTest {

    @Test
    void testPoolInitializesExactlyFourWorkerThreadsWithExpectedNames() throws Exception {
        try (RegionWorkerPool pool = new RegionWorkerPool()) {
            assertEquals(4, pool.poolSize());

            Settlement settlement = new Settlement("Test");
            int id = 1;
            for (RegionId r : RegionId.ALL) {
                settlement.registerSurvivor(new Survivor(
                        new SurvivorId(id),
                        "Survivor-" + id,
                        Personality.neutral(),
                        new Profession(ProfessionType.FARMER, 50),
                        new Needs(50, 50, 50, 50),
                        new Health(80, HealthCondition.HEALTHY),
                        new Inventory(20)
                ), r);
                id++;
            }
            World world = new World(settlement);
            WorldView view = WorldView.snapshot(world, 1L, 0L);

            Set<String> threadNames = ConcurrentHashMap.newKeySet();
            List<RegionTask> tasks = new ArrayList<>();
            for (RegionId r : RegionId.ALL) {
                tasks.add(new RegionTask(r, view, 1L, new SplittableRandom(1L), ctx -> {
                    threadNames.add(Thread.currentThread().getName());
                    return new Action() {
                        @Override
                        public ActionResult evaluate(ActionContext actCtx) {
                            return ActionResult.builder(ActionType.REST).build();
                        }
                        @Override
                        public ActionType type() {
                            return ActionType.REST;
                        }
                    };
                }));
            }

            pool.invokeAll(tasks);

            assertEquals(4, threadNames.size());
            for (int r = 1; r <= 4; r++) {
                assertTrue(threadNames.contains("afterfall-region-" + r),
                        "Missing thread name: afterfall-region-" + r + ", got: " + threadNames);
            }
        }
    }

    @Test
    void testGenuineConcurrentExecutionAcrossFourRegions() throws Exception {
        try (RegionWorkerPool pool = new RegionWorkerPool()) {
            CyclicBarrier barrier = new CyclicBarrier(4);
            Settlement settlement = new Settlement("Test");
            int id = 1;
            for (RegionId r : RegionId.ALL) {
                settlement.registerSurvivor(new Survivor(
                        new SurvivorId(id),
                        "Survivor-" + id,
                        Personality.neutral(),
                        new Profession(ProfessionType.FARMER, 50),
                        new Needs(50, 50, 50, 50),
                        new Health(80, HealthCondition.HEALTHY),
                        new Inventory(20)
                ), r);
                id++;
            }
            World world = new World(settlement);
            WorldView view = WorldView.snapshot(world, 1L, 0L);

            List<RegionTask> tasks = new ArrayList<>();
            for (RegionId r : RegionId.ALL) {
                tasks.add(new RegionTask(r, view, 1L, new SplittableRandom(1L), ctx -> {
                    try {
                        barrier.await(2, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return new Action() {
                        @Override
                        public ActionResult evaluate(ActionContext actCtx) {
                            return ActionResult.builder(ActionType.REST).build();
                        }
                        @Override
                        public ActionType type() {
                            return ActionType.REST;
                        }
                    };
                }));
            }

            List<Future<RegionResult>> futures = pool.invokeAll(tasks);
            assertEquals(4, futures.size());
            for (Future<RegionResult> f : futures) {
                assertNotNull(f.get());
            }
        }
    }

    @Test
    void testPoolShutdownAndIdempotency() {
        RegionWorkerPool pool = new RegionWorkerPool();
        assertFalse(pool.isShutdown());

        pool.shutdown();
        assertTrue(pool.isShutdown());

        // Idempotent close
        assertDoesNotThrow(pool::close);
        assertTrue(pool.isShutdown());
    }

    @Test
    void testCannotSubmitAfterShutdown() {
        RegionWorkerPool pool = new RegionWorkerPool();
        pool.shutdown();

        World world = new World(new Settlement("Test"));
        WorldView view = WorldView.snapshot(world, 1L, 0L);
        List<RegionTask> tasks = new ArrayList<>();
        for (RegionId r : RegionId.ALL) {
            tasks.add(new RegionTask(r, view, 1L, new SplittableRandom(1L), ctx -> null));
        }

        assertThrows(RejectedExecutionException.class, () -> pool.invokeAll(tasks));
    }

    @Test
    void testRejectsTaskCountNotEqualToFour() {
        try (RegionWorkerPool pool = new RegionWorkerPool()) {
            World world = new World(new Settlement("Test"));
            WorldView view = WorldView.snapshot(world, 1L, 0L);

            List<RegionTask> tasks = List.of(
                    new RegionTask(RegionId.REGION_1, view, 1L, new SplittableRandom(1L), ctx -> null)
            );

            assertThrows(IllegalArgumentException.class, () -> pool.invokeAll(tasks));
        }
    }
}
