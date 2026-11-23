package com.afterfall.chapter;

import java.util.Optional;

/**
 * Chapter I: A New Beginning -> Chapter II: The Fracture.
 *
 * <p>Trigger conditions:
 * <ul>
 *   <li>Average trust below T1.</li>
 *   <li>Resource shortage active for N1 consecutive ticks.</li>
 *   <li>First Observer intervention with visible effect executed.</li>
 * </ul>
 */
public final class NewBeginningPolicy implements ChapterPolicy {

    @Override
    public ChapterType sourceChapter() {
        return ChapterType.CHAPTER_1_A_NEW_BEGINNING;
    }

    @Override
    public Optional<String> evaluateTransition(StoryState state, ChapterConfig config) {
        if (state.averageTrust() < config.trustT1()) {
            return Optional.of("Community trust eroded below threshold (" + state.averageTrust() + " < " + config.trustT1() + ")");
        }
        if (state.consecutiveShortageTicks() >= config.shortageN1()) {
            return Optional.of("Resource shortages persisted for " + state.consecutiveShortageTicks() + " consecutive ticks");
        }
        if (state.visibleInterventionsCount() >= 1) {
            return Optional.of("Observer intervention produced visible settlement discord");
        }
        return Optional.empty();
    }
}
