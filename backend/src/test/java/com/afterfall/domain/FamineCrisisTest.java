package com.afterfall.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link FamineCrisis}.
 * Verifies trigger, effects, and resolution via the Template Method.
 */
class FamineCrisisTest {

    private Settlement settlement;
    private Survivor alice;
    private Survivor bob;

    @BeforeEach
    void setUp() {
        settlement = new Settlement("Test");
        alice = makeSurvivor(1, "Alice");
        bob   = makeSurvivor(2, "Bob");
        settlement.registerSurvivor(alice, RegionId.REGION_1);
        settlement.registerSurvivor(bob,   RegionId.REGION_2);
    }

    @Test
    void notActiveInitially() {
        FamineCrisis crisis = new FamineCrisis();
        assertThat(crisis.isActive()).isFalse();
        assertThat(crisis.activeTicks()).isZero();
    }

    @Test
    void doesNotTriggerWhenFoodAboveThreshold() {
        FamineCrisis crisis = new FamineCrisis(10);
        ResourceStock stock = new ResourceStock(50, 50, 20); // food=50 > 10
        List<CrisisEffect> effects = crisis.advance(settlement, stock);
        assertThat(crisis.isActive()).isFalse();
        assertThat(effects).isEmpty();
    }

    @Test
    void triggersWhenFoodAtThreshold() {
        FamineCrisis crisis = new FamineCrisis(10);
        ResourceStock stock = new ResourceStock(10, 50, 20); // food=10 == threshold
        List<CrisisEffect> effects = crisis.advance(settlement, stock);
        assertThat(crisis.isActive()).isTrue();
        assertThat(effects).hasSize(2); // one per alive survivor
    }

    @Test
    void triggersWhenFoodBelowThreshold() {
        FamineCrisis crisis = new FamineCrisis(10);
        ResourceStock stock = new ResourceStock(5, 50, 20); // food=5 < 10
        List<CrisisEffect> effects = crisis.advance(settlement, stock);
        assertThat(crisis.isActive()).isTrue();
        assertThat(effects).hasSize(2);
    }

    @Test
    void effectsContainFoodNeedAndVitalityPenalties() {
        FamineCrisis crisis = new FamineCrisis(10);
        ResourceStock stock = new ResourceStock(0, 50, 20);
        List<CrisisEffect> effects = crisis.advance(settlement, stock);

        for (CrisisEffect effect : effects) {
            assertThat(effect.needDeltas()).containsKey(Need.FOOD);
            assertThat(effect.needDeltas().get(Need.FOOD)).isEqualTo(FamineCrisis.FOOD_NEED_PENALTY);
            assertThat(effect.vitalityDelta()).isEqualTo(FamineCrisis.VITALITY_PENALTY);
        }
    }

    @Test
    void resolvesWhenFoodRecoversAboveThreshold() {
        FamineCrisis crisis = new FamineCrisis(10);
        ResourceStock low  = new ResourceStock(5,  50, 20);
        ResourceStock high = new ResourceStock(50, 50, 20);

        // Trigger
        crisis.advance(settlement, low);
        assertThat(crisis.isActive()).isTrue();

        // Resolve
        List<CrisisEffect> effects = crisis.advance(settlement, high);
        assertThat(crisis.isActive()).isFalse();
        assertThat(effects).isEmpty();
        assertThat(crisis.activeTicks()).isZero();
    }

    @Test
    void activeTicksIncrementsEachTick() {
        FamineCrisis crisis = new FamineCrisis(10);
        ResourceStock low = new ResourceStock(0, 50, 20);
        crisis.advance(settlement, low);
        assertThat(crisis.activeTicks()).isEqualTo(1);
        crisis.advance(settlement, low);
        assertThat(crisis.activeTicks()).isEqualTo(2);
        crisis.advance(settlement, low);
        assertThat(crisis.activeTicks()).isEqualTo(3);
    }

    @Test
    void deadSurvivorsAreExcludedFromEffects() {
        alice.markDead();
        FamineCrisis crisis = new FamineCrisis(10);
        ResourceStock stock = new ResourceStock(0, 50, 20);
        List<CrisisEffect> effects = crisis.advance(settlement, stock);
        // Only bob is alive
        assertThat(effects).hasSize(1);
        assertThat(effects.get(0).targetId()).isEqualTo(bob.id());
    }

    private static Survivor makeSurvivor(int id, String name) {
        return new Survivor(
                new SurvivorId(id), name,
                Personality.neutral(),
                new Profession(ProfessionType.FARMER, 50),
                Needs.fullySatisfied(),
                Health.healthy(),
                new Inventory(10)
        );
    }
}
