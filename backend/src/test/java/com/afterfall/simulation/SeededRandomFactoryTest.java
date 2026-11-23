package com.afterfall.simulation;

import com.afterfall.domain.RegionId;
import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.*;

class SeededRandomFactoryTest {

    @Test
    void testIdenticalInputsProduceIdenticalSequence() {
        SeededRandomFactory factory = new SeededRandomFactory(12345L);
        SplittableRandom rng1 = factory.createForRegion(RegionId.REGION_1, 10L);
        SplittableRandom rng2 = factory.createForRegion(RegionId.REGION_1, 10L);

        for (int i = 0; i < 50; i++) {
            assertEquals(rng1.nextInt(), rng2.nextInt(), "Random outputs should match exactly for same runSeed, region, and tick");
        }
    }

    @Test
    void testDifferentRegionsProduceDifferentSequences() {
        SeededRandomFactory factory = new SeededRandomFactory(12345L);
        SplittableRandom rngRegion1 = factory.createForRegion(RegionId.REGION_1, 1L);
        SplittableRandom rngRegion2 = factory.createForRegion(RegionId.REGION_2, 1L);

        boolean foundDifference = false;
        for (int i = 0; i < 10; i++) {
            if (rngRegion1.nextInt() != rngRegion2.nextInt()) {
                foundDifference = true;
                break;
            }
        }
        assertTrue(foundDifference, "Different regions on the same tick should produce different random sequences");
    }

    @Test
    void testDifferentTicksProduceDifferentSequences() {
        SeededRandomFactory factory = new SeededRandomFactory(12345L);
        SplittableRandom rngTick1 = factory.createForRegion(RegionId.REGION_1, 1L);
        SplittableRandom rngTick2 = factory.createForRegion(RegionId.REGION_1, 2L);

        boolean foundDifference = false;
        for (int i = 0; i < 10; i++) {
            if (rngTick1.nextInt() != rngTick2.nextInt()) {
                foundDifference = true;
                break;
            }
        }
        assertTrue(foundDifference, "Different ticks for the same region should produce different random sequences");
    }

    @Test
    void testDeterministicSequenceConsistencyAcrossInstances() {
        SeededRandomFactory factoryA = new SeededRandomFactory(99999L);
        SeededRandomFactory factoryB = new SeededRandomFactory(99999L);

        for (int r = 1; r <= 4; r++) {
            RegionId regionId = new RegionId(r);
            for (long tick = 1; tick <= 5; tick++) {
                SplittableRandom rngA = factoryA.createForRegion(regionId, tick);
                SplittableRandom rngB = factoryB.createForRegion(regionId, tick);
                for (int i = 0; i < 20; i++) {
                    assertEquals(rngA.nextLong(), rngB.nextLong(),
                            "Separate factory instances with same runSeed must produce identical sequences");
                }
            }
        }
    }
}
