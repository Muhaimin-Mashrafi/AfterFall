package com.afterfall.simulation;

import com.afterfall.action.Action;
import com.afterfall.action.ActionContext;
import com.afterfall.action.ActionResult;
import com.afterfall.action.ActionType;
import com.afterfall.action.ProposedEvent;
import com.afterfall.action.ResourceClaim;
import com.afterfall.behavior.BehaviorContext;
import com.afterfall.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class TickCoordinatorFailureTest {

    private World world;
    private Settlement settlement;
    private WorldClock clock;
    private Survivor s1;
    private Survivor s2;
    private Survivor s3;

    @BeforeEach
    void setUp() {
        settlement = new Settlement("Hope Settlement");
        world = new World(settlement);
        clock = new WorldClock(4);

        s1 = createSurvivor(1, "Survivor-1", 50, 50, 50, 50, 80);
        s2 = createSurvivor(2, "Survivor-2", 50, 50, 50, 50, 80);
        s3 = createSurvivor(3, "Survivor-3", 50, 50, 50, 50, 80);
        Survivor s4 = createSurvivor(4, "Survivor-4", 50, 50, 50, 50, 80);

        settlement.registerSurvivor(s1, RegionId.REGION_1);
        settlement.registerSurvivor(s2, RegionId.REGION_2);
        settlement.registerSurvivor(s3, RegionId.REGION_3);
        settlement.registerSurvivor(s4, RegionId.REGION_4);
    }

    private Survivor createSurvivor(int idVal, String name, int food, int water, int rest, int safety, int vitality) {
        Needs needs = new Needs(food, water, rest, safety);
        Health health = new Health(vitality, HealthCondition.HEALTHY);
        Inventory inventory = new Inventory(20);
        inventory.add(ItemType.FOOD, 2);
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
    void testInjectedBatchFailureRollsBackAllSurvivorsAndDoesNotAdvanceClockOrVersion() {
        Function<BehaviorContext, Action> failingDecider = context -> new Action() {
            @Override
            public ActionResult evaluate(ActionContext ctx) {
                if (ctx.survivor().id().equals(new SurvivorId(3))) {
                    return ActionResult.builder(ActionType.EAT)
                            .needDelta(Need.FOOD, -10)
                            .inventoryDelta(ItemType.FOOD, -10) // Invalid removal
                            .addResourceClaim(new ResourceClaim(ResourceType.FOOD, 2))
                            .migrationRequest(RegionId.REGION_4)
                            .addEvent(new ProposedEvent("FAIL_EVENT", ctx.survivor().id(), "Failing", 2))
                            .build();
                } else {
                    return ActionResult.builder(ActionType.EAT)
                            .needDelta(Need.FOOD, -10)
                            .vitalityDelta(5)
                            .inventoryDelta(ItemType.FOOD, -1)
                            .build();
                }
            }

            @Override
            public ActionType type() {
                return ActionType.EAT;
            }
        };

        long initialTick = clock.tick();
        assertEquals(0L, initialTick);

        TickCoordinator coordinator = new TickCoordinator(world, clock, failingDecider);
        assertEquals(0L, coordinator.worldVersion());

        TickResult result = coordinator.executeTick();

        // 1. Result must report failure
        assertFalse(result.isSuccess(), "Result should be marked failure");
        assertNotNull(result.failureReason());

        // 2. Clock and world version must NOT advance
        assertEquals(0L, clock.tick(), "Clock should not advance on failure");
        assertEquals(0L, coordinator.worldVersion(), "World version must not increment on failure");
        assertEquals(0L, result.tickNumber());
        assertEquals(0L, result.worldVersion());

        // 3. Claims, migration requests, events, and results from evaluated actions must be preserved
        assertFalse(result.resourceClaims().isEmpty(), "Resource claims should still be recorded");
        assertFalse(result.migrationRequests().isEmpty(), "Migration requests should still be recorded");
        assertFalse(result.events().isEmpty(), "Events should still be recorded");

        // 4. State must be completely unchanged/restored for all survivors
        assertEquals(50, s1.needs().level(Need.FOOD));
        assertEquals(80, s1.health().vitality());
        assertEquals(2, s1.inventory().quantity(ItemType.FOOD));

        assertEquals(50, s2.needs().level(Need.FOOD));
        assertEquals(80, s2.health().vitality());
        assertEquals(2, s2.inventory().quantity(ItemType.FOOD));

        assertEquals(50, s3.needs().level(Need.FOOD));
        assertEquals(80, s3.health().vitality());
        assertEquals(2, s3.inventory().quantity(ItemType.FOOD));
    }

    @Test
    void testCannotRunConcurrentTickViaReentrancyGuard() {
        // Create an action decider that attempts to re-enter executeTick during evaluation
        final TickCoordinator[] holder = new TickCoordinator[1];
        Function<BehaviorContext, Action> reentrantDecider = context -> new Action() {
            @Override
            public ActionResult evaluate(ActionContext ctx) {
                // Attempt overlapping call on the same coordinator instance
                assertThrows(IllegalStateException.class, () -> holder[0].executeTick(),
                        "Overlapping tick execution must throw IllegalStateException");
                return ActionResult.builder(ActionType.WORK).workContribution(1).build();
            }

            @Override
            public ActionType type() {
                return ActionType.WORK;
            }
        };

        TickCoordinator coordinator = new TickCoordinator(world, clock, reentrantDecider);
        holder[0] = coordinator;

        TickResult res = coordinator.executeTick();
        assertTrue(res.isSuccess());

        // Guard must have safely reset on exit, allowing subsequent tick
        assertDoesNotThrow(coordinator::executeTick);
    }


}
