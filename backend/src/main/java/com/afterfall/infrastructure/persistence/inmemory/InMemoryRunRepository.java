package com.afterfall.infrastructure.persistence.inmemory;

import com.afterfall.application.persistence.RunCheckpoint;
import com.afterfall.application.persistence.RunId;
import com.afterfall.application.persistence.RunMetadata;
import com.afterfall.application.persistence.RunRecapInputs;
import com.afterfall.application.persistence.RunRepository;
import com.afterfall.chapter.ChapterTransition;
import com.afterfall.choice.ChoiceRecord;
import com.afterfall.narrator.StoryCaption;
import com.afterfall.observer.InterventionRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-memory adapter for {@link RunRepository}.
 *
 * <p>Used for unit tests, rapid integration contract tests, and demo fallback.
 */
public final class InMemoryRunRepository implements RunRepository {

    private final ConcurrentMap<RunId, RunMetadata> runs = new ConcurrentHashMap<>();
    private final ConcurrentMap<RunId, List<RunCheckpoint>> checkpoints = new ConcurrentHashMap<>();
    private final ConcurrentMap<RunId, ConcurrentMap<String, ChoiceRecord>> choices = new ConcurrentHashMap<>();
    private final ConcurrentMap<RunId, ConcurrentMap<Long, InterventionRecord>> interventions = new ConcurrentHashMap<>();
    private final ConcurrentMap<RunId, List<ChapterTransition>> chapters = new ConcurrentHashMap<>();
    private final ConcurrentMap<RunId, ConcurrentMap<Long, StoryCaption>> captions = new ConcurrentHashMap<>();

    @Override
    public void saveRun(RunMetadata metadata) {
        if (metadata != null) {
            runs.put(metadata.runId(), metadata);
        }
    }

    @Override
    public Optional<RunMetadata> findRun(RunId runId) {
        if (runId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(runs.get(runId));
    }

    @Override
    public void saveCheckpoint(RunCheckpoint checkpoint) {
        if (checkpoint != null) {
            checkpoints.computeIfAbsent(checkpoint.runId(), k -> new CopyOnWriteArrayList<>())
                    .add(checkpoint);
        }
    }

    @Override
    public List<RunCheckpoint> checkpointsForRun(RunId runId) {
        if (runId == null) {
            return Collections.emptyList();
        }
        List<RunCheckpoint> list = checkpoints.get(runId);
        if (list == null) {
            return Collections.emptyList();
        }
        List<RunCheckpoint> copy = new ArrayList<>(list);
        copy.sort(Comparator.comparingLong(RunCheckpoint::tick));
        return Collections.unmodifiableList(copy);
    }

    @Override
    public void recordChoice(RunId runId, ChoiceRecord choiceRecord) {
        if (runId != null && choiceRecord != null) {
            choices.computeIfAbsent(runId, k -> new ConcurrentHashMap<>())
                    .putIfAbsent(choiceRecord.choiceId(), choiceRecord);
        }
    }

    @Override
    public List<ChoiceRecord> choicesForRun(RunId runId) {
        if (runId == null) {
            return Collections.emptyList();
        }
        Map<String, ChoiceRecord> map = choices.get(runId);
        if (map == null) {
            return Collections.emptyList();
        }
        List<ChoiceRecord> list = new ArrayList<>(map.values());
        list.sort(Comparator.comparingLong(ChoiceRecord::tick));
        return Collections.unmodifiableList(list);
    }

    @Override
    public void recordIntervention(RunId runId, InterventionRecord interventionRecord) {
        if (runId != null && interventionRecord != null) {
            interventions.computeIfAbsent(runId, k -> new ConcurrentHashMap<>())
                    .putIfAbsent(interventionRecord.interventionId(), interventionRecord);
        }
    }

    @Override
    public List<InterventionRecord> interventionsForRun(RunId runId) {
        if (runId == null) {
            return Collections.emptyList();
        }
        Map<Long, InterventionRecord> map = interventions.get(runId);
        if (map == null) {
            return Collections.emptyList();
        }
        List<InterventionRecord> list = new ArrayList<>(map.values());
        list.sort(Comparator.comparingLong(InterventionRecord::tick));
        return Collections.unmodifiableList(list);
    }

    @Override
    public void recordChapterTransition(RunId runId, ChapterTransition transition) {
        if (runId != null && transition != null) {
            chapters.computeIfAbsent(runId, k -> new CopyOnWriteArrayList<>())
                    .add(transition);
        }
    }

    @Override
    public List<ChapterTransition> chapterTransitionsForRun(RunId runId) {
        if (runId == null) {
            return Collections.emptyList();
        }
        List<ChapterTransition> list = chapters.get(runId);
        if (list == null) {
            return Collections.emptyList();
        }
        List<ChapterTransition> copy = new ArrayList<>(list);
        copy.sort(Comparator.comparingLong(ChapterTransition::tick));
        return Collections.unmodifiableList(copy);
    }

    @Override
    public void recordCaption(RunId runId, StoryCaption caption) {
        if (runId != null && caption != null) {
            captions.computeIfAbsent(runId, k -> new ConcurrentHashMap<>())
                    .putIfAbsent(caption.captionId(), caption);
        }
    }

    @Override
    public List<StoryCaption> captionsForRun(RunId runId) {
        if (runId == null) {
            return Collections.emptyList();
        }
        Map<Long, StoryCaption> map = captions.get(runId);
        if (map == null) {
            return Collections.emptyList();
        }
        List<StoryCaption> list = new ArrayList<>(map.values());
        list.sort(Comparator.comparingLong(StoryCaption::tick));
        return Collections.unmodifiableList(list);
    }

    @Override
    public RunRecapInputs loadRecapInputs(RunId runId) {
        RunMetadata meta = findRun(runId).orElse(null);
        return new RunRecapInputs(
                runId,
                meta,
                chapterTransitionsForRun(runId),
                choicesForRun(runId),
                interventionsForRun(runId),
                captionsForRun(runId),
                checkpointsForRun(runId),
                Collections.emptyList()
        );
    }

    public void clear() {
        runs.clear();
        checkpoints.clear();
        choices.clear();
        interventions.clear();
        chapters.clear();
        captions.clear();
    }
}
