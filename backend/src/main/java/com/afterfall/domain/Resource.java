package com.afterfall.domain;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Settlement-level shared resource (food, water or medicine).
 *
 * <p>Invariant: quantity is always in {@code [0, Integer.MAX_VALUE]}.
 * The quantity never goes negative.
 *
 * <p>Thread-safety: all mutations are protected by a {@link ReentrantLock}.
 * Workers never call this directly; they submit {@link com.afterfall.action.ResourceClaim}s.
 * The TickCoordinator resolves claims as the sole writer.
 * The lock also makes this class correct for the race-condition test (M12).
 */
public final class Resource {

    private final ResourceType type;
    private int quantity;
    private final ReentrantLock lock = new ReentrantLock();

    public Resource(ResourceType type, int initialQuantity) {
        this.type = Objects.requireNonNull(type, "type");
        if (initialQuantity < 0) {
            throw new IllegalArgumentException("initialQuantity must not be negative");
        }
        this.quantity = initialQuantity;
    }

    public ResourceType type() {
        return type;
    }

    /** Current quantity; acquired under lock for consistency. */
    public int quantity() {
        lock.lock();
        try {
            return quantity;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Adds the given amount to stock.
     *
     * @param amount positive amount to add
     * @throws IllegalArgumentException if amount is not positive
     */
    public void add(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive but was " + amount);
        }
        lock.lock();
        try {
            quantity += amount;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Consumes up to {@code amount} units, never going below zero.
     *
     * @param amount positive amount to consume
     * @return the amount actually consumed (may be less than requested if stock is low)
     * @throws IllegalArgumentException if amount is not positive
     */
    public int consume(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive but was " + amount);
        }
        lock.lock();
        try {
            int actual = Math.min(amount, quantity);
            quantity -= actual;
            return actual;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns {@code true} if stock is currently above zero.
     */
    public boolean isAvailable() {
        return quantity() > 0;
    }

    @Override
    public String toString() {
        return "Resource{type=" + type + ", quantity=" + quantity() + "}";
    }
}
