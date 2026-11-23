package com.afterfall.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link DiseaseOutbreak}.
 * Verifies trigger (ill count threshold), spread mechanic, and resolution.
 */
class DiseaseOutbreakTest {

    private Settlement settlement;
    private Survivor alice;
    private Survivor bob;
    private Survivor carol;
    private Survivor dave;

    @BeforeEach
    void setUp() {
        settlement = new Settlement("Test");
        alice = makeSurvivor(1, "Alice", HealthCondition.HEALTHY, 100); // safe
        bob   = makeSurvivor(2, "Bob",   HealthCondition.ILL,     100); // ill, full safety
        carol = makeSurvivor(3, "Carol", HealthCondition.ILL,     100); // ill
        dave  = makeSurvivor(4, "Dave",  HealthCondition.HEALTHY,  20); // healthy but low safety → at risk
        settlement.registerSurvivor(alice, RegionId.REGION_1);
        settlement.registerSurvivor(bob,   RegionId.REGION_2);
        settlement.registerSurvivor(carol, RegionId.REGION_3);
        settlement.registerSurvivor(dave,  RegionId.REGION_4);
    }

    @Test
    void notActiveInitially() {
        DiseaseOutbreak outbreak = new DiseaseOutbreak(3);
        assertThat(outbreak.isActive()).isFalse();
    }

    @Test
    void doesNotTriggerBelowIllThreshold() {
        // Only 1 ill survivor (bob); threshold is 3
        DiseaseOutbreak outbreak = new DiseaseOutbreak(3);
        settlement = new Settlement("Test2");
        Survivor onlyIll = makeSurvivor(1, "OnlyIll", HealthCondition.ILL, 100);
        settlement.registerSurvivor(onlyIll, RegionId.REGION_1);
        settlement.registerSurvivor(makeSurvivor(2, "H2", HealthCondition.HEALTHY, 100), RegionId.REGION_2);
        settlement.registerSurvivor(makeSurvivor(3, "H3", HealthCondition.HEALTHY, 100), RegionId.REGION_3);
        settlement.registerSurvivor(makeSurvivor(4, "H4", HealthCondition.HEALTHY, 100), RegionId.REGION_4);
        ResourceStock stock = ResourceStock.defaultStock();
        List<CrisisEffect> effects = outbreak.advance(settlement, stock);
        assertThat(outbreak.isActive()).isFalse();
        assertThat(effects).isEmpty();
    }

    @Test
    void triggersAtIllThreshold() {
        // bob and carol are ill (2 ill), need threshold 2
        DiseaseOutbreak outbreak = new DiseaseOutbreak(2);
        ResourceStock stock = ResourceStock.defaultStock();
        List<CrisisEffect> effects = outbreak.advance(settlement, stock);
        assertThat(outbreak.isActive()).isTrue();
        assertThat(effects).isNotEmpty();
    }

    @Test
    void illSurvivorsReceiveVitalityPenalty() {
        DiseaseOutbreak outbreak = new DiseaseOutbreak(2);
        ResourceStock stock = ResourceStock.defaultStock();
        List<CrisisEffect> effects = outbreak.advance(settlement, stock);

        // bob and carol are ILL — must receive vitality penalty
        List<SurvivorId> affectedIds = effects.stream()
                .map(CrisisEffect::targetId)
                .toList();
        assertThat(affectedIds).contains(bob.id(), carol.id());

        for (CrisisEffect e : effects) {
            if (e.targetId().equals(bob.id()) || e.targetId().equals(carol.id())) {
                assertThat(e.vitalityDelta()).isEqualTo(DiseaseOutbreak.VITALITY_PENALTY);
            }
        }
    }

    @Test
    void lowSafetySurvivorsContractDisease() {
        // dave has safety = 20, below threshold (50): should get ILL condition
        DiseaseOutbreak outbreak = new DiseaseOutbreak(2);
        ResourceStock stock = ResourceStock.defaultStock();
        List<CrisisEffect> effects = outbreak.advance(settlement, stock);

        CrisisEffect daveEffect = effects.stream()
                .filter(e -> e.targetId().equals(dave.id()))
                .findFirst()
                .orElse(null);
        assertThat(daveEffect).isNotNull();
        assertThat(daveEffect.newCondition()).contains(HealthCondition.ILL);
        assertThat(daveEffect.vitalityDelta()).isEqualTo(DiseaseOutbreak.VITALITY_PENALTY);
    }

    @Test
    void highSafetyHealthySurvivorIsSpared() {
        // alice has safety = 100 (safe), should NOT be in effects
        DiseaseOutbreak outbreak = new DiseaseOutbreak(2);
        ResourceStock stock = ResourceStock.defaultStock();
        List<CrisisEffect> effects = outbreak.advance(settlement, stock);

        boolean aliceAffected = effects.stream().anyMatch(e -> e.targetId().equals(alice.id()));
        assertThat(aliceAffected).isFalse();
    }

    @Test
    void resolvesWhenNoIllSurvivors() {
        DiseaseOutbreak outbreak = new DiseaseOutbreak(2);
        ResourceStock stock = ResourceStock.defaultStock();
        // Trigger
        outbreak.advance(settlement, stock);
        assertThat(outbreak.isActive()).isTrue();

        // Cure everyone (simulate by marking them healthy)
        bob.health().setCondition(HealthCondition.HEALTHY);
        carol.health().setCondition(HealthCondition.HEALTHY);
        dave.health().setCondition(HealthCondition.HEALTHY);

        // Now no ill survivors → should resolve
        List<CrisisEffect> effects2 = outbreak.advance(settlement, stock);
        assertThat(outbreak.isActive()).isFalse();
        assertThat(effects2).isEmpty();
    }

    @Test
    void nameIsCorrect() {
        assertThat(new DiseaseOutbreak().name()).isEqualTo("Disease Outbreak");
    }

    // Helper: create survivor with a specified health condition and safety level
    private static Survivor makeSurvivor(int id, String name, HealthCondition condition, int safety) {
        Survivor s = new Survivor(
                new SurvivorId(id), name,
                Personality.neutral(),
                new Profession(ProfessionType.FARMER, 50),
                new Needs(50, 50, 50, safety),
                new Health(80, condition),
                new Inventory(10)
        );
        return s;
    }
}
