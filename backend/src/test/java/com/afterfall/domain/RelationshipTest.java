package com.afterfall.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RelationshipTest {

    private static final SurvivorId TARGET = new SurvivorId(2);
    private static final EventId EVENT = new EventId(1);

    private static Relationship sample() {
        return new Relationship(TARGET, RelationshipType.ACQUAINTANCE, 50, 40, 10);
    }

    @Test
    void constructorStoresAllFieldsWithEmptyHistory() {
        Relationship relationship = sample();
        assertEquals(TARGET, relationship.target());
        assertEquals(RelationshipType.ACQUAINTANCE, relationship.type());
        assertEquals(50, relationship.trust());
        assertEquals(40, relationship.affection());
        assertEquals(10, relationship.hostility());
        assertTrue(relationship.history().isEmpty());
    }

    @Test
    void constructorValidates() {
        assertThrows(NullPointerException.class,
                () -> new Relationship(null, RelationshipType.FRIEND, 1, 1, 1));
        assertThrows(NullPointerException.class, () -> new Relationship(TARGET, null, 1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Relationship(TARGET, RelationshipType.FRIEND, -1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Relationship(TARGET, RelationshipType.FRIEND, 1, 101, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Relationship(TARGET, RelationshipType.FRIEND, 1, 1, 101));
    }

    @Test
    void adjustAppliesDeltasAndRecordsHistory() {
        Relationship relationship = sample();
        relationship.adjust(3, -20, 5, 15, EVENT);
        assertEquals(30, relationship.trust());
        assertEquals(45, relationship.affection());
        assertEquals(25, relationship.hostility());
        assertEquals(1, relationship.history().size());
        RelationshipEntry entry = relationship.history().get(0);
        assertEquals(3, entry.day());
        assertEquals(30, entry.trust());
        assertEquals(45, entry.affection());
        assertEquals(25, entry.hostility());
        assertEquals(RelationshipType.ACQUAINTANCE, entry.type());
        assertEquals(EVENT, entry.sourceEventId());
    }

    @Test
    void adjustClampsToScale() {
        Relationship relationship = sample();
        relationship.adjust(1, 1000, -1000, 1000, EVENT);
        assertEquals(100, relationship.trust());
        assertEquals(0, relationship.affection());
        assertEquals(100, relationship.hostility());
    }

    @Test
    void changeTypeUpdatesTypeAndRecordsHistory() {
        Relationship relationship = sample();
        relationship.changeType(4, RelationshipType.RIVAL, EVENT);
        assertEquals(RelationshipType.RIVAL, relationship.type());
        assertEquals(RelationshipType.RIVAL, relationship.history().get(0).type());
        assertEquals(50, relationship.trust());
    }

    @Test
    void invalidChangesAreRejectedAndNotRecorded() {
        Relationship relationship = sample();
        assertThrows(IllegalArgumentException.class, () -> relationship.adjust(-1, 1, 1, 1, EVENT));
        assertThrows(NullPointerException.class, () -> relationship.adjust(1, 1, 1, 1, null));
        assertThrows(NullPointerException.class, () -> relationship.changeType(1, null, EVENT));
        assertThrows(IllegalArgumentException.class,
                () -> relationship.changeType(-1, RelationshipType.FRIEND, EVENT));
        assertEquals(50, relationship.trust());
        assertTrue(relationship.history().isEmpty());
    }

    @Test
    void rejectsDayEarlierThanLastHistoryEntryAndPreservesState() {
        Relationship relationship = sample();
        relationship.adjust(5, 10, 5, -5, EVENT);
        assertEquals(1, relationship.history().size());
        assertEquals(60, relationship.trust());

        // Adjust with earlier day rejected without mutation
        assertThrows(IllegalArgumentException.class,
                () -> relationship.adjust(4, 5, 5, 5, new EventId(99)));
        assertEquals(60, relationship.trust());
        assertEquals(1, relationship.history().size());

        // ChangeType with earlier day rejected without mutation
        assertThrows(IllegalArgumentException.class,
                () -> relationship.changeType(3, RelationshipType.FRIEND, new EventId(100)));
        assertEquals(RelationshipType.ACQUAINTANCE, relationship.type());
        assertEquals(1, relationship.history().size());

        // Equal day is allowed
        relationship.adjust(5, 5, 0, 0, new EventId(101));
        assertEquals(65, relationship.trust());
        assertEquals(2, relationship.history().size());

        // Later day is allowed
        relationship.changeType(6, RelationshipType.FRIEND, new EventId(102));
        assertEquals(RelationshipType.FRIEND, relationship.type());
        assertEquals(3, relationship.history().size());
    }

    @Test
    void historyIsBoundedKeepingNewestEntries() {
        Relationship relationship = sample();
        int total = Relationship.MAX_HISTORY + 5;
        for (int day = 1; day <= total; day++) {
            relationship.adjust(day, 0, 0, 0, new EventId(day));
        }
        List<RelationshipEntry> history = relationship.history();
        assertEquals(Relationship.MAX_HISTORY, history.size());
        assertEquals(6, history.get(0).day());
        assertEquals(total, history.get(history.size() - 1).day());
    }

    @Test
    void historyIsAnUnmodifiableCopy() {
        Relationship relationship = sample();
        relationship.adjust(1, 1, 1, 1, EVENT);
        List<RelationshipEntry> history = relationship.history();
        assertThrows(UnsupportedOperationException.class, () -> history.clear());
        relationship.adjust(2, 1, 1, 1, EVENT);
        assertEquals(1, history.size());
        assertEquals(2, relationship.history().size());
    }

    @Test
    void entryValidatesItsFields() {
        assertThrows(IllegalArgumentException.class,
                () -> new RelationshipEntry(-1, RelationshipType.FRIEND, 1, 1, 1, EVENT));
        assertThrows(IllegalArgumentException.class,
                () -> new RelationshipEntry(1, RelationshipType.FRIEND, 101, 1, 1, EVENT));
        assertThrows(NullPointerException.class,
                () -> new RelationshipEntry(1, RelationshipType.FRIEND, 1, 1, 1, null));
    }
}
