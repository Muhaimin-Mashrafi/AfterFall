package com.afterfall.domain;

import com.afterfall.domain.Region;
import com.afterfall.domain.RegionId;
import com.afterfall.domain.Settlement;
import com.afterfall.domain.SurvivorId;
import com.afterfall.domain.World;
import com.afterfall.simulation.TickCoordinator;
import com.afterfall.simulation.TickResult;
import com.afterfall.simulation.WorldClock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MembershipConsistencyRegressionTest {

    @Test
    void testFailsWhenSurvivorInRegionRosterIsMissingFromSettlement() {
        // Since Region.addSurvivor is package-private in com.afterfall.domain,
        // this test in com.afterfall.domain can invoke region.addSurvivor directly!
        Region r1 = new Region(RegionId.REGION_1, "North");
        r1.addSurvivor(new SurvivorId(999)); // Survivor 999 not registered in settlement

        Region r2 = new Region(RegionId.REGION_2, "East");
        Region r3 = new Region(RegionId.REGION_3, "South");
        Region r4 = new Region(RegionId.REGION_4, "West");

        Settlement settlement = new Settlement("Haven", List.of(r1, r2, r3, r4));
        World world = new World(settlement);
        WorldClock clock = new WorldClock(4);

        TickCoordinator coordinator = new TickCoordinator(world, clock);
        TickResult result = coordinator.executeTick();

        assertFalse(result.isSuccess(), "Tick must fail when a survivor in a region is missing from Settlement");
        assertTrue(result.failureReason().contains("Membership inconsistency"), "Failure reason must identify membership inconsistency");
        assertEquals(0L, clock.tick(), "Clock must not advance on membership inconsistency failure");
        assertEquals(0L, coordinator.worldVersion(), "World version must not advance on membership inconsistency failure");
    }
}
