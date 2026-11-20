package com.afterfall.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HealthTest {

    @Test
    void healthyFactoryGivesFullVitalityAndHealthyCondition() {
        Health health = Health.healthy();
        assertEquals(100, health.vitality());
        assertEquals(HealthCondition.HEALTHY, health.condition());
        assertFalse(health.isDepleted());
    }

    @Test
    void constructorValidates() {
        assertThrows(IllegalArgumentException.class, () -> new Health(-1, HealthCondition.ILL));
        assertThrows(IllegalArgumentException.class, () -> new Health(101, HealthCondition.ILL));
        assertThrows(NullPointerException.class, () -> new Health(50, null));
    }

    @Test
    void adjustVitalityClampsAtBothEnds() {
        Health health = new Health(50, HealthCondition.INJURED);
        assertEquals(30, health.adjustVitality(-20));
        assertEquals(100, health.adjustVitality(500));
        assertEquals(0, health.adjustVitality(-500));
        assertTrue(health.isDepleted());
    }

    @Test
    void setVitalityRejectsOutOfRangeAndKeepsOldValue() {
        Health health = new Health(40, HealthCondition.HEALTHY);
        assertThrows(IllegalArgumentException.class, () -> health.setVitality(101));
        assertEquals(40, health.vitality());
        health.setVitality(0);
        assertTrue(health.isDepleted());
    }

    @Test
    void conditionCanChangeButNotBeNull() {
        Health health = Health.healthy();
        health.setCondition(HealthCondition.ILL);
        assertEquals(HealthCondition.ILL, health.condition());
        assertThrows(NullPointerException.class, () -> health.setCondition(null));
        assertEquals(HealthCondition.ILL, health.condition());
    }
}
