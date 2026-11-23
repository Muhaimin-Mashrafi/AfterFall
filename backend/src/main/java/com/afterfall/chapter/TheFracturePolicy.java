package com.afterfall.chapter;

import java.util.Optional;

/**
 * Chapter II: The Fracture -> Chapter III: The Revelation.
 *
 * <p>Trigger conditions:
 * <ul>
 *   <li>Discovered Observer evidence count >= E.</li>
 *   <li>Observer exposure level >= X.</li>
 *   <li>Average morale below M1 with average trust below T2.</li>
 * </ul>
 */
public final class TheFracturePolicy implements ChapterPolicy {

    @Override
    public ChapterType sourceChapter() {
        return ChapterType.CHAPTER_2_THE_FRACTURE;
    }

    @Override
    public Optional<String> evaluateTransition(StoryState state, ChapterConfig config) {
        if (state.discoveredEvidenceCount() >= config.evidenceCountE()) {
            return Optional.of("Survivors discovered " + state.discoveredEvidenceCount() + " pieces of conflicting Observer evidence");
        }
        if (state.observerExposureLevel() >= config.exposureLevelX()) {
            return Optional.of("Observer manipulation exposure reached level " + state.observerExposureLevel());
        }
        if (state.averageMorale() < config.moraleM1() && state.averageTrust() < config.trustT2()) {
            return Optional.of("Widespread despair took hold (morale " + state.averageMorale() + " < " + config.moraleM1() + ", trust " + state.averageTrust() + " < " + config.trustT2() + ")");
        }
        return Optional.empty();
    }
}
