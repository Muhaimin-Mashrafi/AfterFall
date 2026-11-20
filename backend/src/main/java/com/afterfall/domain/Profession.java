package com.afterfall.domain;

import java.util.Objects;

/**
 * A survivor's role, modelled as a role object that a Survivor <em>has</em> (composition),
 * not a Survivor subclass. Immutable: a survivor changes role by being given a new Profession.
 * Skill is in the 0-100 {@link Scale}.
 */
public final class Profession {

    private final ProfessionType type;
    private final int skill;

    public Profession(ProfessionType type, int skill) {
        this.type = Objects.requireNonNull(type, "type");
        this.skill = Scale.requireInRange("skill", skill);
    }

    public ProfessionType type() {
        return type;
    }

    public int skill() {
        return skill;
    }

    public String displayName() {
        return type.displayName();
    }

    public Profession withSkill(int newSkill) {
        return new Profession(type, newSkill);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Profession other && type == other.type && skill == other.skill;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, skill);
    }
}
