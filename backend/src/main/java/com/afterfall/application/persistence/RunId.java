package com.afterfall.application.persistence;

import java.util.Objects;

/**
 * Strongly-typed value object representing a unique simulation run execution.
 */
public record RunId(String value) {
    public RunId {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static RunId of(String value) {
        return new RunId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
