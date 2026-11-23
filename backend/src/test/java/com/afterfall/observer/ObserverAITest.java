package com.afterfall.observer;

import com.afterfall.domain.*;
import com.afterfall.simulation.SeededRandomFactory;
import com.afterfall.simulation.WorldView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ObserverAITest {

    private Settlement settlement;
    private ObserverAI observerAI;
    private SeededRandomFactory randomFactory;

    @BeforeEach
    void setUp() {
        settlement = new Settlement("Observer Test Settlement", Settlement.defaultRegions(), new ResourceStock(10, 50, 5));
        observerAI = new ObserverAI();
        randomFactory = new SeededRandomFactory(100L);

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
    void analyzeAndProposeDoesNotMutateDomainState() {
        World world = new World(settlement);
        WorldView snapshotBefore = WorldView.snapshot(world, 1, 1);

        int foodBefore = settlement.resourceStock().quantity(ResourceType.FOOD);
        int safetyBefore = settlement.survivor(new SurvivorId(1)).get().needs().level(Need.SAFETY);

        Optional<Intervention> intervention = observerAI.analyzeAndPropose(snapshotBefore, settlement, randomFactory);
        assertThat(intervention).isPresent();

        // Verify zero direct state mutations occurred during analysis
        assertThat(settlement.resourceStock().quantity(ResourceType.FOOD)).isEqualTo(foodBefore);
        assertThat(settlement.survivor(new SurvivorId(1)).get().needs().level(Need.SAFETY)).isEqualTo(safetyBefore);
    }

    @Test
    void deterministicProposalForIdenticalSeedAndSnapshot() {
        World world = new World(settlement);
        WorldView snapshot = WorldView.snapshot(world, 1, 1);

        ObserverAI ai1 = new ObserverAI();
        ObserverAI ai2 = new ObserverAI();

        Optional<Intervention> p1 = ai1.analyzeAndPropose(snapshot, settlement, new SeededRandomFactory(555L));
        Optional<Intervention> p2 = ai2.analyzeAndPropose(snapshot, settlement, new SeededRandomFactory(555L));

        assertThat(p1).isPresent();
        assertThat(p2).isPresent();
        assertThat(p1.get().type()).isEqualTo(p2.get().type());
        assertThat(p1.get().intendedEffect()).isEqualTo(p2.get().intendedEffect());
        assertThat(p1.get().alteredReport()).isEqualTo(p2.get().alteredReport());
    }

    @Test
    void recordsAuditTrailAccurately() {
        Intervention intervention = Intervention.builder(1L, 1L, InterventionType.RESOURCE_PRESSURE)
                .targetRegion(RegionId.REGION_1)
                .reason("Test reason")
                .intendedEffect("Test effect")
                .originalReport("Original")
                .alteredReport("Altered")
                .build();

        observerAI.record(intervention, InterventionStatus.APPLIED, "Success");

        assertThat(observerAI.records()).hasSize(1);
        InterventionRecord record = observerAI.records().get(0);
        assertThat(record.interventionId()).isEqualTo(1L);
        assertThat(record.status()).isEqualTo(InterventionStatus.APPLIED);
        assertThat(record.originalReport()).isEqualTo("Original");
        assertThat(record.alteredReport()).isEqualTo("Altered");
    }

    @Test
    void preventsDuplicatePolicyInterventionOnSameTick() {
        World world = new World(settlement);
        WorldView snapshot = WorldView.snapshot(world, 1, 1);

        Optional<Intervention> first = observerAI.analyzeAndPropose(snapshot, settlement, randomFactory);
        assertThat(first).isPresent();

        // On the exact same tick, the same policy should not re-trigger
        Optional<Intervention> duplicate = observerAI.analyzeAndPropose(snapshot, settlement, randomFactory);
        if (duplicate.isPresent()) {
            assertThat(duplicate.get().type()).isNotEqualTo(first.get().type());
        }
    }

    @Test
    void originalAndAlteredReportsAreImmutable() {
        Intervention intervention = Intervention.builder(1L, 1L, InterventionType.INFORMATION_DISTORTION)
                .reason("R")
                .intendedEffect("E")
                .originalReport("Original immutable report")
                .alteredReport("Altered immutable report")
                .needDelta(Need.SAFETY, -10)
                .build();

        assertThat(intervention.originalReport()).isEqualTo("Original immutable report");
        assertThat(intervention.alteredReport()).isEqualTo("Altered immutable report");
        assertThat(intervention.needDeltas()).isUnmodifiable();
    }
}
