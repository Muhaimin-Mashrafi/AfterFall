package com.afterfall.chapter;

import java.util.Optional;

/**
 * Strategy pattern interface for chapter transition evaluation.
 */
public interface ChapterPolicy {

    /** The chapter evaluated by this policy. */
    ChapterType sourceChapter();

    /**
     * Evaluates whether simulation conditions meet the transition criteria.
     * Returns the trigger reason if transition conditions are satisfied, or empty.
     */
    Optional<String> evaluateTransition(StoryState state, ChapterConfig config);
}
