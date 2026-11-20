package com.afterfall.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable set of trait strengths, each in the 0-100 {@link Scale}. Every {@link Trait} has a
 * value. This class only stores data; later behaviour code may read it as an input.
 */
public final class Personality {

    /** Midpoint of the scale, used by {@link #neutral()}. */
    public static final int NEUTRAL = 50;

    private final Map<Trait, Integer> values;

    /** Requires a value for every trait; the map is copied. */
    public Personality(Map<Trait, Integer> traitValues) {
        Objects.requireNonNull(traitValues, "traitValues");
        EnumMap<Trait, Integer> copy = new EnumMap<>(Trait.class);
        for (Trait trait : Trait.values()) {
            Integer value = traitValues.get(trait);
            if (value == null) {
                throw new IllegalArgumentException("Missing value for trait " + trait);
            }
            copy.put(trait, Scale.requireInRange(trait.name(), value));
        }
        this.values = copy;
    }

    public static Personality neutral() {
        EnumMap<Trait, Integer> all = new EnumMap<>(Trait.class);
        for (Trait trait : Trait.values()) {
            all.put(trait, NEUTRAL);
        }
        return new Personality(all);
    }

    public int value(Trait trait) {
        return values.get(Objects.requireNonNull(trait, "trait"));
    }

    /** Returns a new Personality with one trait changed; this instance is untouched. */
    public Personality with(Trait trait, int value) {
        Objects.requireNonNull(trait, "trait");
        EnumMap<Trait, Integer> copy = new EnumMap<>(values);
        copy.put(trait, value);
        return new Personality(copy);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Personality other && values.equals(other.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }
}
