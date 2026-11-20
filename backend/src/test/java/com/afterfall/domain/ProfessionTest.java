package com.afterfall.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProfessionTest {

    @Test
    void onlyTheFiveMustProfessionsExist() {
        assertEquals(
                List.of("Farmer", "Scavenger", "Medic", "Guard", "General Worker"),
                Arrays.stream(ProfessionType.values()).map(ProfessionType::displayName).toList());
    }

    @Test
    void constructorStoresTypeAndSkill() {
        Profession profession = new Profession(ProfessionType.MEDIC, 70);
        assertEquals(ProfessionType.MEDIC, profession.type());
        assertEquals(70, profession.skill());
        assertEquals("Medic", profession.displayName());
    }

    @Test
    void constructorValidates() {
        assertThrows(NullPointerException.class, () -> new Profession(null, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new Profession(ProfessionType.GUARD, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new Profession(ProfessionType.GUARD, 101));
    }

    @Test
    void withSkillReturnsNewInstanceOfSameType() {
        Profession original = new Profession(ProfessionType.FARMER, 30);
        Profession improved = original.withSkill(60);
        assertEquals(30, original.skill());
        assertEquals(60, improved.skill());
        assertEquals(ProfessionType.FARMER, improved.type());
        assertNotEquals(original, improved);
    }

    @Test
    void equalityIsByValue() {
        assertEquals(new Profession(ProfessionType.GUARD, 5), new Profession(ProfessionType.GUARD, 5));
    }

    @Test
    void professionIsARoleObjectNotASurvivorSubclass() {
        assertFalse(Survivor.class.isAssignableFrom(Profession.class));
        assertEquals(Object.class, Profession.class.getSuperclass());
        assertEquals(Object.class, Survivor.class.getSuperclass());
    }
}
