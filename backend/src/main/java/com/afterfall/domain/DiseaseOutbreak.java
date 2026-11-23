package com.afterfall.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Disease Outbreak crisis: triggers when the number of ill survivors reaches a threshold.
 *
 * <p>Trigger: {@code illCount >= illThreshold}.
 * <p>Effects each active tick:
 * <ul>
 *   <li>Every alive survivor not already {@link HealthCondition#HEALTHY} loses
 *       {@value #VITALITY_PENALTY} vitality.</li>
 *   <li>Every alive {@link HealthCondition#HEALTHY} survivor whose safety need is below
 *       {@value #SPREAD_SAFETY_THRESHOLD} contracts the disease (condition set to ILL)
 *       and loses {@value #VITALITY_PENALTY} vitality — representing exposure spread.</li>
 * </ul>
 *
 * <p>Resolution: ill survivor count drops to zero.
 *
 * <p>Extends {@link Crisis} to demonstrate LSP and OCP alongside {@link FamineCrisis}.
 */
public final class DiseaseOutbreak extends Crisis {

    /** Number of ill survivors at or above which the outbreak triggers. */
    public static final int DEFAULT_ILL_THRESHOLD = 3;

    /** Safety-need level below which a healthy survivor may catch the disease. */
    static final int SPREAD_SAFETY_THRESHOLD = 50;

    /** Vitality penalty applied to affected survivors each tick. */
    static final int VITALITY_PENALTY = -8;

    private final int illThreshold;

    public DiseaseOutbreak() {
        this(DEFAULT_ILL_THRESHOLD);
    }

    public DiseaseOutbreak(int illThreshold) {
        super("Disease Outbreak");
        if (illThreshold <= 0) {
            throw new IllegalArgumentException("illThreshold must be positive");
        }
        this.illThreshold = illThreshold;
    }

    public int illThreshold() {
        return illThreshold;
    }

    @Override
    protected boolean checkTrigger(Settlement settlement, ResourceStock stock) {
        return countIll(settlement) >= illThreshold;
    }

    @Override
    protected boolean checkResolution(Settlement settlement, ResourceStock stock) {
        return countIll(settlement) == 0;
    }

    @Override
    protected List<CrisisEffect> collectEffects(Settlement settlement, ResourceStock stock) {
        List<CrisisEffect> effects = new ArrayList<>();

        for (Survivor survivor : settlement.survivors()) {
            if (!survivor.isAlive()) {
                continue;
            }

            HealthCondition condition = survivor.health().condition();

            if (condition != HealthCondition.HEALTHY) {
                // Already ill or injured: drain vitality
                effects.add(
                        CrisisEffect.forSurvivor(survivor.id())
                                .vitalityDelta(VITALITY_PENALTY)
                                .build()
                );
            } else {
                // Healthy survivors with low safety spread the disease
                int safety = survivor.needs().level(Need.SAFETY);
                if (safety < SPREAD_SAFETY_THRESHOLD) {
                    effects.add(
                            CrisisEffect.forSurvivor(survivor.id())
                                    .newCondition(HealthCondition.ILL)
                                    .vitalityDelta(VITALITY_PENALTY)
                                    .build()
                    );
                }
            }
        }

        return effects;
    }

    private static int countIll(Settlement settlement) {
        int count = 0;
        for (Survivor s : settlement.survivors()) {
            if (s.isAlive() && s.health().condition() == HealthCondition.ILL) {
                count++;
            }
        }
        return count;
    }
}
