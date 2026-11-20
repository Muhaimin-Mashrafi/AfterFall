package com.afterfall.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InitialPopulationTest {

    @Test
    void generatedUnregisteredSurvivorsHaveEmptyLocationWhileRegisteredHaveMatchingRoster() {
        List<Survivor> generated = InitialPopulation.generateSurvivors(25);
        assertEquals(25, generated.size());
        for (Survivor s : generated) {
            assertTrue(s.location().isEmpty(), "Unregistered survivor must initially have empty location");
        }

        Settlement settlement = InitialPopulation.createDefaultSettlement("Haven");
        for (Survivor s : settlement.survivors()) {
            RegionId loc = s.location().orElseThrow();
            assertEquals(loc, settlement.regionOf(s.id()).orElseThrow());
            assertTrue(settlement.survivorsIn(loc).contains(s.id()));
        }
    }

    @Test
    void generatesDefaultPopulationOfTwentyFive() {
        Settlement settlement = InitialPopulation.createDefaultSettlement("Haven");
        assertEquals(25, settlement.population());

        World world = InitialPopulation.createDefaultWorld("Haven");
        assertEquals(25, world.settlement().population());
    }

    @Test
    void generatesAllValidPopulationCountsDeterministically() {
        for (int count = PopulationConfig.MIN_POPULATION; count <= PopulationConfig.MAX_POPULATION; count++) {
            PopulationConfig config = new PopulationConfig(count);
            Settlement settlement = InitialPopulation.createSettlement("Haven", config);
            assertEquals(count, settlement.population());

            // Check uniqueness of survivor IDs
            Set<SurvivorId> ids = new HashSet<>();
            for (Survivor s : settlement.survivors()) {
                assertTrue(ids.add(s.id()), "Duplicate survivor ID: " + s.id());
                assertTrue(s.location().isPresent());
            }

            // Check that every survivor belongs to exactly one region
            for (SurvivorId id : ids) {
                RegionId rId = settlement.regionOf(id).orElseThrow();
                assertTrue(settlement.survivorsIn(rId).contains(id));
            }
        }
    }

    @Test
    void distributesSurvivorsEvenlyAcrossFourRegions() {
        Settlement settlement = InitialPopulation.createDefaultSettlement("Haven");
        assertEquals(4, settlement.regions().size());

        int r1 = settlement.survivorsIn(RegionId.REGION_1).size();
        int r2 = settlement.survivorsIn(RegionId.REGION_2).size();
        int r3 = settlement.survivorsIn(RegionId.REGION_3).size();
        int r4 = settlement.survivorsIn(RegionId.REGION_4).size();

        // 25 divided by 4: three regions get 6, one gets 7
        assertEquals(25, r1 + r2 + r3 + r4);
        assertTrue(r1 >= 6 && r1 <= 7);
        assertTrue(r2 >= 6 && r2 <= 7);
        assertTrue(r3 >= 6 && r3 <= 7);
        assertTrue(r4 >= 6 && r4 <= 7);
    }

    @Test
    void usesOnlyFiveMustProfessions() {
        List<Survivor> survivors = InitialPopulation.generateSurvivors(25);
        Set<ProfessionType> used = new HashSet<>();
        for (Survivor s : survivors) {
            used.add(s.profession().type());
        }

        assertEquals(Set.of(
                ProfessionType.FARMER,
                ProfessionType.SCAVENGER,
                ProfessionType.MEDIC,
                ProfessionType.GUARD,
                ProfessionType.GENERAL_WORKER
        ), used);
    }

    @Test
    void generationIsDeterministicAcrossRepeatedCalls() {
        List<Survivor> run1 = InitialPopulation.generateSurvivors(25);
        List<Survivor> run2 = InitialPopulation.generateSurvivors(25);

        assertEquals(run1.size(), run2.size());
        for (int i = 0; i < run1.size(); i++) {
            Survivor s1 = run1.get(i);
            Survivor s2 = run2.get(i);
            assertEquals(s1.id(), s2.id());
            assertEquals(s1.name(), s2.name());
            assertEquals(s1.profession(), s2.profession());
            assertEquals(s1.personality(), s2.personality());
            assertEquals(s1.location(), s2.location());
        }
    }
}
