package com.afterfall.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Items carried by one survivor. Invariants: no quantity is ever negative, and the total never
 * exceeds the (positive) capacity. Operations that would break an invariant throw and leave the
 * inventory unchanged.
 */
public final class Inventory {

    private final int capacity;
    private final Map<ItemType, Integer> quantities = new EnumMap<>(ItemType.class);

    public Inventory(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive but was " + capacity);
        }
        this.capacity = capacity;
        for (ItemType type : ItemType.values()) {
            quantities.put(type, 0);
        }
    }

    public int capacity() {
        return capacity;
    }

    public int quantity(ItemType type) {
        return quantities.get(Objects.requireNonNull(type, "type"));
    }

    public int totalQuantity() {
        return quantities.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int freeSpace() {
        return capacity - totalQuantity();
    }

    public boolean has(ItemType type, int amount) {
        Objects.requireNonNull(type, "type");
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative but was " + amount);
        }
        return quantity(type) >= amount;
    }

    public void add(ItemType type, int amount) {
        Objects.requireNonNull(type, "type");
        requirePositive(amount);
        if (amount > freeSpace()) {
            throw new IllegalStateException(
                    "Not enough space: adding " + amount + " with " + freeSpace() + " free");
        }
        quantities.merge(type, amount, Integer::sum);
    }

    public void remove(ItemType type, int amount) {
        Objects.requireNonNull(type, "type");
        requirePositive(amount);
        if (amount > quantity(type)) {
            throw new IllegalStateException(
                    "Cannot remove " + amount + " " + type + "; only " + quantity(type) + " held");
        }
        quantities.merge(type, -amount, Integer::sum);
    }

    private static void requirePositive(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive but was " + amount);
        }
    }
}
