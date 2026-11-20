package com.afterfall.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WorldTest {

    @Test
    void encapsulatesSettlementAndGuardsInvariants() {
        Settlement settlement = new Settlement("Haven");
        World world = new World(settlement);

        assertNotNull(world.settlement());
        assertEquals("Haven", world.settlement().name());
        assertThrows(NullPointerException.class, () -> new World(null));
    }
}
