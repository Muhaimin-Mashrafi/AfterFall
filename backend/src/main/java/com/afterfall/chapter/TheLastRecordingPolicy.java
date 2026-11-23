package com.afterfall.chapter;

import java.util.Optional;

/**
 * Terminal chapter policy. Chapter IV is the final chapter of AfterFall V1.
 */
public final class TheLastRecordingPolicy implements ChapterPolicy {

    @Override
    public ChapterType sourceChapter() {
        return ChapterType.CHAPTER_4_THE_LAST_RECORDING;
    }

    @Override
    public Optional<String> evaluateTransition(StoryState state, ChapterConfig config) {
        return Optional.empty(); // Terminal chapter
    }
}
