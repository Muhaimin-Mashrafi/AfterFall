package com.afterfall.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InventoryTest {

    @Test
    void startsEmptyWithGivenCapacity() {
        Inventory inventory = new Inventory(10);
        assertEquals(10, inventory.capacity());
        assertEquals(0, inventory.totalQuantity());
        assertEquals(10, inventory.freeSpace());
        for (ItemType type : ItemType.values()) {
            assertEquals(0, inventory.quantity(type));
        }
    }

    @Test
    void capacityMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new Inventory(0));
        assertThrows(IllegalArgumentException.class, () -> new Inventory(-5));
    }

    @Test
    void addAndRemoveChangeQuantitiesAndTotals() {
        Inventory inventory = new Inventory(10);
        inventory.add(ItemType.FOOD, 4);
        inventory.add(ItemType.WATER, 3);
        inventory.add(ItemType.FOOD, 1);
        assertEquals(5, inventory.quantity(ItemType.FOOD));
        assertEquals(8, inventory.totalQuantity());
        assertEquals(2, inventory.freeSpace());
        inventory.remove(ItemType.FOOD, 2);
        assertEquals(3, inventory.quantity(ItemType.FOOD));
        assertTrue(inventory.has(ItemType.FOOD, 3));
        assertFalse(inventory.has(ItemType.FOOD, 4));
    }

    @Test
    void hasRejectsNegativeAmountsAndAcceptsZero() {
        Inventory inventory = new Inventory(10);
        inventory.add(ItemType.FOOD, 2);

        // Negative amounts throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> inventory.has(ItemType.FOOD, -1));
        assertThrows(IllegalArgumentException.class, () -> inventory.has(ItemType.FOOD, -5));

        // Zero amount is allowed and returns true for valid item type
        assertTrue(inventory.has(ItemType.FOOD, 0));
        assertTrue(inventory.has(ItemType.WATER, 0));
        assertTrue(inventory.has(ItemType.MEDICINE, 0));

        // Null item type throws NullPointerException
        assertThrows(NullPointerException.class, () -> inventory.has(null, 0));
        assertThrows(NullPointerException.class, () -> inventory.has(null, 1));
    }

    @Test
    void cannotRemoveMoreThanHeldAndStateIsUnchanged() {
        Inventory inventory = new Inventory(10);
        inventory.add(ItemType.MEDICINE, 2);
        assertThrows(IllegalStateException.class, () -> inventory.remove(ItemType.MEDICINE, 3));
        assertEquals(2, inventory.quantity(ItemType.MEDICINE));
    }

    @Test
    void cannotExceedCapacityAndStateIsUnchanged() {
        Inventory inventory = new Inventory(5);
        inventory.add(ItemType.FOOD, 4);
        assertThrows(IllegalStateException.class, () -> inventory.add(ItemType.WATER, 2));
        assertEquals(4, inventory.totalQuantity());
        inventory.add(ItemType.WATER, 1);
        assertEquals(0, inventory.freeSpace());
    }

    @Test
    void amountsMustBePositive() {
        Inventory inventory = new Inventory(5);
        assertThrows(IllegalArgumentException.class, () -> inventory.add(ItemType.FOOD, 0));
        assertThrows(IllegalArgumentException.class, () -> inventory.add(ItemType.FOOD, -1));
        assertThrows(IllegalArgumentException.class, () -> inventory.remove(ItemType.FOOD, 0));
        assertThrows(IllegalArgumentException.class, () -> inventory.remove(ItemType.FOOD, -2));
    }

    @Test
    void nullTypeIsRejected() {
        Inventory inventory = new Inventory(5);
        assertThrows(NullPointerException.class, () -> inventory.add(null, 1));
        assertThrows(NullPointerException.class, () -> inventory.remove(null, 1));
        assertThrows(NullPointerException.class, () -> inventory.quantity(null));
    }
}
