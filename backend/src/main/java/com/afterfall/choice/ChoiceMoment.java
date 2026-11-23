package com.afterfall.choice;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A critical decision moment that requests user / Overseer intervention.
 *
 * <p>Contains authored options, a guaranteed default option if unselected,
 * and tracks resolution status.
 */
public final class ChoiceMoment {

    private final String id;
    private final long tick;
    private final String chapterId;
    private final String title;
    private final String prompt;
    private final List<ChoiceOption> options;
    private final String defaultOptionId;
    private Optional<String> selectedOptionId = Optional.empty();

    public ChoiceMoment(String id,
                        long tick,
                        String chapterId,
                        String title,
                        String prompt,
                        List<ChoiceOption> options,
                        String defaultOptionId) {
        this.id = Objects.requireNonNull(id, "id");
        this.tick = tick;
        this.chapterId = Objects.requireNonNull(chapterId, "chapterId");
        this.title = Objects.requireNonNull(title, "title");
        this.prompt = Objects.requireNonNull(prompt, "prompt");
        this.options = List.copyOf(Objects.requireNonNull(options, "options"));
        this.defaultOptionId = Objects.requireNonNull(defaultOptionId, "defaultOptionId");

        if (this.options.isEmpty()) {
            throw new IllegalArgumentException("ChoiceMoment must contain at least one option");
        }
        boolean hasDefault = this.options.stream().anyMatch(o -> o.optionId().equals(defaultOptionId));
        if (!hasDefault) {
            throw new IllegalArgumentException("Default option ID '" + defaultOptionId + "' not found among options");
        }
    }

    public String id() {
        return id;
    }

    public long tick() {
        return tick;
    }

    public String chapterId() {
        return chapterId;
    }

    public String title() {
        return title;
    }

    public String prompt() {
        return prompt;
    }

    public List<ChoiceOption> options() {
        return options;
    }

    public String defaultOptionId() {
        return defaultOptionId;
    }

    public Optional<String> selectedOptionId() {
        return selectedOptionId;
    }

    public boolean isResolved() {
        return selectedOptionId.isPresent();
    }

    public Optional<ChoiceOption> findOption(String optionId) {
        return options.stream().filter(o -> o.optionId().equals(optionId)).findFirst();
    }

    public ChoiceOption resolve(String optionId) {
        ChoiceOption option = findOption(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown optionId: " + optionId));
        this.selectedOptionId = Optional.of(optionId);
        return option;
    }

    public ChoiceOption resolveDefault() {
        return resolve(defaultOptionId);
    }
}
