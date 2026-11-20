package com.afterfall.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CharacterMemoryTest {

    private static final EventId EVENT = new EventId(12);

    @Test
    void storesAllFields() {
        CharacterMemory memory = new CharacterMemory(5, -40, new SurvivorId(3), EVENT);
        assertEquals(5, memory.day());
        assertEquals(-40, memory.emotionalImpact());
        assertEquals(new SurvivorId(3), memory.relatedSurvivor().orElseThrow());
        assertEquals(EVENT, memory.sourceEventId());
    }

    @Test
    void relatedSurvivorIsOptional() {
        CharacterMemory memory = new CharacterMemory(0, 10, null, EVENT);
        assertTrue(memory.relatedSurvivor().isEmpty());
    }

    @Test
    void impactBoundsAreInclusiveAndZeroIsAllowed() {
        new CharacterMemory(0, -100, null, EVENT);
        new CharacterMemory(0, 100, null, EVENT);
        CharacterMemory neutral = new CharacterMemory(0, 0, null, EVENT);
        assertEquals(0, neutral.emotionalImpact());
        assertThrows(IllegalArgumentException.class,
                () -> new CharacterMemory(0, 101, null, EVENT));
        assertThrows(IllegalArgumentException.class,
                () -> new CharacterMemory(0, -101, null, EVENT));
    }

    @Test
    void dayMustNotBeNegativeAndSourceEventIsRequired() {
        assertThrows(IllegalArgumentException.class,
                () -> new CharacterMemory(-1, 10, null, EVENT));
        assertThrows(NullPointerException.class, () -> new CharacterMemory(1, 10, null, null));
    }

    @Test
    void equalityIsByValue() {
        CharacterMemory a = new CharacterMemory(1, 10, new SurvivorId(2), EVENT);
        CharacterMemory b = new CharacterMemory(1, 10, new SurvivorId(2), EVENT);
        CharacterMemory c = new CharacterMemory(1, 10, null, EVENT);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
