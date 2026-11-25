package com.afterfall.simulation;

import com.afterfall.action.Action;
import com.afterfall.action.ActionContext;
import com.afterfall.action.ActionResult;
import com.afterfall.action.ActionType;
import com.afterfall.action.ProposedEvent;
import com.afterfall.behavior.BehaviorContext;
import com.afterfall.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M12 Verification Test: Worker scheduling jitter determinism (Blueprint 17.2).
 *
 * <p>Verifies that introducing synthetic 0–50 ms scheduling delays and thread jitter
 * across worker threads has ZERO impact on:
 * <ul>
 *   <li>The ordered event sequence numbers and event log</li>
 *   <li>Survivor states, needs, health, and locations</li>
 *   <li>Total settlement resource quantities</li>
 *   <li>Deterministic final state hash across repeated runs with identical seed</li>
 * </ul>
 */
class WorkerJitterDeterminismTest {

    @Test
    @DisplayName("Identical seed produces identical state hash and event logs despite 0-50ms worker jitter")
    void testDeterminismUnderWorkerSchedulingJitter() {
        long seed = 42L;
        int ticksToRun = 10;

        // Run 1: Standard baseline execution without intentional jitter
        SimulationOutcome run1 = runSimulation(seed, ticksToRun, false);

        // Run 2: Jittered execution with randomized 0-50ms delays in worker action evaluations
        SimulationOutcome run2 = runSimulation(seed, ticksToRun, true);

        // Run 3: Second jittered execution with distinct thread timing
        SimulationOutcome run3 = runSimulation(seed, ticksToRun, true);

        // 1. Verify tick counts match
        assertEquals(run1.views.size(), run2.views.size(), "Tick count mismatch between standard and jittered run");
        assertEquals(run1.views.size(), run3.views.size(), "Tick count mismatch between jittered runs");

        // 2. Verify all tick-by-tick state views match exactly
        for (int t = 0; t < run1.views.size(); t++) {
            WorldView v1 = run1.views.get(t);
            WorldView v2 = run2.views.get(t);
            WorldView v3 = run3.views.get(t);

            assertEquals(v1.tick(), v2.tick(), "Tick number mismatch at index " + t);
            assertEquals(v1.version(), v2.version(), "World version mismatch at index " + t);

            // Compare region membership
            for (RegionId r : RegionId.ALL) {
                assertEquals(v1.regions().get(r).survivorIds(), v2.regions().get(r).survivorIds(),
                        "Region " + r + " survivor IDs mismatch at tick " + t + " (run1 vs run2)");
                assertEquals(v1.regions().get(r).survivorIds(), v3.regions().get(r).survivorIds(),
                        "Region " + r + " survivor IDs mismatch at tick " + t + " (run1 vs run3)");
            }

            // Compare individual survivor states
            for (SurvivorId sId : v1.survivors().keySet()) {
                SurvivorView sv1 = v1.survivors().get(sId);
                SurvivorView sv2 = v2.survivors().get(sId);
                SurvivorView sv3 = v3.survivors().get(sId);

                assertEquals(sv1.vitality(), sv2.vitality(), "Survivor " + sId + " vitality mismatch at tick " + t);
                assertEquals(sv1.vitality(), sv3.vitality(), "Survivor " + sId + " vitality mismatch at tick " + t);

                assertEquals(sv1.needLevels(), sv2.needLevels(), "Survivor " + sId + " needs mismatch at tick " + t);
                assertEquals(sv1.needLevels(), sv3.needLevels(), "Survivor " + sId + " needs mismatch at tick " + t);

                assertEquals(sv1.location(), sv2.location(), "Survivor " + sId + " location mismatch at tick " + t);
                assertEquals(sv1.location(), sv3.location(), "Survivor " + sId + " location mismatch at tick " + t);
            }
        }

        // 3. Verify event log count and ordering match exactly
        assertEquals(run1.eventLogs.size(), run2.eventLogs.size(), "Event log size mismatch (run1 vs run2)");
        assertEquals(run1.eventLogs.size(), run3.eventLogs.size(), "Event log size mismatch (run1 vs run3)");

        for (int i = 0; i < run1.eventLogs.size(); i++) {
            assertEquals(run1.eventLogs.get(i), run2.eventLogs.get(i), "Event log item mismatch at " + i + " (run1 vs run2)");
            assertEquals(run1.eventLogs.get(i), run3.eventLogs.get(i), "Event log item mismatch at " + i + " (run1 vs run3)");
        }

        // 4. Verify composite state hash matches
        assertEquals(run1.stateHash, run2.stateHash, "Composite state hash mismatch under jitter");
        assertEquals(run1.stateHash, run3.stateHash, "Composite state hash mismatch across multiple jittered runs");
    }

    private record SimulationOutcome(List<WorldView> views, List<String> eventLogs, int stateHash) {}

    private SimulationOutcome runSimulation(long seed, int ticks, boolean applyJitter) {
        Settlement settlement = new Settlement("Jitter Settlement");
        int id = 1;
        for (RegionId r : RegionId.ALL) {
            for (int s = 0; s < 5; s++) { // 20 survivors total
                Needs needs = new Needs(60, 60, 60, 60);
                Health health = new Health(85, HealthCondition.HEALTHY);
                Inventory inventory = new Inventory(20);
                Survivor survivor = new Survivor(
                        new SurvivorId(id),
                        "Survivor-" + id,
                        Personality.neutral(),
                        new Profession(ProfessionType.FARMER, 50),
                        needs,
                        health,
                        inventory
                );
                settlement.registerSurvivor(survivor, r);
                id++;
            }
        }
        settlement.resourceStock().resource(ResourceType.FOOD).add(100);
        settlement.resourceStock().resource(ResourceType.WATER).add(100);
        settlement.resourceStock().resource(ResourceType.MEDICINE).add(50);

        World world = new World(settlement);
        WorldClock clock = new WorldClock(4);

        Function<BehaviorContext, Action> actionDecider = ctx -> {
            if (applyJitter) {
                // Synthetic jitter: random pause between 0 and 50 milliseconds
                int jitterMs = ThreadLocalRandom.current().nextInt(0, 51);
                if (jitterMs > 0) {
                    try {
                        Thread.sleep(jitterMs);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            // Deterministic action evaluation based on survivor state
            int vit = ctx.survivor().vitality();
            if (vit < 70) {
                return new Action() {
                    @Override public ActionType type() { return ActionType.REST; }
                    @Override public ActionResult evaluate(ActionContext actCtx) {
                        return ActionResult.builder(ActionType.REST).vitalityDelta(2).build();
                    }
                };
            } else if (ctx.foodAvailable()) {
                return new Action() {
                    @Override public ActionType type() { return ActionType.EAT; }
                    @Override public ActionResult evaluate(ActionContext actCtx) {
                        return ActionResult.builder(ActionType.EAT).vitalityDelta(1).build();
                    }
                };
            } else {
                return new Action() {
                    @Override public ActionType type() { return ActionType.WORK; }
                    @Override public ActionResult evaluate(ActionContext actCtx) {
                        return ActionResult.builder(ActionType.WORK).vitalityDelta(0).build();
                    }
                };
            }
        };

        RegionWorkerPool pool = new RegionWorkerPool();
        TickCoordinator coordinator = new TickCoordinator(
                world,
                clock,
                actionDecider,
                pool,
                true,
                new SeededRandomFactory(seed)
        );

        List<WorldView> views = new ArrayList<>();
        List<String> eventLogs = new ArrayList<>();

        try {
            for (int i = 0; i < ticks; i++) {
                TickResult tr = coordinator.executeTick();
                assertTrue(tr.isSuccess());
                views.add(WorldView.snapshot(world, tr.tickNumber(), tr.worldVersion()));
                for (ProposedEvent ev : tr.events()) {
                    eventLogs.add("Tick " + tr.tickNumber() + " | " + ev.eventType() + " | " + ev.summary());
                }
            }
        } finally {
            coordinator.shutdown();
        }

        // Calculate final state hash
        int hash = computeStateHash(settlement, views.get(views.size() - 1));
        return new SimulationOutcome(views, eventLogs, hash);
    }

    private int computeStateHash(Settlement settlement, WorldView finalView) {
        int result = Objects.hash(
                finalView.tick(),
                finalView.version(),
                settlement.resourceStock().resource(ResourceType.FOOD).quantity(),
                settlement.resourceStock().resource(ResourceType.WATER).quantity(),
                settlement.resourceStock().resource(ResourceType.MEDICINE).quantity()
        );

        for (RegionId r : RegionId.ALL) {
            result = 31 * result + finalView.regions().get(r).survivorIds().hashCode();
        }

        for (Map.Entry<SurvivorId, SurvivorView> entry : finalView.survivors().entrySet()) {
            SurvivorView sv = entry.getValue();
            result = 31 * result + Objects.hash(
                    entry.getKey(),
                    sv.vitality(),
                    sv.condition(),
                    sv.needLevels(),
                    sv.location()
            );
        }

        return result;
    }
}
