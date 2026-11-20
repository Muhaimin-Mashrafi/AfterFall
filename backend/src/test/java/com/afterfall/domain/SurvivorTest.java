package com.afterfall.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

class SurvivorTest {

    private static final EventId EVENT = new EventId(1);

    private static Survivor survivor(int id, String name) {
        return new Survivor(new SurvivorId(id), name, Personality.neutral(),
                new Profession(ProfessionType.FARMER, 40), Needs.fullySatisfied(),
                Health.healthy(), new Inventory(10));
    }

    @Test
    void constructorComposesAllComponents() {
        Personality personality = Personality.neutral().with(Trait.RESILIENT, 80);
        Profession profession = new Profession(ProfessionType.MEDIC, 60);
        Needs needs = Needs.fullySatisfied();
        Health health = Health.healthy();
        Inventory inventory = new Inventory(8);
        Survivor survivor = new Survivor(new SurvivorId(1), "Mara", personality, profession,
                needs, health, inventory);

        assertEquals(new SurvivorId(1), survivor.id());
        assertEquals("Mara", survivor.name());
        assertSame(personality, survivor.personality());
        assertSame(profession, survivor.profession());
        assertSame(needs, survivor.needs());
        assertSame(health, survivor.health());
        assertSame(inventory, survivor.inventory());
        assertEquals(SurvivorStatus.ALIVE, survivor.status());
        assertTrue(survivor.isAlive());
        assertTrue(survivor.relationships().isEmpty());
        assertTrue(survivor.memories().isEmpty());
    }

    @Test
    void nameIsStrippedAndMustNotBeBlank() {
        assertEquals("Mara", survivor(1, "  Mara ").name());
        assertThrows(IllegalArgumentException.class, () -> survivor(1, "  "));
        assertThrows(IllegalArgumentException.class, () -> survivor(1, null));
    }

    @Test
    void constructorRejectsNullComponents() {
        SurvivorId id = new SurvivorId(1);
        Profession profession = new Profession(ProfessionType.GUARD, 1);
        assertThrows(NullPointerException.class, () -> new Survivor(null, "A",
                Personality.neutral(), profession, Needs.fullySatisfied(), Health.healthy(),
                new Inventory(1)));
        assertThrows(NullPointerException.class, () -> new Survivor(id, "A", null, profession,
                Needs.fullySatisfied(), Health.healthy(), new Inventory(1)));
        assertThrows(NullPointerException.class, () -> new Survivor(id, "A",
                Personality.neutral(), null, Needs.fullySatisfied(), Health.healthy(),
                new Inventory(1)));
        assertThrows(NullPointerException.class, () -> new Survivor(id, "A",
                Personality.neutral(), profession, null, Health.healthy(), new Inventory(1)));
        assertThrows(NullPointerException.class, () -> new Survivor(id, "A",
                Personality.neutral(), profession, Needs.fullySatisfied(), null,
                new Inventory(1)));
        assertThrows(NullPointerException.class, () -> new Survivor(id, "A",
                Personality.neutral(), profession, Needs.fullySatisfied(), Health.healthy(),
                null));
    }

    @Test
    void componentStateChangesAreVisibleThroughTheSurvivor() {
        Survivor survivor = survivor(1, "Mara");
        survivor.needs().adjust(Need.FOOD, -30);
        survivor.health().setCondition(HealthCondition.ILL);
        survivor.inventory().add(ItemType.FOOD, 3);
        assertEquals(70, survivor.needs().level(Need.FOOD));
        assertEquals(HealthCondition.ILL, survivor.health().condition());
        assertEquals(3, survivor.inventory().quantity(ItemType.FOOD));
    }

    @Test
    void professionCanBeChangedWithoutChangingTheSurvivorType() {
        Survivor survivor = survivor(1, "Mara");
        Class<?> before = survivor.getClass();
        survivor.changeProfession(new Profession(ProfessionType.GUARD, 55));
        assertEquals(ProfessionType.GUARD, survivor.profession().type());
        assertEquals(before, survivor.getClass());
        assertThrows(NullPointerException.class, () -> survivor.changeProfession(null));
        assertEquals(ProfessionType.GUARD, survivor.profession().type());
    }

    @Test
    void markDeadIsPermanent() {
        Survivor survivor = survivor(1, "Mara");
        survivor.markDead();
        assertEquals(SurvivorStatus.DEAD, survivor.status());
        assertFalse(survivor.isAlive());
        survivor.markDead();
        assertEquals(SurvivorStatus.DEAD, survivor.status());
    }

    @Test
    void relationshipsAreStoredByTargetAndOrderedById() {
        Survivor survivor = survivor(1, "Mara");
        survivor.addRelationship(new Relationship(new SurvivorId(9), RelationshipType.FRIEND, 1, 1, 1));
        survivor.addRelationship(new Relationship(new SurvivorId(3), RelationshipType.RIVAL, 1, 1, 1));
        survivor.addRelationship(new Relationship(new SurvivorId(5), RelationshipType.FAMILY, 1, 1, 1));

        assertEquals(List.of(new SurvivorId(3), new SurvivorId(5), new SurvivorId(9)),
                survivor.relationships().stream().map(Relationship::target).toList());
        assertEquals(RelationshipType.RIVAL,
                survivor.relationshipWith(new SurvivorId(3)).orElseThrow().type());
        assertTrue(survivor.relationshipWith(new SurvivorId(4)).isEmpty());
    }

    @Test
    void relationshipRulesAreEnforced() {
        Survivor survivor = survivor(1, "Mara");
        assertThrows(IllegalArgumentException.class, () -> survivor.addRelationship(
                new Relationship(new SurvivorId(1), RelationshipType.FAMILY, 1, 1, 1)));
        survivor.addRelationship(new Relationship(new SurvivorId(2), RelationshipType.FRIEND, 1, 1, 1));
        assertThrows(IllegalStateException.class, () -> survivor.addRelationship(
                new Relationship(new SurvivorId(2), RelationshipType.RIVAL, 1, 1, 1)));
        assertThrows(NullPointerException.class, () -> survivor.addRelationship(null));
        assertEquals(1, survivor.relationships().size());
    }

    @Test
    void relationshipViewIsUnmodifiableButRelationshipObjectsAreLive() {
        Survivor survivor = survivor(1, "Mara");
        Relationship relationship = new Relationship(new SurvivorId(2), RelationshipType.FRIEND, 50, 50, 0);
        survivor.addRelationship(relationship);
        assertThrows(UnsupportedOperationException.class, () -> survivor.relationships().clear());
        relationship.adjust(2, -10, 0, 0, EVENT);
        assertEquals(40, survivor.relationshipWith(new SurvivorId(2)).orElseThrow().trust());
    }

    @Test
    void memoriesAreRememberedInOrderAndReturnedAsCopy() {
        Survivor survivor = survivor(1, "Mara");
        CharacterMemory first = new CharacterMemory(1, -50, new SurvivorId(2), new EventId(10));
        CharacterMemory second = new CharacterMemory(4, 30, null, new EventId(11));
        survivor.remember(first);
        survivor.remember(second);

        List<CharacterMemory> memories = survivor.memories();
        assertEquals(List.of(first, second), memories);
        assertThrows(UnsupportedOperationException.class, () -> memories.add(first));
        survivor.remember(second);
        assertEquals(2, memories.size());
        assertEquals(3, survivor.memories().size());
    }

    @Test
    void rejectsMemoryEarlierThanLastStoredMemoryAndLeavesCollectionUnchanged() {
        Survivor survivor = survivor(1, "Mara");
        CharacterMemory m1 = new CharacterMemory(3, 10, null, EVENT);
        survivor.remember(m1);
        assertEquals(1, survivor.memories().size());

        // Earlier day rejected, leaving collection unchanged
        CharacterMemory earlier = new CharacterMemory(2, -15, null, new EventId(101));
        assertThrows(IllegalArgumentException.class, () -> survivor.remember(earlier));
        assertEquals(1, survivor.memories().size());
        assertEquals(m1, survivor.memories().get(0));

        // Equal day allowed
        CharacterMemory equalDay = new CharacterMemory(3, -5, null, new EventId(102));
        survivor.remember(equalDay);
        assertEquals(2, survivor.memories().size());
        assertEquals(equalDay, survivor.memories().get(1));

        // Later day allowed
        CharacterMemory laterDay = new CharacterMemory(4, 25, null, new EventId(103));
        survivor.remember(laterDay);
        assertEquals(3, survivor.memories().size());
        assertEquals(laterDay, survivor.memories().get(2));
    }

    @Test
    void memoryRulesAreEnforced() {
        Survivor survivor = survivor(1, "Mara");
        assertThrows(NullPointerException.class, () -> survivor.remember(null));
        assertThrows(IllegalArgumentException.class, () -> survivor.remember(
                new CharacterMemory(1, 10, new SurvivorId(1), EVENT)));
        assertTrue(survivor.memories().isEmpty());
    }

    @Test
    void identityIsTheSurvivorId() {
        assertEquals(survivor(1, "Mara"), survivor(1, "Different name"));
        assertEquals(survivor(1, "Mara").hashCode(), survivor(1, "Other").hashCode());
        assertNotEquals(survivor(1, "Mara"), survivor(2, "Mara"));
    }

    @Test
    void fieldsAreEncapsulatedAndSurvivorHasNoSubclasses() {
        for (var field : Survivor.class.getDeclaredFields()) {
            assertTrue(Modifier.isPrivate(field.getModifiers()),
                    field.getName() + " must be private");
        }
        assertTrue(Modifier.isFinal(Survivor.class.getModifiers()));
    }
}
