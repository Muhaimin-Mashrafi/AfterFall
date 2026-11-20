package com.afterfall.domain;

import java.util.Objects;
import java.util.Optional;

/**
 * A significant experience remembered by a survivor. Immutable. It stores no free text: the
 * {@code sourceEventId} points at the real event, so nothing is invented. Emotional impact is
 * signed, from {@value #MIN_IMPACT} (deeply negative) to {@value #MAX_IMPACT} (deeply positive),
 * with 0 representing an emotionally neutral experience.
 */
public final class CharacterMemory {

    public static final int MIN_IMPACT = -100;
    public static final int MAX_IMPACT = 100;

    private final int day;
    private final int emotionalImpact;
    private final SurvivorId relatedSurvivor;
    private final EventId sourceEventId;

    /** {@code relatedSurvivor} may be null when the memory involves no other survivor. */
    public CharacterMemory(int day, int emotionalImpact, SurvivorId relatedSurvivor,
                           EventId sourceEventId) {
        if (day < 0) {
            throw new IllegalArgumentException("day must not be negative but was " + day);
        }
        if (emotionalImpact < MIN_IMPACT || emotionalImpact > MAX_IMPACT) {
            throw new IllegalArgumentException(
                    "emotionalImpact must be in [" + MIN_IMPACT + ", " + MAX_IMPACT
                            + "] but was " + emotionalImpact);
        }
        this.day = day;
        this.emotionalImpact = emotionalImpact;
        this.relatedSurvivor = relatedSurvivor;
        this.sourceEventId = Objects.requireNonNull(sourceEventId, "sourceEventId");
    }

    public int day() {
        return day;
    }

    public int emotionalImpact() {
        return emotionalImpact;
    }

    public Optional<SurvivorId> relatedSurvivor() {
        return Optional.ofNullable(relatedSurvivor);
    }

    public EventId sourceEventId() {
        return sourceEventId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CharacterMemory m && day == m.day
                && emotionalImpact == m.emotionalImpact
                && Objects.equals(relatedSurvivor, m.relatedSurvivor)
                && sourceEventId.equals(m.sourceEventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(day, emotionalImpact, relatedSurvivor, sourceEventId);
    }
}
