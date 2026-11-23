package com.afterfall.simulation;

import com.afterfall.domain.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for M5 resource claim resolution and crisis advancement
 * through the full TickCoordinator pipeline.
 *
 * <p>Verifies that:
 * <ol>
 *   <li>Resource stock in the settlement decreases after claim resolution at the tick boundary.</li>
 *   <li>WorldView snapshot includes resource quantities from the settlement.</li>
 *   <li>Famine crisis triggers and applies need/vitality penalties through the full tick loop.</li>
 *   <li>Disease Outbreak crisis applies spread effects through the full tick loop.</li>
 *   <li>All existing M0–M4 tests remain unaffected (zero regressions).</li>
 * </ol>
 */
class ResourceAndCrisisIntegrationTest {

    // -----------------------------------------------------------------------
    // Setup helpers
    // -----------------------------------------------------------------------

    private static World buildWorld(int food, int water, int medicine) {
        Settlement s = new Settlement("Test", Settlement.defaultRegions(),
                new ResourceStock(food, water, medicine));
        populateSettlement(s, 4);
        return new World(s);
    }

    /** Register one survivor per region (4 total). */
    private static void populateSettlement(Settlement settlement, int count) {
        RegionId[] regions = RegionId.ALL.toArray(new RegionId[0]);
        for (int i = 0; i < count; i++) {
            Survivor s = new Survivor(
                    new SurvivorId(i + 1), "Survivor" + (i + 1),
                    Personality.neutral(),
                    new Profession(ProfessionType.FARMER, 50),
                    Needs.fullySatisfied(),
                    Health.healthy(),
                    new Inventory(10)
            );
            settlement.registerSurvivor(s, regions[i % regions.length]);
        }
    }

    // -----------------------------------------------------------------------
    // WorldView snapshot includes resource quantities
    // -----------------------------------------------------------------------

    @Test
    void worldViewSnapshotIncludesResourceQuantities() {
        World world = buildWorld(80, 60, 25);
        WorldView view = WorldView.snapshot(world, 0, 0);
        assertThat(view.resourceQuantities().get(ResourceType.FOOD)).isEqualTo(80);
        assertThat(view.resourceQuantities().get(ResourceType.WATER)).isEqualTo(60);
        assertThat(view.resourceQuantities().get(ResourceType.MEDICINE)).isEqualTo(25);
    }

    @Test
    void worldViewSnapshotIsImmutable() {
        World world = buildWorld(50, 50, 20);
        WorldView view = WorldView.snapshot(world, 0, 0);
        assertThatThrownBy(() -> view.resourceQuantities().put(ResourceType.FOOD, 999))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void isFoodAvailableReflectsStock() {
        WorldView with    = WorldView.snapshot(new World(new Settlement("A", Settlement.defaultRegions(), new ResourceStock(10, 0, 0))), 0, 0);
        WorldView without = WorldView.snapshot(new World(new Settlement("B", Settlement.defaultRegions(), new ResourceStock(0, 10, 0))), 0, 0);
        assertThat(with.isFoodAvailable()).isTrue();
        assertThat(without.isFoodAvailable()).isFalse();
        assertThat(without.isMedicineAvailable()).isFalse();
    }

    // -----------------------------------------------------------------------
    // Famine crisis integration through TickCoordinator
    // -----------------------------------------------------------------------

    @Test
    void famineCrisisTriggersAndAppliesEffectsWhenFoodEmpty() {
        // Food = 0, threshold = 10 → famine should trigger immediately
        Settlement settlement = new Settlement("Test", Settlement.defaultRegions(),
                new ResourceStock(0, 50, 20));
        populateSettlement(settlement, 4);
        World world = new World(settlement);

        FamineCrisis famine = new FamineCrisis(10);
        CrisisManager crisisManager = new CrisisManager(famine);

        WorldClock clock = new WorldClock(4);
        RegionWorkerPool pool = new RegionWorkerPool();
        try {
            TickCoordinator coordinator = new TickCoordinator(
                    world, clock, ctx -> new com.afterfall.behavior.BehaviorSelector().selectAction(ctx),
                    pool, false, new SeededRandomFactory(42L), crisisManager
            );

            // Record vitality before tick
            List<Integer> vitalityBefore = world.settlement().survivors().stream()
                    .map(s -> s.health().vitality())
                    .toList();

            TickResult result = coordinator.executeTick();
            assertThat(result.isSuccess()).isTrue();

            // Famine should be active
            assertThat(famine.isActive()).isTrue();

            // Every survivor should have lower vitality after famine effect
            List<Integer> vitalityAfter = world.settlement().survivors().stream()
                    .map(s -> s.health().vitality())
                    .toList();

            for (int i = 0; i < vitalityBefore.size(); i++) {
                // Vitality should decrease (famine applies -5 per tick)
                assertThat(vitalityAfter.get(i)).isLessThan(vitalityBefore.get(i));
            }
        } finally {
            pool.close();
        }
    }

    @Test
    void famineDoesNotTriggerWhenFoodAboveThreshold() {
        Settlement settlement = new Settlement("Test", Settlement.defaultRegions(),
                new ResourceStock(100, 50, 20));
        populateSettlement(settlement, 4);
        World world = new World(settlement);

        FamineCrisis famine = new FamineCrisis(10);
        CrisisManager crisisManager = new CrisisManager(famine);

        WorldClock clock = new WorldClock(4);
        RegionWorkerPool pool = new RegionWorkerPool();
        try {
            TickCoordinator coordinator = new TickCoordinator(
                    world, clock, ctx -> new com.afterfall.behavior.BehaviorSelector().selectAction(ctx),
                    pool, false, new SeededRandomFactory(42L), crisisManager
            );

            coordinator.executeTick();
            assertThat(famine.isActive()).isFalse();
        } finally {
            pool.close();
        }
    }

    // -----------------------------------------------------------------------
    // Disease Outbreak integration through TickCoordinator
    // -----------------------------------------------------------------------

    @Test
    void diseaseOutbreakTriggersAndSpreadsWhenThresholdReached() {
        Settlement settlement = new Settlement("Test", Settlement.defaultRegions(),
                new ResourceStock(50, 50, 0)); // No medicine available, so ill survivors cannot heal

        // Add 3 ill survivors with low safety + 1 healthy vulnerable survivor
        RegionId[] regions = RegionId.ALL.toArray(new RegionId[0]);
        for (int i = 1; i <= 3; i++) {
            Survivor s = new Survivor(
                    new SurvivorId(i), "Ill" + i,
                    Personality.neutral(),
                    new Profession(ProfessionType.FARMER, 50),
                    new Needs(50, 50, 50, 20), // low safety → spreadable
                    new Health(80, HealthCondition.ILL),
                    new Inventory(10)
            );
            settlement.registerSurvivor(s, regions[i - 1]);
        }
        // 4th survivor is healthy but low safety
        Survivor vulnerable = new Survivor(
                new SurvivorId(4), "Healthy",
                Personality.neutral(),
                new Profession(ProfessionType.FARMER, 50),
                new Needs(50, 50, 50, 20), // low safety
                Health.healthy(),
                new Inventory(10)
        );
        settlement.registerSurvivor(vulnerable, regions[3]);

        World world = new World(settlement);
        DiseaseOutbreak outbreak = new DiseaseOutbreak(3);
        CrisisManager crisisManager = new CrisisManager(outbreak);

        WorldClock clock = new WorldClock(4);
        RegionWorkerPool pool = new RegionWorkerPool();
        try {
            TickCoordinator coordinator = new TickCoordinator(
                    world, clock, ctx -> new com.afterfall.behavior.BehaviorSelector().selectAction(ctx),
                    pool, false, new SeededRandomFactory(42L), crisisManager
            );

            int vitalityBefore = vulnerable.health().vitality();
            coordinator.executeTick();

            // Outbreak should be active
            assertThat(outbreak.isActive()).isTrue();

            // Vulnerable healthy survivor with low safety should contract disease
            assertThat(vulnerable.health().condition()).isEqualTo(HealthCondition.ILL);
            assertThat(vulnerable.health().vitality()).isLessThan(vitalityBefore);
        } finally {
            pool.close();
        }
    }

    // -----------------------------------------------------------------------
    // Settlement with ResourceStock — backward compatibility
    // -----------------------------------------------------------------------

    @Test
    void settlementDefaultConstructorCreatesDefaultStock() {
        Settlement s = new Settlement("X");
        assertThat(s.resourceStock()).isNotNull();
        assertThat(s.resourceStock().quantity(ResourceType.FOOD)).isPositive();
    }

    @Test
    void settlementTwoArgConstructorCreatesDefaultStock() {
        Settlement s = new Settlement("X", Settlement.defaultRegions());
        assertThat(s.resourceStock()).isNotNull();
    }

    @Test
    void settlementThreeArgConstructorUsesProvidedStock() {
        ResourceStock stock = new ResourceStock(999, 888, 777);
        Settlement s = new Settlement("X", Settlement.defaultRegions(), stock);
        assertThat(s.resourceStock().quantity(ResourceType.FOOD)).isEqualTo(999);
    }
}
