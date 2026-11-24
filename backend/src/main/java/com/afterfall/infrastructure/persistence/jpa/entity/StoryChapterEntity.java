package com.afterfall.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "story_chapters", indexes = {
        @Index(name = "idx_chapter_run_tick", columnList = "run_id, tick")
})
public class StoryChapterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false, length = 64)
    private String runId;

    @Column(name = "tick", nullable = false)
    private long tick;

    @Column(name = "from_chapter", nullable = false, length = 64)
    private String fromChapter;

    @Column(name = "to_chapter", nullable = false, length = 64)
    private String toChapter;

    @Column(name = "trigger_reason", nullable = false, length = 512)
    private String triggerReason;

    @Column(name = "is_forced", nullable = false)
    private boolean isForced;

    public StoryChapterEntity() {}

    public StoryChapterEntity(String runId, long tick, String fromChapter, String toChapter, String triggerReason, boolean isForced) {
        this.runId = runId;
        this.tick = tick;
        this.fromChapter = fromChapter;
        this.toChapter = toChapter;
        this.triggerReason = triggerReason;
        this.isForced = isForced;
    }

    public Long getId() { return id; }
    public String getRunId() { return runId; }
    public long getTick() { return tick; }
    public String getFromChapter() { return fromChapter; }
    public String getToChapter() { return toChapter; }
    public String getTriggerReason() { return triggerReason; }
    public boolean isForced() { return isForced; }
}
