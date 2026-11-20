package com.afterfall.domain;

/** The five professions in MUST scope. Others (Engineer, Builder, Teacher) are SHOULD. */
public enum ProfessionType {
    FARMER("Farmer"),
    SCAVENGER("Scavenger"),
    MEDIC("Medic"),
    GUARD("Guard"),
    GENERAL_WORKER("General Worker");

    private final String displayName;

    ProfessionType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
