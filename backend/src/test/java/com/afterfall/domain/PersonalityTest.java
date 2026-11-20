package com.afterfall.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PersonalityTest {

    private static Map<Trait, Integer> all(int value) {
        Map<Trait, Integer> map = new EnumMap<>(Trait.class);
        for (Trait trait : Trait.values()) {
            map.put(trait, value);
        }
        return map;
    }

    @Test
    void hasTheEightBlueprintTraits() {
        assertEquals(8, Trait.values().length);
    }

    @Test
    void neutralGivesMidpointForEveryTrait() {
        Personality personality = Personality.neutral();
        for (Trait trait : Trait.values()) {
            assertEquals(Personality.NEUTRAL, personality.value(trait));
        }
    }

    @Test
    void constructorRequiresEveryTrait() {
        Map<Trait, Integer> incomplete = all(10);
        incomplete.remove(Trait.FEARFUL);
        assertThrows(IllegalArgumentException.class, () -> new Personality(incomplete));
    }

    @Test
    void constructorRejectsOutOfRangeValues() {
        Map<Trait, Integer> bad = all(10);
        bad.put(Trait.AGGRESSIVE, 101);
        assertThrows(IllegalArgumentException.class, () -> new Personality(bad));
        bad.put(Trait.AGGRESSIVE, -1);
        assertThrows(IllegalArgumentException.class, () -> new Personality(bad));
    }

    @Test
    void inputMapIsCopiedSoLaterChangesDoNotLeakIn() {
        Map<Trait, Integer> source = all(20);
        Personality personality = new Personality(source);
        source.put(Trait.FEARFUL, 99);
        assertEquals(20, personality.value(Trait.FEARFUL));
    }

    @Test
    void withReturnsNewInstanceAndLeavesOriginalUnchanged() {
        Personality original = Personality.neutral();
        Personality changed = original.with(Trait.COMPASSIONATE, 90);
        assertEquals(90, changed.value(Trait.COMPASSIONATE));
        assertEquals(Personality.NEUTRAL, original.value(Trait.COMPASSIONATE));
        assertNotEquals(original, changed);
        assertThrows(IllegalArgumentException.class, () -> original.with(Trait.FEARFUL, 200));
    }

    @Test
    void equalityIsByValue() {
        assertEquals(Personality.neutral(), Personality.neutral());
        assertEquals(Personality.neutral().hashCode(), Personality.neutral().hashCode());
    }
}
