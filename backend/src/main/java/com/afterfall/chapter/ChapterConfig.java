package com.afterfall.chapter;

/**
 * Configurable thresholds and guardrail parameters for chapter transitions.
 */
public record ChapterConfig(
        int minDwellTicks,     // D_min: Minimum dwell before transition allowed (e.g. 8 ticks = 2 days)
        int softPressureTicks, // D_soft: Ticks after which pressure escalates (e.g. 24 ticks = 6 days)
        int hardBackstopTicks, // D_hard: Ticks at which transition is forced (e.g. 48 ticks = 12 days)
        int trustT1,           // T1: Trust threshold to enter Ch 2 (e.g. 50)
        int shortageN1,        // N1: Consecutive ticks shortage active for Ch 2 (e.g. 4)
        int evidenceCountE,    // E: Discovered evidence count to enter Ch 3 (e.g. 2)
        int exposureLevelX,    // X: Observer exposure to enter Ch 3 (e.g. 60)
        int moraleM1,          // M1: Morale threshold for Ch 3 (e.g. 40)
        int trustT2,           // T2: Trust threshold for Ch 3 (e.g. 40)
        int populationP,       // P: Active population threshold for Ch 4 (e.g. 15)
        int moraleM2,          // M2: Morale threshold for Ch 4 (e.g. 25)
        int trustT3,           // T3: Trust threshold for Ch 4 (e.g. 25)
        int criticalTicksN2    // N2: Ticks under M2 & T3 for Ch 4 (e.g. 6)
) {
    public static ChapterConfig defaultConfig() {
        return new ChapterConfig(
                8,   // D_min = 2 days
                60,  // D_soft = 15 days
                120, // D_hard = 30 days
                50,  // T1
                4,   // N1
                2,   // E
                60,  // X
                40,  // M1
                40,  // T2
                20,  // P
                25,  // M2
                25,  // T3
                6    // N2
        );
    }
}
