package com.afterfall.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable proposal of changes that a {@link Crisis} requests to apply to one survivor.
 *
 * <p>All fields are optional deltas; a field absent (zero / empty) means no change.
 * The TickCoordinator applies these proposals after claim resolution, in survivor-id order.
 * This keeps crises stateless with respect to individual survivors and keeps the
 * TickCoordinator as the sole writer.
 */
public final class CrisisEffect {

    private final SurvivorId targetId;
    private final Map<Need, Integer> needDeltas;
    private final int vitalityDelta;
    private final Optional<HealthCondition> newCondition;

    private CrisisEffect(Builder b) {
        this.targetId = b.targetId;
        this.needDeltas = Map.copyOf(b.needDeltas);
        this.vitalityDelta = b.vitalityDelta;
        this.newCondition = Optional.ofNullable(b.newCondition);
    }

    public SurvivorId targetId() { return targetId; }
    public Map<Need, Integer> needDeltas() { return needDeltas; }
    public int vitalityDelta() { return vitalityDelta; }
    public Optional<HealthCondition> newCondition() { return newCondition; }

    public static Builder forSurvivor(SurvivorId targetId) {
        return new Builder(targetId);
    }

    public static final class Builder {
        private final SurvivorId targetId;
        private final Map<Need, Integer> needDeltas = new EnumMap<>(Need.class);
        private int vitalityDelta = 0;
        private HealthCondition newCondition = null;

        private Builder(SurvivorId targetId) {
            this.targetId = Objects.requireNonNull(targetId, "targetId");
        }

        public Builder needDelta(Need need, int delta) {
            needDeltas.put(Objects.requireNonNull(need, "need"), delta);
            return this;
        }

        public Builder vitalityDelta(int delta) {
            this.vitalityDelta = delta;
            return this;
        }

        public Builder newCondition(HealthCondition condition) {
            this.newCondition = condition;
            return this;
        }

        public CrisisEffect build() {
            return new CrisisEffect(this);
        }
    }
}
