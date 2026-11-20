package com.afterfall.domain;

import java.util.Objects;

/** Physical state: vitality in the 0-100 {@link Scale} plus a {@link HealthCondition}. */
public final class Health {

    private int vitality;
    private HealthCondition condition;

    public Health(int vitality, HealthCondition condition) {
        this.vitality = Scale.requireInRange("vitality", vitality);
        this.condition = Objects.requireNonNull(condition, "condition");
    }

    public static Health healthy() {
        return new Health(Scale.MAX, HealthCondition.HEALTHY);
    }

    public int vitality() {
        return vitality;
    }

    public HealthCondition condition() {
        return condition;
    }

    public void setVitality(int vitality) {
        this.vitality = Scale.requireInRange("vitality", vitality);
    }

    /** Adds a (possibly negative) delta, clamped to the scale. Returns the new vitality. */
    public int adjustVitality(int delta) {
        vitality = Scale.clamp((long) vitality + delta);
        return vitality;
    }

    public void setCondition(HealthCondition condition) {
        this.condition = Objects.requireNonNull(condition, "condition");
    }

    /** True when vitality has reached the bottom of the scale. */
    public boolean isDepleted() {
        return vitality == Scale.MIN;
    }
}
