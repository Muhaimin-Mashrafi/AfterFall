package com.afterfall.chapter;

import java.util.Objects;

/**
 * Record of an executed chapter transition.
 */
public record ChapterTransition(
        long tick,
        ChapterType fromChapter,
        ChapterType toChapter,
        String triggerReason,
        boolean isForced
) {
    public ChapterTransition {
        Objects.requireNonNull(fromChapter, "fromChapter");
        Objects.requireNonNull(toChapter, "toChapter");
        Objects.requireNonNull(triggerReason, "triggerReason");
    }
}
