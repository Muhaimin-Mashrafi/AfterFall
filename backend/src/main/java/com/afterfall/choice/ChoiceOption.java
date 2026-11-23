package com.afterfall.choice;

import java.util.Objects;

/**
 * An available choice option presented to the Humanity Overseer / Observer.
 */
public record ChoiceOption(
        String optionId,
        String label,
        String description,
        ChoiceConsequence consequence,
        boolean isDefault
) {
    public ChoiceOption {
        Objects.requireNonNull(optionId, "optionId");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(consequence, "consequence");
    }
}
