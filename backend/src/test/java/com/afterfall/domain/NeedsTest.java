package com.afterfall.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class NeedsTest {

    @Test
    void constructorStoresEachLevel() {
        Needs needs = new Needs(10, 20, 30, 40);
        assertEquals(10, needs.level(Need.FOOD));
        assertEquals(20, needs.level(Need.WATER));
        assertEquals(30, needs.level(Need.REST));
        assertEquals(40, needs.level(Need.SAFETY));
    }

    @Test
    void fullySatisfiedIsAllMax() {
        Needs needs = Needs.fullySatisfied();
        for (Need need : Need.values()) {
            assertEquals(Scale.MAX, needs.level(need));
        }
    }

    @Test
    void constructorRejectsOutOfRangeValues() {
        assertThrows(IllegalArgumentException.class, () -> new Needs(-1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Needs(0, 101, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Needs(0, 0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Needs(0, 0, 0, 101));
    }

    @Test
    void setChangesOnlyThatNeedAndValidates() {
        Needs needs = new Needs(50, 50, 50, 50);
        needs.set(Need.WATER, 5);
        assertEquals(5, needs.level(Need.WATER));
        assertEquals(50, needs.level(Need.FOOD));
        assertThrows(IllegalArgumentException.class, () -> needs.set(Need.WATER, 150));
        assertEquals(5, needs.level(Need.WATER));
    }

    @Test
    void adjustClampsAndReturnsNewLevel() {
        Needs needs = new Needs(50, 50, 50, 50);
        assertEquals(60, needs.adjust(Need.FOOD, 10));
        assertEquals(100, needs.adjust(Need.FOOD, 1000));
        assertEquals(0, needs.adjust(Need.FOOD, -1000));
        assertEquals(0, needs.adjust(Need.FOOD, Integer.MIN_VALUE));
    }

    @Test
    void nullNeedIsRejected() {
        Needs needs = Needs.fullySatisfied();
        assertThrows(NullPointerException.class, () -> needs.level(null));
        assertThrows(NullPointerException.class, () -> needs.set(null, 1));
    }
}
