package com.afterfall.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PopulationConfigTest {

    @Test
    void defaultPopulationIsTwentyFive() {
        PopulationConfig config = PopulationConfig.defaultConfig();
        assertEquals(25, config.size());
    }

    @Test
    void validRangeBoundsAreInclusive() {
        PopulationConfig min = new PopulationConfig(20);
        assertEquals(20, min.size());

        PopulationConfig mid = new PopulationConfig(25);
        assertEquals(25, mid.size());

        PopulationConfig max = new PopulationConfig(30);
        assertEquals(30, max.size());
    }

    @Test
    void rejectsSizesBelowMinimumAndAboveMaximum() {
        assertThrows(IllegalArgumentException.class, () -> new PopulationConfig(19));
        assertThrows(IllegalArgumentException.class, () -> new PopulationConfig(0));
        assertThrows(IllegalArgumentException.class, () -> new PopulationConfig(-1));
        assertThrows(IllegalArgumentException.class, () -> new PopulationConfig(31));
        assertThrows(IllegalArgumentException.class, () -> new PopulationConfig(100));
    }
}
