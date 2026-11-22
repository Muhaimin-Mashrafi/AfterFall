package com.afterfall.simulation;

import com.afterfall.action.ActionResult;
import com.afterfall.action.ActionType;
import com.afterfall.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the batch apply functionality of {@link StateChangeApplier},
 * testing both pre-validation rejection and mid-application failure rollback.
 */
public class StateChangeApplierBatchTest {

    private Survivor s1, s2, s3;
    private Needs needs1, needs2, needs3;
    private Health health1, health2, health3;
    private Inventory inv1, inv2, inv3;

    @BeforeEach
    void setUp() {
        needs1 = Needs.fullySatisfied();
        needs2 = Needs.fullySatisfied();
        needs3 = Needs.fullySatisfied();
        health1 = Health.healthy();
        health2 = Health.healthy();
        health3 = Health.healthy();
        inv1 = new Inventory(20);
        inv2 = new Inventory(20);
        inv3 = new Inventory(20);

        for (Need n : Need.values()) {
            needs1.set(n, 5);
            needs2.set(n, 5);
            needs3.set(n, 5);
        }
        health1.setVitality(10);
        health2.setVitality(10);
        health3.setVitality(10);
        health1.setCondition(HealthCondition.HEALTHY);
        health2.setCondition(HealthCondition.HEALTHY);
        health3.setCondition(HealthCondition.HEALTHY);

        for (ItemType it : ItemType.values()) {
            inv1.add(it, 3);
            inv2.add(it, 3);
            inv3.add(it, 3);
        }
        s1 = new Survivor(new SurvivorId(1), "Survivor 1", Personality.neutral(),
                new Profession(ProfessionType.FARMER, 50), needs1, health1, inv1);
        s2 = new Survivor(new SurvivorId(2), "Survivor 2", Personality.neutral(),
                new Profession(ProfessionType.SCAVENGER, 40), needs2, health2, inv2);
        s3 = new Survivor(new SurvivorId(3), "Survivor 3", Personality.neutral(),
                new Profession(ProfessionType.MEDIC, 30), needs3, health3, inv3);
    }

    @Test
    void batchApplySuccess() {
        LinkedHashMap<Survivor, ActionResult> proposals = new LinkedHashMap<>();
        ActionResult r1 = ActionResult.builder(ActionType.EAT)
                .needDelta(Need.FOOD, -2)
                .vitalityDelta(1)
                .inventoryDelta(ItemType.FOOD, -1)
                .build();
        ActionResult r2 = ActionResult.builder(ActionType.WORK)
                .vitalityDelta(-3)
                .inventoryDelta(ItemType.MEDICINE, 1)
                .build();
        proposals.put(s1, r1);
        proposals.put(s2, r2);

        StateChangeApplier.applyBatch(proposals);

        assertEquals(3, needs1.level(Need.FOOD));
        assertEquals(11, health1.vitality());
        assertEquals(2, inv1.quantity(ItemType.FOOD));

        assertEquals(7, health2.vitality());
        assertEquals(4, inv2.quantity(ItemType.MEDICINE));
    }

    @Test
    void batchApplyPreValidationRejectionPreventsAllMutations() {
        LinkedHashMap<Survivor, ActionResult> proposals = new LinkedHashMap<>();
        ActionResult r1 = ActionResult.builder(ActionType.EAT)
                .needDelta(Need.FOOD, -1)
                .inventoryDelta(ItemType.FOOD, -1)
                .build();
        ActionResult r2 = ActionResult.builder(ActionType.WORK)
                .vitalityDelta(-2)
                .build();
        // r3 is invalid: tries to remove 5 FOOD when only 3 are held
        ActionResult r3 = ActionResult.builder(ActionType.EAT)
                .inventoryDelta(ItemType.FOOD, -5)
                .build();
        proposals.put(s1, r1);
        proposals.put(s2, r2);
        proposals.put(s3, r3);

        Executable exec = () -> StateChangeApplier.applyBatch(proposals);
        assertThrows(IllegalArgumentException.class, exec, "Pre-validation must reject batch before mutating any survivor");

        // Verify zero mutations occurred on any survivor
        for (Survivor s : new Survivor[]{s1, s2, s3}) {
            for (Need need : Need.values()) {
                assertEquals(5, s.needs().level(need), "Need unchanged for " + s.name());
            }
            assertEquals(10, s.health().vitality(), "Vitality unchanged for " + s.name());
            assertEquals(HealthCondition.HEALTHY, s.health().condition(), "Condition unchanged for " + s.name());
            for (ItemType it : ItemType.values()) {
                assertEquals(3, s.inventory().quantity(it), "Inventory unchanged for " + s.name());
            }
        }
    }

    @Test
    void batchApplyMidApplicationFailureRollsBackAllModifiedSurvivors() {
        // All proposals are completely valid according to pre-validation
        LinkedHashMap<Survivor, ActionResult> proposals = new LinkedHashMap<>();
        ActionResult r1 = ActionResult.builder(ActionType.EAT)
                .needDelta(Need.FOOD, -2)
                .vitalityDelta(3)
                .inventoryDelta(ItemType.FOOD, -1)
                .newCondition(HealthCondition.ILL)
                .build();
        ActionResult r2 = ActionResult.builder(ActionType.WORK)
                .vitalityDelta(-2)
                .inventoryDelta(ItemType.MEDICINE, 1)
                .build();
        ActionResult r3 = ActionResult.builder(ActionType.REST)
                .needDelta(Need.REST, 10)
                .vitalityDelta(1)
                .build();

        proposals.put(s1, r1);
        proposals.put(s2, r2);
        proposals.put(s3, r3);

        // Snapshot original states before application
        Map<Need, Integer> origNeeds1 = new EnumMap<>(Need.class);
        Map<Need, Integer> origNeeds2 = new EnumMap<>(Need.class);
        Map<Need, Integer> origNeeds3 = new EnumMap<>(Need.class);
        for (Need n : Need.values()) {
            origNeeds1.put(n, needs1.level(n));
            origNeeds2.put(n, needs2.level(n));
            origNeeds3.put(n, needs3.level(n));
        }

        // Test seam: inject failure when mutating survivor 3 (after survivor 1 and 2 are modified)
        RuntimeException injectedException = new IllegalStateException("Simulated hardware/database unexpected failure on Survivor 3");

        Executable exec = () -> StateChangeApplier.applyBatch(proposals, (actionResult, survivor) -> {
            if (survivor.id().equals(s3.id())) {
                throw injectedException;
            }
            StateChangeApplier.apply(actionResult, survivor);
        });

        RuntimeException thrown = assertThrows(RuntimeException.class, exec);
        assertSame(injectedException, thrown, "Original exception must be propagated");

        // Verify that Survivor 1 and Survivor 2 (which were successfully mutated) were restored to original state
        for (Need n : Need.values()) {
            assertEquals(origNeeds1.get(n), s1.needs().level(n), "Survivor 1 needs restored");
            assertEquals(origNeeds2.get(n), s2.needs().level(n), "Survivor 2 needs restored");
            assertEquals(origNeeds3.get(n), s3.needs().level(n), "Survivor 3 needs restored");
        }

        assertEquals(10, s1.health().vitality(), "Survivor 1 vitality restored");
        assertEquals(HealthCondition.HEALTHY, s1.health().condition(), "Survivor 1 condition restored");
        assertEquals(3, s1.inventory().quantity(ItemType.FOOD), "Survivor 1 inventory restored");

        assertEquals(10, s2.health().vitality(), "Survivor 2 vitality restored");
        assertEquals(3, s2.inventory().quantity(ItemType.MEDICINE), "Survivor 2 inventory restored");

        assertEquals(10, s3.health().vitality(), "Survivor 3 vitality restored");
        assertEquals(3, s3.inventory().quantity(ItemType.FOOD), "Survivor 3 inventory restored");
    }
}
