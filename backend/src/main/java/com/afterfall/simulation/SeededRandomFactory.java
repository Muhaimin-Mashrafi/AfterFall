package com.afterfall.simulation;

import com.afterfall.domain.RegionId;
import java.util.Objects;
import java.util.SplittableRandom;

/**
 * Factory for creating deterministic, independent {@link SplittableRandom} instances
 * for each processing region and tick.
 *
 * <p>Uses a stable 64-bit mixing algorithm (SplitMix64) to ensure that the RNG stream
 * depends strictly on {@code (runSeed, regionId, tick)}.
 * No system clock, memory address, ThreadLocalRandom, or unseeded Random is used.
 */
public final class SeededRandomFactory {

    private final long runSeed;

    public SeededRandomFactory(long runSeed) {
        this.runSeed = runSeed;
    }

    public long runSeed() {
        return runSeed;
    }

    /**
     * Derives an independent {@link SplittableRandom} for the specified region and tick.
     *
     * @param regionId the region identifier
     * @param tick     the simulation tick number
     * @return a new deterministic {@link SplittableRandom}
     */
    public SplittableRandom createForRegion(RegionId regionId, long tick) {
        Objects.requireNonNull(regionId, "regionId");
        long combined = mix64(runSeed) ^ mix64((long) regionId.value() * 0x9E3779B97F4A7C15L + tick * 0xBF58476D1CE4E5B9L);
        return new SplittableRandom(combined);
    }

    /**
     * Staffed/SplitMix64 64-bit mixer for avalanche properties.
     */
    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
