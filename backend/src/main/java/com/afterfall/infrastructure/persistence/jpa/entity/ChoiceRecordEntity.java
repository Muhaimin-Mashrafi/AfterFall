package com.afterfall.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "choice_records", indexes = {
        @Index(name = "idx_choice_run_tick", columnList = "run_id, tick")
})
@IdClass(ChoiceRecordEntity.ChoiceCompositeKey.class)
public class ChoiceRecordEntity {

    public static class ChoiceCompositeKey implements Serializable {
        private String runId;
        private String choiceId;

        public ChoiceCompositeKey() {}
        public ChoiceCompositeKey(String runId, String choiceId) {
            this.runId = runId;
            this.choiceId = choiceId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChoiceCompositeKey that = (ChoiceCompositeKey) o;
            return Objects.equals(runId, that.runId) && Objects.equals(choiceId, that.choiceId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(runId, choiceId);
        }
    }

    @Id
    @Column(name = "run_id", nullable = false, length = 64)
    private String runId;

    @Id
    @Column(name = "choice_id", nullable = false, length = 64)
    private String choiceId;

    @Column(name = "tick", nullable = false)
    private long tick;

    @Column(name = "chapter_id", nullable = false, length = 64)
    private String chapterId;

    @Column(name = "selected_option_id", nullable = false, length = 64)
    private String selectedOptionId;

    @Column(name = "option_label", nullable = false, length = 256)
    private String optionLabel;

    @Column(name = "consequence_summary", nullable = false, length = 512)
    private String consequenceSummary;

    @Column(name = "used_default", nullable = false)
    private boolean usedDefault;

    public ChoiceRecordEntity() {}

    public ChoiceRecordEntity(String runId, String choiceId, long tick, String chapterId, String selectedOptionId, String optionLabel, String consequenceSummary, boolean usedDefault) {
        this.runId = runId;
        this.choiceId = choiceId;
        this.tick = tick;
        this.chapterId = chapterId;
        this.selectedOptionId = selectedOptionId;
        this.optionLabel = optionLabel;
        this.consequenceSummary = consequenceSummary;
        this.usedDefault = usedDefault;
    }

    public String getRunId() { return runId; }
    public String getChoiceId() { return choiceId; }
    public long getTick() { return tick; }
    public String getChapterId() { return chapterId; }
    public String getSelectedOptionId() { return selectedOptionId; }
    public String getOptionLabel() { return optionLabel; }
    public String getConsequenceSummary() { return consequenceSummary; }
    public boolean isUsedDefault() { return usedDefault; }
}
