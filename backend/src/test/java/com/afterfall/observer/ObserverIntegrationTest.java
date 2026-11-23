package com.afterfall.observer;

import com.afterfall.domain.*;
import com.afterfall.simulation.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ObserverIntegrationTest {

    private World world;
    private WorldClock clock;
    private RegionWorkerPool pool;
    private TickCoordinator coordinator;
    private ObserverAI observerAI;

    @BeforeEach
    void setUp() {
        Settlement settlement = new Settlement("Observer Integration", Settlement.defaultRegions(), new ResourceStock(10, 50, 5));
        // Register survivors
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

        // Add a relationship between 1 and 2
        Survivor s1 = settlement.survivor(new SurvivorId(1)).get();
        Survivor s2 = settlement.survivor(new SurvivorId(2)).get();
        s1.addRelationship(new Relationship(s2.id(), RelationshipType.FRIEND, 50, 50, 0));

        world = new World(settlement);
        clock = new WorldClock(4);
        pool = new RegionWorkerPool();
        observerAI = new ObserverAI();
        coordinator = new TickCoordinator(
                world, clock,
                ctx -> new com.afterfall.behavior.BehaviorSelector().selectAction(ctx),
                pool, false, new SeededRandomFactory(42L),
                new CrisisManager(),
                observerAI
        );
    }

    @AfterEach
    void tearDown() {
        pool.close();
    }

    @Test
    void queuedInterventionAppliesAtTickBoundary() {
        Survivor target = world.settlement().survivor(new SurvivorId(1)).get();
        int initialSafety = target.needs().level(Need.SAFETY);

        Intervention intervention = Intervention.builder(1L, 1L, InterventionType.INFORMATION_DISTORTION)
                .targetSurvivor(target.id())
                .reason("Elevate vigilance")
                .intendedEffect("Reduce perceived security")
                .originalReport("All secure")
                .alteredReport("Breach incoming")
                .needDelta(Need.SAFETY, -20)
                .build();

        coordinator.queueIntervention(intervention);

        // Before tick execution, target state is untouched
        assertThat(target.needs().level(Need.SAFETY)).isEqualTo(initialSafety);

        // Execute tick (boundary merge applies queued intervention)
        TickResult result = coordinator.executeTick();
        assertThat(result.isSuccess()).isTrue();

        // Applied at safe boundary
        assertThat(target.needs().level(Need.SAFETY)).isEqualTo(initialSafety - 20);

        // Recorded in ObserverAI audit trail
        assertThat(observerAI.records()).hasSize(1);
        assertThat(observerAI.records().get(0).status()).isEqualTo(InterventionStatus.APPLIED);
    }

    @Test
    void rejectedInterventionOnDeadOrMissingSurvivor() {
        // Target an unmanaged / non-existent survivor id
        SurvivorId nonExistent = new SurvivorId(999);

        Intervention intervention = Intervention.builder(2L, 1L, InterventionType.INFORMATION_DISTORTION)
                .targetSurvivor(nonExistent)
                .reason("Target non-existent survivor")
                .intendedEffect("Should fail safely")
                .originalReport("A")
                .alteredReport("B")
                .needDelta(Need.SAFETY, -10)
                .build();

        coordinator.queueIntervention(intervention);
        TickResult result = coordinator.executeTick();
        assertThat(result.isSuccess()).isTrue();

        // Audit log reflects REJECTED
        assertThat(observerAI.records()).hasSize(1);
        assertThat(observerAI.records().get(0).status()).isEqualTo(InterventionStatus.REJECTED);
        assertThat(observerAI.records().get(0).actualResult()).contains("does not exist or is deceased");
    }

    @Test
    void trustErosionInterventionReducesRelationshipTrust() {
        Survivor s1 = world.settlement().survivor(new SurvivorId(1)).get();
        Survivor s2 = world.settlement().survivor(new SurvivorId(2)).get();

        int trustBefore = s1.relationshipWith(s2.id()).get().trust();

        Intervention intervention = Intervention.builder(3L, 1L, InterventionType.TRUST_EROSION)
                .targetSurvivor(s1.id())
                .relatedSurvivor(s2.id())
                .reason("Erode trust")
                .intendedEffect("Sever social cohesion")
                .originalReport("Stable")
                .alteredReport("Betrayal detected")
                .trustDelta(-25)
                .build();

        coordinator.queueIntervention(intervention);
        coordinator.executeTick();

        int trustAfter = s1.relationshipWith(s2.id()).get().trust();
        assertThat(trustAfter).isEqualTo(trustBefore - 25);
    }

    @Test
    void conflictingInterventionsAppliedInDeterministicIdOrder() {
        Survivor s1 = world.settlement().survivor(new SurvivorId(1)).get();
        int safetyBefore = s1.needs().level(Need.SAFETY);

        // Queue two interventions targeting the same survivor
        Intervention first = Intervention.builder(10L, 1L, InterventionType.INFORMATION_DISTORTION)
                .targetSurvivor(s1.id())
                .reason("First")
                .intendedEffect("First effect")
                .originalReport("O1")
                .alteredReport("A1")
                .needDelta(Need.SAFETY, -10)
                .build();

        Intervention second = Intervention.builder(20L, 1L, InterventionType.RESOURCE_PRESSURE)
                .targetSurvivor(s1.id())
                .reason("Second")
                .intendedEffect("Second effect")
                .originalReport("O2")
                .alteredReport("A2")
                .needDelta(Need.SAFETY, -15)
                .build();

        // Queue out of order
        coordinator.queueIntervention(second);
        coordinator.queueIntervention(first);

        coordinator.executeTick();

        // Both applied sequentially in ID order: total delta = -25
        assertThat(s1.needs().level(Need.SAFETY)).isEqualTo(safetyBefore - 25);

        // Verify audit trail recorded both in deterministic execution order
        assertThat(observerAI.records()).hasSize(2);
        assertThat(observerAI.records().get(0).interventionId()).isEqualTo(10L);
        assertThat(observerAI.records().get(1).interventionId()).isEqualTo(20L);
    }
}
