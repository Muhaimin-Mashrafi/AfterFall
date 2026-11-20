package com.afterfall.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScaleAndIdsTest {

    @Test
    void scaleIsZeroToOneHundred() {
        assertEquals(0, Scale.MIN);
        assertEquals(100, Scale.MAX);
    }

    @Test
    void requireInRangeAcceptsBoundsAndRejectsOutside() {
        assertEquals(0, Scale.requireInRange("x", 0));
        assertEquals(100, Scale.requireInRange("x", 100));
        assertThrows(IllegalArgumentException.class, () -> Scale.requireInRange("x", -1));
        assertThrows(IllegalArgumentException.class, () -> Scale.requireInRange("x", 101));
    }

    @Test
    void clampLimitsToRangeWithoutOverflow() {
        assertEquals(0, Scale.clamp(-5));
        assertEquals(100, Scale.clamp(500));
        assertEquals(42, Scale.clamp(42));
        assertEquals(100, Scale.clamp((long) Integer.MAX_VALUE + 10));
        assertEquals(0, Scale.clamp((long) Integer.MIN_VALUE - 10));
    }

    @Test
    void survivorIdMustBePositiveAndIsOrderedByValue() {
        assertThrows(IllegalArgumentException.class, () -> new SurvivorId(0));
        assertThrows(IllegalArgumentException.class, () -> new SurvivorId(-3));
        assertTrue(new SurvivorId(1).compareTo(new SurvivorId(2)) < 0);
        assertEquals(new SurvivorId(7), new SurvivorId(7));
    }

    @Test
    void eventIdMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new EventId(0));
        assertTrue(new EventId(1).compareTo(new EventId(5)) < 0);
        assertEquals(new EventId(9), new EventId(9));
    }
}
