package com.afterfall.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * A survivor's current need levels, each in the 0-100 {@link Scale}.
 * 100 means fully satisfied, 0 means completely unmet. No decay or thresholds live here.
 */
public final class Needs {

    private final Map<Need, Integer> levels = new EnumMap<>(Need.class);

    public Needs(int food, int water, int rest, int safety) {
        levels.put(Need.FOOD, Scale.requireInRange("food", food));
        levels.put(Need.WATER, Scale.requireInRange("water", water));
        levels.put(Need.REST, Scale.requireInRange("rest", rest));
        levels.put(Need.SAFETY, Scale.requireInRange("safety", safety));
    }

    public static Needs fullySatisfied() {
        return new Needs(Scale.MAX, Scale.MAX, Scale.MAX, Scale.MAX);
    }

    public int level(Need need) {
        return levels.get(Objects.requireNonNull(need, "need"));
    }

    /** Sets a level; rejects values outside the scale. */
    public void set(Need need, int level) {
        Objects.requireNonNull(need, "need");
        levels.put(need, Scale.requireInRange(need.name(), level));
    }

    /** Adds a (possibly negative) delta, clamped to the scale. Returns the new level. */
    public int adjust(Need need, int delta) {
        int updated = Scale.clamp((long) level(need) + delta);
        levels.put(need, updated);
        return updated;
    }
}
