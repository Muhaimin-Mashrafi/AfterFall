package com.afterfall.chapter;

import com.afterfall.domain.Settlement;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages chapter state and orchestrates threshold-driven chapter transitions
 * with the three mandatory guardrails:
 * <ol>
 *   <li><b>Minimum dwell (D_min):</b> Transition cannot fire before D_min ticks elapsed.</li>
 *   <li><b>Pressure escalation (D_soft):</b> Soft overdue state enables pressure mechanics.</li>
 *   <li><b>Hard backstop (D_hard):</b> Forces transition if still open at D_hard, emitting a forced transition event.</li>
 * </ol>
 */
public final class ChapterManager {

    private final ChapterConfig config;
    private final Map<ChapterType, ChapterPolicy> policies = new EnumMap<>(ChapterType.class);
    private final List<ChapterTransition> transitionHistory = new CopyOnWriteArrayList<>();

    private ChapterType currentChapter = ChapterType.CHAPTER_1_A_NEW_BEGINNING;
    private long chapterStartTick = 0L;
    private int shortageTicks = 0;
    private int evidenceCount = 0;
    private int exposureLevel = 0;
    private int visibleInterventions = 0;
    private int criticalTicks = 0;

    public ChapterManager() {
        this(ChapterConfig.defaultConfig());
    }

    public ChapterManager(ChapterConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        registerPolicy(new NewBeginningPolicy());
        registerPolicy(new TheFracturePolicy());
        registerPolicy(new TheRevelationPolicy());
        registerPolicy(new TheLastRecordingPolicy());
    }

    public void registerPolicy(ChapterPolicy policy) {
        policies.put(policy.sourceChapter(), policy);
    }

    public ChapterType currentChapter() {
        return currentChapter;
    }

    public long chapterStartTick() {
        return chapterStartTick;
    }

    public ChapterConfig config() {
        return config;
    }

    public List<ChapterTransition> transitionHistory() {
        return Collections.unmodifiableList(transitionHistory);
    }

    public void recordEvidenceDiscovered() {
        evidenceCount++;
    }

    public void setExposureLevel(int level) {
        this.exposureLevel = Math.max(0, level);
    }

    public void incrementExposureLevel(int delta) {
        this.exposureLevel = Math.max(0, this.exposureLevel + delta);
    }

    public void recordVisibleIntervention() {
        visibleInterventions++;
    }

    /**
     * Evaluates chapter progression at a tick boundary.
     *
     * @return an {@link Optional} containing a {@link ChapterTransition} if a transition fired
     */
    public Optional<ChapterTransition> evaluateTick(Settlement settlement, long currentTick, boolean foodOrMedShortage) {
        if (foodOrMedShortage) {
            shortageTicks++;
        } else {
            shortageTicks = 0;
        }

        StoryState state = StoryState.snapshot(
                settlement,
                currentChapter,
                chapterStartTick,
                currentTick,
                shortageTicks,
                evidenceCount,
                exposureLevel,
                visibleInterventions,
                criticalTicks
        );

        if (state.averageMorale() < config.moraleM2() && state.averageTrust() < config.trustT3()) {
            criticalTicks++;
        } else {
            criticalTicks = 0;
        }

        if (currentChapter.isTerminal()) {
            return Optional.empty();
        }

        long dwell = currentTick - chapterStartTick;

        // Guardrail 1: Minimum Dwell (D_min)
        if (dwell < config.minDwellTicks()) {
            return Optional.empty();
        }

        ChapterPolicy policy = policies.get(currentChapter);
        if (policy == null) {
            return Optional.empty();
        }

        // Evaluate policy-driven threshold transition
        Optional<String> reasonOpt = policy.evaluateTransition(state, config);
        if (reasonOpt.isPresent()) {
            return Optional.of(transitionTo(currentChapter.next(), currentTick, reasonOpt.get(), false));
        }

        // Guardrail 3: Hard Backstop (D_hard)
        if (dwell >= config.hardBackstopTicks()) {
            String forcedReason = "Hard backstop triggered: chapter dwell (" + dwell + " ticks) reached limit of " + config.hardBackstopTicks();
            return Optional.of(transitionTo(currentChapter.next(), currentTick, forcedReason, true));
        }

        return Optional.empty();
    }

    /**
     * Guardrail 2: Returns true if chapter dwell exceeds D_soft, indicating
     * that simulation hazard / pressure mechanisms should escalate.
     */
    public boolean isPressureEscalationActive(long currentTick) {
        return (currentTick - chapterStartTick) >= config.softPressureTicks();
    }

    private ChapterTransition transitionTo(ChapterType nextChapter, long currentTick, String reason, boolean isForced) {
        ChapterType prev = this.currentChapter;
        this.currentChapter = nextChapter;
        this.chapterStartTick = currentTick;
        // Reset chapter-specific counters for the new chapter
        this.shortageTicks = 0;
        this.criticalTicks = 0;

        ChapterTransition transition = new ChapterTransition(currentTick, prev, nextChapter, reason, isForced);
        transitionHistory.add(transition);
        return transition;
    }
}
