package com.afterfall.domain;

/** Reference to a simulation event (the event type itself arrives in a later milestone). */
public record EventId(long value) implements Comparable<EventId> {

    public EventId {
        if (value <= 0) {
            throw new IllegalArgumentException("EventId must be positive but was " + value);
        }
    }

    @Override
    public int compareTo(EventId other) {
        return Long.compare(value, other.value);
    }
}
