package com.afterfall.simulation;

import com.afterfall.action.Action;
import com.afterfall.action.ActionContext;
import com.afterfall.action.ActionResult;
import com.afterfall.action.ActionType;
import com.afterfall.behavior.BehaviorContext;
import com.afterfall.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationEngineTest {

    private World world;
    private Settlement settlement;
    private WorldClock clock;

    @BeforeEach
    void setUp() {
        world = InitialPopulation.createDefaultWorld("Haven");
        settlement = world.settlement();
        clock = new WorldClock(4);
    }

    @Test
    void testInitialLifecycleStateIsPaused() {
        SimulationEngine engine = new SimulationEngine(world, clock);
        assertEquals(SimulationLifecycle.PAUSED, engine.getStatus());
        assertNotNull(engine.getLatestSnapshot(), "Baseline snapshot must be present at tick 0");
        assertEquals(0L, engine.getLatestSnapshot().tick());
        assertEquals(0L, engine.getLatestSnapshot().version());
        assertEquals(25, engine.getLatestSnapshot().survivors().size());
    }

    @Test
    void testTickRejectionWhilePaused() {
        SimulationEngine engine = new SimulationEngine(world, clock);
        assertThrows(IllegalStateException.class, engine::tick, "Tick must be rejected when PAUSED");
    }

    @Test
    void testStartAndRunningTickExecution() {
        SimulationEngine engine = new SimulationEngine(world, clock);
        engine.start();
        assertEquals(SimulationLifecycle.RUNNING, engine.getStatus());

        TickResult result = engine.tick();
        assertTrue(result.isSuccess());
        assertEquals(1L, result.tickNumber());
        assertEquals(1L, result.worldVersion());
        assertEquals(1L, clock.tick());
        assertEquals(1L, engine.getLatestSnapshot().tick());
        assertEquals(1L, engine.getLatestSnapshot().version());
    }

    @Test
    void testPauseRequestedToPausedAtTickBoundary() {
        SimulationEngine engine = new SimulationEngine(world, clock);
        engine.start();

        engine.requestPause();
        assertEquals(SimulationLifecycle.PAUSE_REQUESTED, engine.getStatus());

        TickResult result = engine.tick();
        assertTrue(result.isSuccess());
        assertEquals(SimulationLifecycle.PAUSED, engine.getStatus(), "Engine should be PAUSED after tick completion");

        assertThrows(IllegalStateException.class, engine::tick);
    }

    @Test
    void testResumeAfterPause() {
        SimulationEngine engine = new SimulationEngine(world, clock);
        engine.start();
        engine.pause();
        assertEquals(SimulationLifecycle.PAUSED, engine.getStatus());

        engine.resume();
        assertEquals(SimulationLifecycle.RUNNING, engine.getStatus());

        TickResult result = engine.tick();
        assertTrue(result.isSuccess());
        assertEquals(1L, result.tickNumber());
        assertEquals(1L, result.worldVersion());
    }

    @Test
    void testStopAndStoppedCannotResumeOrTick() {
        SimulationEngine engine = new SimulationEngine(world, clock);
        engine.start();
        engine.stop();

        assertEquals(SimulationLifecycle.STOPPED, engine.getStatus());
        assertThrows(IllegalStateException.class, engine::start);
        assertThrows(IllegalStateException.class, engine::resume);
        assertThrows(IllegalStateException.class, engine::tick);
        assertThrows(IllegalStateException.class, engine::pause);
        assertThrows(IllegalStateException.class, engine::requestPause);
    }

    @Test
    void testRepeatedPauseAndStopIdempotence() {
        SimulationEngine engine = new SimulationEngine(world, clock);
        assertDoesNotThrow(engine::pause);
        assertEquals(SimulationLifecycle.PAUSED, engine.getStatus());

        engine.start();
        assertDoesNotThrow(engine::start);
        assertEquals(SimulationLifecycle.RUNNING, engine.getStatus());

        engine.stop();
        assertDoesNotThrow(engine::stop);
        assertEquals(SimulationLifecycle.STOPPED, engine.getStatus());
    }

    @Test
    void testFailedTickPreservesLastSuccessfulSnapshotAndClock() {
        Function<BehaviorContext, Action> failingDecider = ctx -> new Action() {
            @Override
            public ActionResult evaluate(ActionContext context) {
                if (context.survivor().id().equals(new SurvivorId(1))) {
                    return ActionResult.builder(ActionType.EAT)
                            .needDelta(Need.FOOD, -10)
                            .inventoryDelta(ItemType.FOOD, -50) // Impossible removal
                            .build();
                }
                return ActionResult.builder(ActionType.WORK).workContribution(1).build();
            }

            @Override
            public ActionType type() {
                return ActionType.EAT;
            }
        };

        TickCoordinator failingCoordinator = new TickCoordinator(world, clock, failingDecider);
        SimulationEngine engine = new SimulationEngine(world, clock, failingCoordinator);
        engine.start();

        WorldView baselineSnapshot = engine.getLatestSnapshot();
        assertEquals(0L, baselineSnapshot.tick());
        assertEquals(0L, baselineSnapshot.version());

        TickResult failedResult = engine.tick();
        assertFalse(failedResult.isSuccess());
        assertEquals(0L, clock.tick(), "Clock should not advance");
        assertEquals(0L, failingCoordinator.worldVersion(), "World version should not advance");
        assertSame(baselineSnapshot, engine.getLatestSnapshot(), "Snapshot should not be updated on failed tick");
    }

    @Test
    void testFiveTickIntegrationRunDefaultPopulation() {
        SimulationEngine engine = new SimulationEngine(world, clock);
        engine.start();

        for (int i = 1; i <= 5; i++) {
            TickResult result = engine.tick();
            assertTrue(result.isSuccess(), "Tick " + i + " must succeed");
            assertEquals(i, result.tickNumber());
            assertEquals(i, result.worldVersion());
            assertEquals(i, clock.tick());
            assertEquals(25, result.survivorsProcessed());
            assertEquals(4, result.regionResults().size());
            assertEquals(i, engine.getLatestSnapshot().tick());
            assertEquals(i, engine.getLatestSnapshot().version());
        }
    }

    @Test
    void testDeterminismIdenticalRunsProduceIdenticalSnapshots() {
        World worldA = InitialPopulation.createDefaultWorld("Haven");
        WorldClock clockA = new WorldClock(4);
        SimulationEngine engineA = new SimulationEngine(worldA, clockA);
        engineA.start();

        World worldB = InitialPopulation.createDefaultWorld("Haven");
        WorldClock clockB = new WorldClock(4);
        SimulationEngine engineB = new SimulationEngine(worldB, clockB);
        engineB.start();

        for (int i = 0; i < 5; i++) {
            TickResult resA = engineA.tick();
            TickResult resB = engineB.tick();

            assertEquals(resA.tickNumber(), resB.tickNumber());
            assertEquals(resA.worldVersion(), resB.worldVersion());
            assertEquals(resA.survivorsProcessed(), resB.survivorsProcessed());
            assertEquals(resA.actionsAccepted(), resB.actionsAccepted());
            assertEquals(resA.actionsRejected(), resB.actionsRejected());
            assertEquals(resA.events().size(), resB.events().size());
        }

        WorldView snapA = engineA.getLatestSnapshot();
        WorldView snapB = engineB.getLatestSnapshot();

        assertEquals(snapA.tick(), snapB.tick());
        assertEquals(snapA.version(), snapB.version());

        for (SurvivorId id : snapA.survivors().keySet()) {
            SurvivorView svA = snapA.survivors().get(id);
            SurvivorView svB = snapB.survivors().get(id);
            assertEquals(svA.vitality(), svB.vitality());
            assertEquals(svA.condition(), svB.condition());
            for (Need need : Need.values()) {
                assertEquals(svA.needLevel(need), svB.needLevel(need));
            }
            for (ItemType item : ItemType.values()) {
                assertEquals(svA.itemQuantity(item), svB.itemQuantity(item));
            }
        }
    }
}
