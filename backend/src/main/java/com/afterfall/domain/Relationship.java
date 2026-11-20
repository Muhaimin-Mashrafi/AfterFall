package com.afterfall.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A directed relationship held by one survivor toward another: type plus trust, affection and
 * hostility (each 0-100). Changes are recorded in a minimal, bounded history so the Narrator can
 * later cite what happened. The history keeps only the most recent {@value #MAX_HISTORY} entries.
 */
public final class Relationship {

    public static final int MAX_HISTORY = 20;

    private final SurvivorId target;
    private RelationshipType type;
    private int trust;
    private int affection;
    private int hostility;
    private final List<RelationshipEntry> history = new ArrayList<>();

    public Relationship(SurvivorId target, RelationshipType type,
                        int trust, int affection, int hostility) {
        this.target = Objects.requireNonNull(target, "target");
        this.type = Objects.requireNonNull(type, "type");
        this.trust = Scale.requireInRange("trust", trust);
        this.affection = Scale.requireInRange("affection", affection);
        this.hostility = Scale.requireInRange("hostility", hostility);
    }

    public SurvivorId target() {
        return target;
    }

    public RelationshipType type() {
        return type;
    }

    public int trust() {
        return trust;
    }

    public int affection() {
        return affection;
    }

    public int hostility() {
        return hostility;
    }

    /** Applies clamped deltas and records the resulting state in the history. */
    public void adjust(int day, int trustDelta, int affectionDelta, int hostilityDelta,
                       EventId source) {
        requireDay(day);
        requireChronological(day);
        Objects.requireNonNull(source, "source");
        trust = Scale.clamp((long) trust + trustDelta);
        affection = Scale.clamp((long) affection + affectionDelta);
        hostility = Scale.clamp((long) hostility + hostilityDelta);
        record(day, source);
    }

    /** Changes the relationship type and records the resulting state in the history. */
    public void changeType(int day, RelationshipType newType, EventId source) {
        requireDay(day);
        requireChronological(day);
        Objects.requireNonNull(source, "source");
        this.type = Objects.requireNonNull(newType, "newType");
        record(day, source);
    }

    /** Unmodifiable, oldest first. */
    public List<RelationshipEntry> history() {
        return List.copyOf(history);
    }

    private void record(int day, EventId source) {
        history.add(new RelationshipEntry(day, type, trust, affection, hostility, source));
        if (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
    }

    private static void requireDay(int day) {
        if (day < 0) {
            throw new IllegalArgumentException("day must not be negative but was " + day);
        }
    }

    private void requireChronological(int day) {
        if (!history.isEmpty()) {
            int lastDay = history.get(history.size() - 1).day();
            if (day < lastDay) {
                throw new IllegalArgumentException(
                        "day " + day + " must not be earlier than last history entry day " + lastDay);
            }
        }
    }
}
