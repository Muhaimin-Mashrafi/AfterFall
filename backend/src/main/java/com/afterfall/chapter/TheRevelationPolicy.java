package com.afterfall.chapter;

import java.util.Optional;

/**
 * Chapter III: The Revelation -> Chapter IV: The Last Recording.
 *
 * <p>Trigger conditions:
 * <ul>
 *   <li>Active population falls below P.</li>
 *   <li>Morale below M2 and trust below T3 for N2 consecutive ticks.</li>
 * </ul>
 */
public final class TheRevelationPolicy implements ChapterPolicy {

    @Override
    public ChapterType sourceChapter() {
        return ChapterType.CHAPTER_3_THE_REVELATION;
    }

    @Override
    public Optional<String> evaluateTransition(StoryState state, ChapterConfig config) {
        if (state.activePopulation() < config.populationP()) {
            return Optional.of("Settlement population dwindled below critical threshold (" + state.activePopulation() + " < " + config.populationP() + ")");
        }
        if (state.consecutiveCriticalTicks() >= config.criticalTicksN2()) {
            return Optional.of("Acute collapse: morale and trust remained depressed for " + state.consecutiveCriticalTicks() + " ticks");
        }
        return Optional.empty();
    }
}
