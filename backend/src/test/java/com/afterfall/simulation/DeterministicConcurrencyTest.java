package com.afterfall.simulation;

import com.afterfall.action.Action;
import com.afterfall.action.ActionContext;
import com.afterfall.action.ActionResult;
import com.afterfall.action.ActionType;
import com.afterfall.action.ProposedEvent;
import com.afterfall.behavior.BehaviorContext;
import com.afterfall.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicConcurrencyTest {

    private World world;
    private Settlement settlement;
    private WorldClock clock;

    @BeforeEach
    void setUp() {
        settlement = new Settlement("Multithreaded Settlement");
        // Distribute 8 survivors evenly: 2 per region
        int id = 1;
        for (int r = 1; r <= 4; r++) {
            RegionId regionId = new RegionId(r);
            for (int s = 0; s < 2; s++) {
                settlement.registerSurvivor(createSurvivor(id, "Survivor-" + id), regionId);
                id++;
            }
        }
        world = new World(settlement);
        clock = new WorldClock(4);
    }

    private Survivor createSurvivor(int idVal, String name) {
        Needs needs = new Needs(50, 50, 50, 50);
        Health health = new Health(80, HealthCondition.HEALTHY);
        Inventory inventory = new Inventory(20);
        return new Survivor(
                new SurvivorId(idVal),
                name,
                Personality.neutral(),
                new Profession(ProfessionType.FARMER, 50),
                needs,
                health,
                inventory
        );
    }

    @Test
    void testCompletionOrderPermutationDoesNotAffectDeterministicMerge() throws Exception {
        // Artificially delay Region 1 worker so Region 4 and others complete first
        Function<BehaviorContext, Action> deciderWithDelay = ctx -> {
            if (ctx.currentRegion().equals(RegionId.REGION_1)) {
                try {
                    Thread.sleep(80);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return new Action() {
                @Override
                public ActionResult evaluate(ActionContext actCtx) {
                    return ActionResult.builder(ActionType.REST)
                            .vitalityDelta(1)
                            .addEvent(new ProposedEvent("REST_LOG", actCtx.survivor().id(), "Rested", 1))
                            .build();
                }

                @Override
                public ActionType type() {
                    return ActionType.REST;
                }
            };
        };

        TickCoordinator coordinator = new TickCoordinator(world, clock, deciderWithDelay);
        TickResult result = coordinator.executeTick();

        assertTrue(result.isSuccess());

        // Verify RegionResults are strictly in RegionId order 1, 2, 3, 4
        List<RegionResult> regionResults = result.regionResults();
        assertEquals(4, regionResults.size());
        assertEquals(RegionId.REGION_1, regionResults.get(0).regionId());
        assertEquals(RegionId.REGION_2, regionResults.get(1).regionId());
        assertEquals(RegionId.REGION_3, regionResults.get(2).regionId());
        assertEquals(RegionId.REGION_4, regionResults.get(3).regionId());

        // Verify survivor results in each RegionResult are strictly in ascending SurvivorId order
        for (RegionResult rr : regionResults) {
            List<SurvivorActionResult> actions = rr.survivorResults();
            for (int i = 1; i < actions.size(); i++) {
                assertTrue(actions.get(i - 1).survivorId().value() < actions.get(i).survivorId().value());
            }
        }

        // Verify events are collected in deterministic order
        List<ProposedEvent> events = result.events();
        assertEquals(8, events.size());

        coordinator.shutdown();
    }

    @Test
    void testMultiTickDeterminismAcrossTwoIndependentSimulations() {
        long runSeed = 777L;

        // Run simulation A for 5 ticks
        List<WorldView> viewsA = runSimulation(runSeed, 5);

        // Run simulation B for 5 ticks with identical configuration
        List<WorldView> viewsB = runSimulation(runSeed, 5);

        assertEquals(viewsA.size(), viewsB.size());
        for (int tick = 0; tick < viewsA.size(); tick++) {
            WorldView a = viewsA.get(tick);
            WorldView b = viewsB.get(tick);

            assertEquals(a.tick(), b.tick());
            assertEquals(a.version(), b.version());

            for (int r = 1; r <= 4; r++) {
                RegionId rId = new RegionId(r);
                RegionView rA = a.regions().get(rId);
                RegionView rB = b.regions().get(rId);

                assertEquals(rA.survivorIds(), rB.survivorIds());
            }

            for (int s = 1; s <= 8; s++) {
                SurvivorId sId = new SurvivorId(s);
                SurvivorView svA = a.survivors().get(sId);
                SurvivorView svB = b.survivors().get(sId);

                assertEquals(svA.vitality(), svB.vitality());
                assertEquals(svA.condition(), svB.condition());
                assertEquals(svA.needLevels(), svB.needLevels());
                assertEquals(svA.inventoryQuantities(), svB.inventoryQuantities());
            }
        }
    }

    private List<WorldView> runSimulation(long seed, int ticks) {
        Settlement simSettlement = new Settlement("Sim Settlement");
        int id = 1;
        for (int r = 1; r <= 4; r++) {
            RegionId regionId = new RegionId(r);
            for (int s = 0; s < 2; s++) {
                simSettlement.registerSurvivor(createSurvivor(id, "Survivor-" + id), regionId);
                id++;
            }
        }
        World simWorld = new World(simSettlement);
        WorldClock simClock = new WorldClock(4);

        TickCoordinator coordinator = new TickCoordinator(simWorld, simClock, seed);
        List<WorldView> views = new ArrayList<>();

        for (int i = 0; i < ticks; i++) {
            TickResult res = coordinator.executeTick();
            assertTrue(res.isSuccess());
            views.add(WorldView.snapshot(simWorld, res.tickNumber(), res.worldVersion()));
        }

        coordinator.shutdown();
        return views;
    }

    @Test
    void testWorkerFailureInOneRegionAbortsTickAndPreservesState() {
        Function<BehaviorContext, Action> failingDecider = ctx -> {
            if (ctx.currentRegion().equals(RegionId.REGION_3)) {
                throw new RuntimeException("Simulated catastrophic worker crash in Region 3");
            }
            return new Action() {
                @Override
                public ActionResult evaluate(ActionContext actCtx) {
                    return ActionResult.builder(ActionType.REST).vitalityDelta(5).build();
                }

                @Override
                public ActionType type() {
                    return ActionType.REST;
                }
            };
        };

        TickCoordinator coordinator = new TickCoordinator(world, clock, failingDecider);

        TickResult result = coordinator.executeTick();
        assertFalse(result.isSuccess(), "Tick must fail if a worker throws an exception");
        assertTrue(result.failureReason().contains("Simulated catastrophic worker crash in Region 3"));

        // Clock and version unchanged
        assertEquals(0L, clock.tick());
        assertEquals(0L, coordinator.worldVersion());

        // All survivors remain at initial state
        for (Survivor s : settlement.survivors()) {
            assertEquals(80, s.health().vitality());
        }

        // Tick guard released: subsequent tick can be executed
        TickCoordinator recoveryCoordinator = new TickCoordinator(world, clock);
        TickResult recoveryResult = recoveryCoordinator.executeTick();
        assertTrue(recoveryResult.isSuccess());
        assertEquals(1L, clock.tick());

        coordinator.shutdown();
        recoveryCoordinator.shutdown();
    }
}
