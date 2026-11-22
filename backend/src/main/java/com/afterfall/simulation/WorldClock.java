package com.afterfall.simulation;

/**
 * Simple deterministic clock for the simulation. It tracks the current tick number and a configurable
 * mapping from ticks to days. The mapping is expressed as {@code ticksPerDay}. A value of 0 disables
 * day tracking (ticks only). The clock only advances when a tick is successfully completed.
 */
public final class WorldClock {
    private long tick;
    private final int ticksPerDay;
    private long day;

    /**
     * Creates a clock starting at tick 0. {@code ticksPerDay} must be positive when day tracking is
     * desired; a non‑positive value disables day tracking.
     */
    public WorldClock(int ticksPerDay) {
        if (ticksPerDay < 0) {
            throw new IllegalArgumentException("ticksPerDay cannot be negative");
        }
        this.ticksPerDay = ticksPerDay;
        this.tick = 0L;
        this.day = 0L;
    }

    public long tick() {
        return tick;
    }

    public long day() {
        return day;
    }

    public int ticksPerDay() {
        return ticksPerDay;
    }

    /** Advances the clock by one tick. If day tracking is enabled the day counter is updated
     *  according to the configured mapping.
     */
    void advance() {
        tick++;
        if (ticksPerDay > 0 && tick % ticksPerDay == 0) {
            day++;
        }
    }

    /** Rolls back a previously advanced tick. Used when a tick fails validation.
     *  The method restores both tick and day counters to the previous state.
     */
    void rollback() {
        if (tick == 0) {
            return; // nothing to rollback
        }
        long oldTick = tick - 1;
        long oldDay = day;
        if (ticksPerDay > 0 && tick % ticksPerDay == 0) {
            oldDay = day - 1;
        }
        this.tick = oldTick;
        this.day = oldDay;
    }
}
