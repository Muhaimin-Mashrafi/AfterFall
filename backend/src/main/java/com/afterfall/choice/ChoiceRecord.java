package com.afterfall.choice;

import java.util.Objects;

/**
 * Audit record of a resolved choice for the decision log and future recap.
 */
public record ChoiceRecord(
        String choiceId,
        long tick,
        String chapterId,
        String selectedOptionId,
        String optionLabel,
        String consequenceSummary,
        boolean usedDefault
) {
    public ChoiceRecord {
        Objects.requireNonNull(choiceId, "choiceId");
        Objects.requireNonNull(chapterId, "chapterId");
        Objects.requireNonNull(selectedOptionId, "selectedOptionId");
        Objects.requireNonNull(optionLabel, "optionLabel");
        Objects.requireNonNull(consequenceSummary, "consequenceSummary");
    }
}
