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

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class TickCoordinatorSuccessTest {

    private World world;
    private Settlement settlement;
    private WorldClock clock;

    @BeforeEach
    void setUp() {
        settlement = new Settlement("Hope Settlement");
        world = new World(settlement);
        clock = new WorldClock(4);

        // Register 4 survivors across all four regions (1 survivor per region)
        int idCounter = 1;
        for (RegionId regionId : RegionId.ALL) {
            Survivor s = createSurvivor(idCounter++, "Survivor-" + regionId.value(), 50, 50, 50, 50, 80);
            settlement.registerSurvivor(s, regionId);
        }
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
    void testAllFourRegionsProcessedInOrder() {
        TickCoordinator coordinator = new TickCoordinator(world, clock);
        TickResult result = coordinator.executeTick();

        assertTrue(result.isSuccess(), "Tick should succeed");
        assertEquals(1L, result.tickNumber());
        assertEquals(1L, result.worldVersion(), "World version must increment to 1");
        assertEquals(1L, coordinator.worldVersion());
        assertEquals(4, result.survivorsProcessed());

        List<RegionResult> regionResults = result.regionResults();
        assertEquals(4, regionResults.size());

        for (int i = 0; i < 4; i++) {
            assertEquals(RegionId.ALL.get(i), regionResults.get(i).regionId());
            assertEquals(1, regionResults.get(i).survivorResults().size());
            assertEquals(new SurvivorId(i + 1), regionResults.get(i).survivorResults().get(0).survivorId());
        }
    }

    @Test
    void testSurvivorsProcessedInAscendingIdOrderWithinRegion() {
        // Register additional survivors in Region 1
        Survivor s5 = createSurvivor(5, "Survivor-5", 50, 50, 50, 50, 80);
        Survivor s6 = createSurvivor(6, "Survivor-6", 50, 50, 50, 50, 80);
        settlement.registerSurvivor(s5, RegionId.REGION_1);
        settlement.registerSurvivor(s6, RegionId.REGION_1);

        TickCoordinator coordinator = new TickCoordinator(world, clock);
        TickResult result = coordinator.executeTick();

        assertTrue(result.isSuccess());
        RegionResult r1 = result.regionResults().get(0);
        assertEquals(RegionId.REGION_1, r1.regionId());
        assertEquals(3, r1.survivorResults().size());
        assertEquals(new SurvivorId(1), r1.survivorResults().get(0).survivorId());
        assertEquals(new SurvivorId(5), r1.survivorResults().get(1).survivorId());
        assertEquals(new SurvivorId(6), r1.survivorResults().get(2).survivorId());
    }

    @Test
    void testMultipleConsecutiveTicksAdvanceClockAndVersion() {
        TickCoordinator coordinator = new TickCoordinator(world, clock);

        assertEquals(0L, clock.tick());
        assertEquals(0L, coordinator.worldVersion());

        TickResult t1 = coordinator.executeTick();
        assertTrue(t1.isSuccess());
        assertEquals(1L, clock.tick());
        assertEquals(1L, t1.tickNumber());
        assertEquals(1L, t1.worldVersion());
        assertEquals(1L, coordinator.worldVersion());

        TickResult t2 = coordinator.executeTick();
        assertTrue(t2.isSuccess());
        assertEquals(2L, clock.tick());
        assertEquals(2L, t2.tickNumber());
        assertEquals(2L, t2.worldVersion());
        assertEquals(2L, coordinator.worldVersion());
    }

    @Test
    void testResourceClaimsAndMigrationPreserved() {
        // Custom decider returning an action with ResourceClaim and MigrationRequest (to destination != current)
        Function<BehaviorContext, Action> customDecider = context -> new Action() {
            @Override
            public ActionResult evaluate(ActionContext ctx) {
                // If in Region 1, move to Region 2; if in Region 2, move to Region 3; etc.
                int nextVal = (ctx.currentRegion().value() % 4) + 1;
                RegionId target = new RegionId(nextVal);
                return ActionResult.builder(ActionType.WORK)
                        .workContribution(5)
                        .addResourceClaim(new ResourceClaim(ResourceType.FOOD, 1))
                        .migrationRequest(target)
                        .addEvent(new ProposedEvent("CLAIM_TEST", ctx.survivor().id(), "Claimed food", 1))
                        .build();
            }

            @Override
            public ActionType type() {
                return ActionType.WORK;
            }
        };

        TickCoordinator coordinator = new TickCoordinator(world, clock, customDecider);
        TickResult result = coordinator.executeTick();

        assertTrue(result.isSuccess(), "Tick failure reason: " + result.failureReason());
        assertFalse(result.resourceClaims().isEmpty(), "Resource claims should be preserved");
        assertEquals(4, result.resourceClaims().size());
        assertEquals(ResourceType.FOOD, result.resourceClaims().get(0).resourceType());

        assertFalse(result.migrationRequests().isEmpty(), "Migration requests should be preserved");
        assertEquals(4, result.migrationRequests().size());

        // Verify MigrationRequest structured fields
        MigrationRequest mr0 = result.migrationRequests().get(0);
        assertEquals(new SurvivorId(1), mr0.survivorId());
        assertEquals(RegionId.REGION_1, mr0.sourceRegion());
        assertEquals(RegionId.REGION_2, mr0.destinationRegion());
        assertEquals(1, mr0.actionSequence());

        assertFalse(result.events().isEmpty(), "Events should be collected");
        assertEquals(4, result.events().size());

        // In M4, migration handoff is actually performed during Phase 5!
        // Survivor 1 (initially in Region 1) has migrated to Region 2
        assertEquals(Optional.of(RegionId.REGION_2), settlement.survivor(new SurvivorId(1)).flatMap(Survivor::location));
    }
}
