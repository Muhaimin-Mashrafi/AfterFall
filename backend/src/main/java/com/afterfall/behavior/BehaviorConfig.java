package com.afterfall.behavior;

/**
 * Centralized, immutable configuration of behavior evaluation thresholds and weights.
 * Eliminates arbitrary magic numbers in behavior strategies.
 */
public record BehaviorConfig(
        int criticalFoodThreshold,
        int hungerThreshold,
        int criticalRestThreshold,
        int fatigueThreshold,
        int criticalVitalityThreshold,
        int healthVitalityThreshold
) {

    public static final int DEFAULT_CRITICAL_FOOD = 25;
    public static final int DEFAULT_HUNGER = 50;
    public static final int DEFAULT_CRITICAL_REST = 20;
    public static final int DEFAULT_FATIGUE = 45;
    public static final int DEFAULT_CRITICAL_VITALITY = 30;
    public static final int DEFAULT_HEALTH_VITALITY = 50;

    public BehaviorConfig {
        if (criticalFoodThreshold < 0 || criticalFoodThreshold > 100
                || hungerThreshold < criticalFoodThreshold || hungerThreshold > 100
                || criticalRestThreshold < 0 || criticalRestThreshold > 100
                || fatigueThreshold < criticalRestThreshold || fatigueThreshold > 100
                || criticalVitalityThreshold < 0 || criticalVitalityThreshold > 100
                || healthVitalityThreshold < criticalVitalityThreshold || healthVitalityThreshold > 100) {
            throw new IllegalArgumentException("Invalid threshold range in BehaviorConfig");
        }
    }

    public static BehaviorConfig defaultConfig() {
        return new BehaviorConfig(
                DEFAULT_CRITICAL_FOOD,
                DEFAULT_HUNGER,
                DEFAULT_CRITICAL_REST,
                DEFAULT_FATIGUE,
                DEFAULT_CRITICAL_VITALITY,
                DEFAULT_HEALTH_VITALITY
        );
    }
}
