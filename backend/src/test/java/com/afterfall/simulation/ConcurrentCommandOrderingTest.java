package com.afterfall.simulation;

import com.afterfall.domain.Need;
import com.afterfall.domain.Survivor;
import com.afterfall.domain.SurvivorId;
import com.afterfall.domain.InitialPopulation;
import com.afterfall.domain.World;
import com.afterfall.observer.Intervention;
import com.afterfall.observer.InterventionRecord;
import com.afterfall.observer.InterventionStatus;
import com.afterfall.observer.InterventionType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concurrent command ordering (CLAUDE.md section 12): interventions submitted from many threads in
 * arbitrary order are applied at the next tick boundary in a defined order (by intervention id), so
 * the outcome never depends on thread scheduling.
 */
class ConcurrentCommandOrderingTest {

    private static final int COMMANDS = 40;

    private static Intervention command(long id, SurvivorId target) {
        return Intervention.builder(id, 0, InterventionType.RESOURCE_PRESSURE)
                .targetSurvivor(target)
                .reason("ordering test")
                .intendedEffect("ordering test")
                .needDelta(Need.values()[0], (int) (id % 5) - 2)
                .build();
    }

    private record Outcome(List<Long> appliedOrder, List<Integer> needLevels) {}

    private static Outcome runWith(List<Intervention> submissionOrder, boolean concurrent) throws Exception {
        World world = InitialPopulation.createDefaultWorld("Ordering");
        WorldClock clock = new WorldClock(4);
        TickCoordinator coordinator = new TickCoordinator(world, clock, 42L);
        try {
            if (concurrent) {
                ExecutorService pool = Executors.newFixedThreadPool(8);
                CountDownLatch start = new CountDownLatch(1);
                for (Intervention i : submissionOrder) {
                    pool.submit(() -> {
                        start.await();
                        coordinator.queueIntervention(i);
                        return null;
                    });
                }
                start.countDown();
                pool.shutdown();
                assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
            } else {
                submissionOrder.forEach(coordinator::queueIntervention);
            }
            coordinator.executeTick();
            List<Long> order = coordinator.observerAI().records().stream()
                    .filter(r -> r.status() == InterventionStatus.APPLIED)
                    .map(InterventionRecord::interventionId)
                    .toList();
            List<Integer> levels = new ArrayList<>();
            for (Survivor s : world.settlement().survivors()) {
                levels.add(s.needs().level(Need.values()[0]));
            }
            return new Outcome(order, levels);
        } finally {
            coordinator.close();
        }
    }

    @Test
    void interventionsFromManyThreadsApplyInIdOrderAtTheTickBoundary() throws Exception {
        SurvivorId target = InitialPopulation.createDefaultWorld("probe").settlement().survivors()
                .iterator().next().id();
        List<Intervention> inOrder = new ArrayList<>();
        for (long id = 1; id <= COMMANDS; id++) {
            inOrder.add(command(id, target));
        }
        List<Intervention> shuffled = new ArrayList<>(inOrder);
        Collections.shuffle(shuffled, new Random(7));

        Outcome baseline = runWith(inOrder, false);
        assertEquals(COMMANDS, baseline.appliedOrder().size());

        for (int round = 0; round < 5; round++) {
            Collections.shuffle(shuffled, new Random(round));
            Outcome concurrent = runWith(shuffled, true);
            assertEquals(baseline.appliedOrder(), concurrent.appliedOrder(),
                    "application order must be by intervention id, not arrival order");
            assertEquals(baseline.needLevels(), concurrent.needLevels(),
                    "final state must not depend on submission interleaving");
        }
    }
}
