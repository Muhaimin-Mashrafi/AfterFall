package com.afterfall.domain;

import java.util.Objects;

/**
 * Root domain entity representing the current settlement/world state.
 *
 * <p>Uses composition to encapsulate the settlement. Does not contain simulation ticking,
 * clock advances, or engine coordination logic.
 */
public final class World {

    private final Settlement settlement;

    public World(Settlement settlement) {
        this.settlement = Objects.requireNonNull(settlement, "settlement");
    }

    public Settlement settlement() {
        return settlement;
    }
}
