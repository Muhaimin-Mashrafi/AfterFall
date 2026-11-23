package com.afterfall.choice;

import com.afterfall.domain.Need;
import com.afterfall.domain.ResourceType;
import com.afterfall.domain.SurvivorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChoiceTest {

    private ChoiceCoordinator coordinator;

    @BeforeEach
    void setUp() {
        coordinator = new ChoiceCoordinator();
    }

    @Test
    void rejectsMomentWithoutDefaultOption() {
        ChoiceOption opt = new ChoiceOption("OPT_A", "Option A", "Desc", ChoiceConsequence.builder("Cons").build(), false);
        assertThatThrownBy(() -> new ChoiceMoment("CH_1", 10L, "CHAP_1", "Title", "Prompt", List.of(opt), "NON_EXISTENT"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submittingValidOptionQueuesConsequenceAndLogsDecision() {
        ChoiceConsequence consA = ChoiceConsequence.builder("Add food")
                .resourceDelta(ResourceType.FOOD, 20)
                .build();
        ChoiceConsequence consB = ChoiceConsequence.builder("Add med")
                .resourceDelta(ResourceType.MEDICINE, 5)
                .build();

        ChoiceOption optA = new ChoiceOption("OPT_A", "Ration Food", "Desc", consA, false);
        ChoiceOption optB = new ChoiceOption("OPT_B", "Prioritize Meds", "Desc", consB, true);

        ChoiceMoment moment = new ChoiceMoment("CH_1", 10L, "CHAP_1", "Crisis Choice", "Prompt", List.of(optA, optB), "OPT_B");
        coordinator.triggerChoice(moment);

        assertThat(coordinator.hasPendingPause()).isTrue();

        ChoiceRecord record = coordinator.submitDecision("CH_1", "OPT_A");
        assertThat(record.selectedOptionId()).isEqualTo("OPT_A");
        assertThat(record.usedDefault()).isFalse();
        assertThat(coordinator.hasPendingPause()).isFalse();

        List<ChoiceConsequence> drained = coordinator.drainPendingConsequences();
        assertThat(drained).hasSize(1);
        assertThat(drained.get(0).summary()).isEqualTo("Add food");
        assertThat(drained.get(0).resourceDeltas()).containsEntry(ResourceType.FOOD, 20);
    }

    @Test
    void invalidOptionFallsBackToGuaranteedDefault() {
        ChoiceConsequence consDefault = ChoiceConsequence.builder("Default path")
                .resourceDelta(ResourceType.WATER, 10)
                .build();
        ChoiceOption optA = new ChoiceOption("OPT_DEFAULT", "Default", "Desc", consDefault, true);

        ChoiceMoment moment = new ChoiceMoment("CH_2", 15L, "CHAP_1", "Choice", "Prompt", List.of(optA), "OPT_DEFAULT");
        coordinator.triggerChoice(moment);

        // Submitting an invalid option id
        ChoiceRecord record = coordinator.submitDecision("CH_2", "NON_EXISTENT");
        assertThat(record.selectedOptionId()).isEqualTo("OPT_DEFAULT");
        assertThat(record.usedDefault()).isTrue();

        List<ChoiceConsequence> drained = coordinator.drainPendingConsequences();
        assertThat(drained).hasSize(1);
        assertThat(drained.get(0).summary()).isEqualTo("Default path");
    }

    @Test
    void cannotResolveSameChoiceTwice() {
        ChoiceOption opt = new ChoiceOption("OPT", "Option", "Desc", ChoiceConsequence.builder("C").build(), true);
        ChoiceMoment moment = new ChoiceMoment("CH_3", 10L, "CHAP_1", "T", "P", List.of(opt), "OPT");
        coordinator.triggerChoice(moment);

        coordinator.submitDecision("CH_3", "OPT");
        assertThatThrownBy(() -> coordinator.submitDecision("CH_3", "OPT"))
                .isInstanceOf(IllegalStateException.class);
    }
}
