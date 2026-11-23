package com.afterfall.observer;

import com.afterfall.domain.*;
import com.afterfall.simulation.SeededRandomFactory;
import com.afterfall.simulation.WorldView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ObserverPolicyTest {

    private Settlement settlement;
    private SeededRandomFactory randomFactory;

    @BeforeEach
    void setUp() {
        settlement = new Settlement("Test Settlement", Settlement.defaultRegions(), new ResourceStock(50, 50, 20));
        randomFactory = new SeededRandomFactory(42L);

        // Add 4 survivors
        for (int i = 1; i <= 4; i++) {
            Survivor s = new Survivor(
                    new SurvivorId(i), "Survivor" + i,
                    Personality.neutral(),
                    new Profession(ProfessionType.FARMER, 50),
                    new Needs(50, 50, 50, 50),
                    Health.healthy(),
                    new Inventory(10)
            );
            settlement.registerSurvivor(s, RegionId.ALL.get(i - 1));
        }
    }

    @Test
    void informationDistortionPolicyTriggersOnLowTrustAndScarcity() {
        InformationDistortionPolicy policy = new InformationDistortionPolicy(60.0, 30, 20.0);

        // Setup relationship tension
        Survivor s1 = settlement.survivor(new SurvivorId(1)).get();
        Survivor s2 = settlement.survivor(new SurvivorId(2)).get();
        s1.addRelationship(new Relationship(s2.id(), RelationshipType.RIVAL, 20, 20, 80));

        // WorldView with low food
        settlement.resourceStock().resource(ResourceType.FOOD).consume(40); // food remaining = 10 <= 30
        World world = new World(settlement);
        WorldView worldView = WorldView.snapshot(world, 1, 1);

        ObservationReport report = ObservationReport.from(worldView, settlement);
        assertThat(policy.shouldTrigger(report)).isTrue();

        Optional<Intervention> intervention = policy.evaluate(report, worldView, settlement, 101L, randomFactory);
        assertThat(intervention).isPresent();
        assertThat(intervention.get().type()).isEqualTo(InterventionType.INFORMATION_DISTORTION);
        assertThat(intervention.get().needDeltas()).containsKey(Need.SAFETY);
        assertThat(intervention.get().originalReport()).contains("Settlement food supply");
        assertThat(intervention.get().alteredReport()).contains("CONFIDENTIAL");
    }

    @Test
    void trustErosionPolicyTriggersOnTension() {
        TrustErosionPolicy policy = new TrustErosionPolicy(20.0, -15);

        Survivor s1 = settlement.survivor(new SurvivorId(1)).get();
        Survivor s2 = settlement.survivor(new SurvivorId(2)).get();
        s1.addRelationship(new Relationship(s2.id(), RelationshipType.FRIEND, 40, 20, 60));

        World world = new World(settlement);
        WorldView worldView = WorldView.snapshot(world, 1, 1);
        ObservationReport report = ObservationReport.from(worldView, settlement);

        assertThat(policy.shouldTrigger(report)).isTrue();
        Optional<Intervention> intervention = policy.evaluate(report, worldView, settlement, 102L, randomFactory);
        assertThat(intervention).isPresent();
        assertThat(intervention.get().type()).isEqualTo(InterventionType.TRUST_EROSION);
        assertThat(intervention.get().trustDelta()).isEqualTo(-15);
        assertThat(intervention.get().targetSurvivor()).contains(s1.id());
        assertThat(intervention.get().relatedSurvivor()).contains(s2.id());
    }

    @Test
    void resourcePressurePolicyTriggersOnLowReserves() {
        ResourcePressurePolicy policy = new ResourcePressurePolicy(20, 10);

        // Consume food below threshold
        settlement.resourceStock().resource(ResourceType.FOOD).consume(35); // 15 remaining <= 20
        World world = new World(settlement);
        WorldView worldView = WorldView.snapshot(world, 1, 1);
        ObservationReport report = ObservationReport.from(worldView, settlement);

        assertThat(policy.shouldTrigger(report)).isTrue();
        Optional<Intervention> intervention = policy.evaluate(report, worldView, settlement, 103L, randomFactory);
        assertThat(intervention).isPresent();
        assertThat(intervention.get().type()).isEqualTo(InterventionType.RESOURCE_PRESSURE);
        assertThat(intervention.get().needDeltas()).containsKeys(Need.FOOD, Need.SAFETY);
        assertThat(intervention.get().targetRegion()).isPresent();
    }

    @Test
    void selectiveDisclosurePolicyTriggersOnIllness() {
        SelectiveDisclosurePolicy policy = new SelectiveDisclosurePolicy(1, 40.0);

        // Mark one survivor ill
        settlement.survivor(new SurvivorId(1)).get().health().setCondition(HealthCondition.ILL);
        World world = new World(settlement);
        WorldView worldView = WorldView.snapshot(world, 1, 1);
        ObservationReport report = ObservationReport.from(worldView, settlement);

        assertThat(policy.shouldTrigger(report)).isTrue();
        Optional<Intervention> intervention = policy.evaluate(report, worldView, settlement, 104L, randomFactory);
        assertThat(intervention).isPresent();
        assertThat(intervention.get().type()).isEqualTo(InterventionType.SELECTIVE_DISCLOSURE);
        assertThat(intervention.get().targetSurvivor()).contains(new SurvivorId(1));
        assertThat(intervention.get().alteredReport()).contains("Quarantine");
    }
}
