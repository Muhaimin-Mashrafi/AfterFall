package com.afterfall.domain;

/**
 * The normalized range shared by needs, health, personality, skill and relationship values.
 *
 * <p>All such values are integers in {@code [MIN, MAX]} = {@code [0, 100]}. The scale carries
 * no meaning of its own: what a given number triggers is decided by later behaviour code,
 * never by the domain model.
 */
public final class Scale {

    public static final int MIN = 0;
    public static final int MAX = 100;

    private Scale() {
    }

    /** Returns {@code value} if it is within range, otherwise throws. */
    public static int requireInRange(String name, int value) {
        if (value < MIN || value > MAX) {
            throw new IllegalArgumentException(
                    name + " must be in [" + MIN + ", " + MAX + "] but was " + value);
        }
        return value;
    }

    /** Limits {@code value} to the range. Uses long so large deltas cannot overflow. */
    public static int clamp(long value) {
        return (int) Math.max(MIN, Math.min(MAX, value));
    }
}
