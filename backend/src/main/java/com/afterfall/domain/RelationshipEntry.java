package com.afterfall.domain;

import java.util.Objects;

/** One history item: the relationship's state right after a change, with its day and cause. */
public record RelationshipEntry(int day, RelationshipType type, int trust, int affection,
                                int hostility, EventId sourceEventId) {

    public RelationshipEntry {
        if (day < 0) {
            throw new IllegalArgumentException("day must not be negative but was " + day);
        }
        Objects.requireNonNull(type, "type");
        Scale.requireInRange("trust", trust);
        Scale.requireInRange("affection", affection);
        Scale.requireInRange("hostility", hostility);
        Objects.requireNonNull(sourceEventId, "sourceEventId");
    }
}
