package com.afterfall.choice;

import com.afterfall.domain.Need;
import com.afterfall.domain.ResourceType;
import com.afterfall.domain.SurvivorId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable specification of state adjustments resulting from a chosen option.
 *
 * <p>Can modify settlement stock (food, water, medicine), survivor needs,
 * interpersonal trust, and emit narrative event descriptions.
 */
public record ChoiceConsequence(
        Map<ResourceType, Integer> resourceDeltas,
        Map<Need, Integer> generalNeedDeltas,
        int trustDelta,
        List<SurvivorId> targetSurvivors,
        String summary,
        int severity
) {
    public ChoiceConsequence {
        resourceDeltas = Map.copyOf(Objects.requireNonNull(resourceDeltas, "resourceDeltas"));
        generalNeedDeltas = Map.copyOf(Objects.requireNonNull(generalNeedDeltas, "generalNeedDeltas"));
        targetSurvivors = List.copyOf(Objects.requireNonNull(targetSurvivors, "targetSurvivors"));
        Objects.requireNonNull(summary, "summary");
    }

    public static Builder builder(String summary) {
        return new Builder(summary);
    }

    public static final class Builder {
        private final String summary;
        private Map<ResourceType, Integer> resourceDeltas = Map.of();
        private Map<Need, Integer> generalNeedDeltas = Map.of();
        private int trustDelta = 0;
        private List<SurvivorId> targetSurvivors = List.of();
        private int severity = 2; // Default SIGNIFICANT

        private Builder(String summary) {
            this.summary = Objects.requireNonNull(summary, "summary");
        }

        public Builder resourceDelta(ResourceType type, int amount) {
            Map<ResourceType, Integer> copy = new java.util.EnumMap<>(ResourceType.class);
            copy.putAll(this.resourceDeltas);
            copy.put(type, amount);
            this.resourceDeltas = copy;
            return this;
        }

        public Builder needDelta(Need need, int amount) {
            Map<Need, Integer> copy = new java.util.EnumMap<>(Need.class);
            copy.putAll(this.generalNeedDeltas);
            copy.put(need, amount);
            this.generalNeedDeltas = copy;
            return this;
        }

        public Builder trustDelta(int trustDelta) {
            this.trustDelta = trustDelta;
            return this;
        }

        public Builder targetSurvivors(List<SurvivorId> survivors) {
            this.targetSurvivors = survivors;
            return this;
        }

        public Builder severity(int severity) {
            this.severity = severity;
            return this;
        }

        public ChoiceConsequence build() {
            return new ChoiceConsequence(resourceDeltas, generalNeedDeltas, trustDelta, targetSurvivors, summary, severity);
        }
    }
}
