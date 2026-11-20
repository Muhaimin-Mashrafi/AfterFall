package com.afterfall.domain;

/**
 * Configuration for the initial settlement survivor population.
 *
 * <p>In MUST scope, the population starts between {@value #MIN_POPULATION} and
 * {@value #MAX_POPULATION} survivors, defaulting to {@value #DEFAULT_POPULATION}.
 */
public record PopulationConfig(int size) {

    public static final int MIN_POPULATION = 20;
    public static final int MAX_POPULATION = 30;
    public static final int DEFAULT_POPULATION = 25;

    public PopulationConfig {
        if (size < MIN_POPULATION || size > MAX_POPULATION) {
            throw new IllegalArgumentException(
                    "Population size must be between " + MIN_POPULATION + " and " + MAX_POPULATION
                            + " inclusive, but was " + size);
        }
    }

    public static PopulationConfig defaultConfig() {
        return new PopulationConfig(DEFAULT_POPULATION);
    }
}
