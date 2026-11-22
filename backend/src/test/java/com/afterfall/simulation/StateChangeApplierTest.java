package com.afterfall.simulation;

import com.afterfall.action.ActionResult;
import com.afterfall.action.ActionType;
import com.afterfall.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link StateChangeApplier} ensuring atomic application and rollback on failure.
 */
public class StateChangeApplierTest {

    private Survivor survivor;
    private Needs needs;
    private Health health;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        // Use domain default factories
        needs = Needs.fullySatisfied();
        health = Health.healthy();
        inventory = new Inventory(20);
        // Set baseline levels
        for (Need need : Need.values()) {
            needs.set(need, 5);
        }
        health.setVitality(10);
        health.setCondition(HealthCondition.HEALTHY);
        for (ItemType item : ItemType.values()) {
            inventory.add(item, 3);
        }
        survivor = new Survivor(new SurvivorId(1), "Test Survivor",
                Personality.neutral(), new Profession(ProfessionType.FARMER, 50),
                needs, health, inventory);
    }

    @Test
    void applyValidResultChangesState() {
        Map<Need, Integer> needDeltas = new EnumMap<>(Need.class);
        needDeltas.put(Need.FOOD, -2);
        needDeltas.put(Need.WATER, 1);

        Map<ItemType, Integer> inventoryDeltas = new EnumMap<>(ItemType.class);
        inventoryDeltas.put(ItemType.FOOD, -1);
        inventoryDeltas.put(ItemType.MEDICINE, 2);

        ActionResult result = ActionResult.builder(ActionType.EAT)
                .needDelta(Need.FOOD, -2)
                .needDelta(Need.WATER, 1)
                .vitalityDelta(3)
                .newCondition(HealthCondition.ILL)
                .inventoryDelta(ItemType.FOOD, -1)
                .inventoryDelta(ItemType.MEDICINE, 2)
                .build();

        StateChangeApplier.apply(result, survivor);

        assertEquals(3, needs.level(Need.FOOD));
        assertEquals(6, needs.level(Need.WATER));
        assertEquals(13, health.vitality());
        assertEquals(HealthCondition.ILL, health.condition());
        assertEquals(2, inventory.quantity(ItemType.FOOD));
        assertEquals(5, inventory.quantity(ItemType.MEDICINE));
    }

    @Test
    void applyResultFailureRollsBackState() {
        Map<Need, Integer> needDeltas = new EnumMap<>(Need.class);
        needDeltas.put(Need.FOOD, -1);

        Map<ItemType, Integer> inventoryDeltas = new EnumMap<>(ItemType.class);
        // Attempt to remove more than present (inventory initialized with 3 each)
        inventoryDeltas.put(ItemType.FOOD, -5);

        ActionResult badResult = ActionResult.builder(ActionType.EAT)
                .needDelta(Need.FOOD, -1)
                .inventoryDelta(ItemType.FOOD, -5)
                .build();

        // Capture original state snapshots
        Map<Need, Integer> originalNeeds = new EnumMap<>(Need.class);
        for (Need n : Need.values()) {
            originalNeeds.put(n, needs.level(n));
        }
        int originalVitality = health.vitality();
        HealthCondition originalCondition = health.condition();
        Map<ItemType, Integer> originalInventory = new EnumMap<>(ItemType.class);
        for (ItemType it : ItemType.values()) {
            originalInventory.put(it, inventory.quantity(it));
        }

        Executable executable = () -> StateChangeApplier.apply(badResult, survivor);
        assertThrows(RuntimeException.class, executable);

        // Verify rollback
        for (Need n : Need.values()) {
            assertEquals(originalNeeds.get(n), needs.level(n), "Need " + n + " rolled back");
        }
        assertEquals(originalVitality, health.vitality(), "Vitality rolled back");
        assertEquals(originalCondition, health.condition(), "Condition rolled back");
        for (ItemType it : ItemType.values()) {
            assertEquals(originalInventory.get(it), inventory.quantity(it), "Inventory " + it + " rolled back");
        }
    }
}
