package com.afterfall.chapter;

/**
 * The four sequential narrative chapters of AfterFall V1.
 */
public enum ChapterType {
    CHAPTER_1_A_NEW_BEGINNING("Chapter I: A New Beginning"),
    CHAPTER_2_THE_FRACTURE("Chapter II: The Fracture"),
    CHAPTER_3_THE_REVELATION("Chapter III: The Revelation"),
    CHAPTER_4_THE_LAST_RECORDING("Chapter IV: The Last Recording");

    private final String displayName;

    ChapterType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public ChapterType next() {
        return switch (this) {
            case CHAPTER_1_A_NEW_BEGINNING -> CHAPTER_2_THE_FRACTURE;
            case CHAPTER_2_THE_FRACTURE -> CHAPTER_3_THE_REVELATION;
            case CHAPTER_3_THE_REVELATION -> CHAPTER_4_THE_LAST_RECORDING;
            case CHAPTER_4_THE_LAST_RECORDING -> CHAPTER_4_THE_LAST_RECORDING;
        };
    }

    public boolean isTerminal() {
        return this == CHAPTER_4_THE_LAST_RECORDING;
    }
}
