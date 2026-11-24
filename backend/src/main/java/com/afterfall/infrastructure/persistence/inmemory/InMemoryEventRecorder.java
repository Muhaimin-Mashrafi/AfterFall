package com.afterfall.infrastructure.persistence.inmemory;

import com.afterfall.application.persistence.EventRecorder;
import com.afterfall.application.persistence.RunId;
import com.afterfall.domain.EventId;
import com.afterfall.events.SimEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory adapter for {@link EventRecorder}.
 *
 * <p>Preserves canonical ordering, guarantees idempotency per {@link EventId},
 * and functions as a standalone testing store and a non-crash-durable runtime fallback.
 */
public final class InMemoryEventRecorder implements EventRecorder {

    private final ConcurrentMap<RunId, ConcurrentMap<EventId, SimEvent>> storage = new ConcurrentHashMap<>();

    @Override
    public void record(RunId runId, SimEvent event) {
        if (runId == null || event == null) {
            return;
        }
        storage.computeIfAbsent(runId, k -> new ConcurrentHashMap<>())
                .putIfAbsent(event.id(), event);
    }

    @Override
    public void recordAll(RunId runId, List<SimEvent> events) {
        if (runId == null || events == null) {
            return;
        }
        ConcurrentMap<EventId, SimEvent> runEvents = storage.computeIfAbsent(runId, k -> new ConcurrentHashMap<>());
        for (SimEvent event : events) {
            if (event != null) {
                runEvents.putIfAbsent(event.id(), event);
            }
        }
    }

    @Override
    public List<SimEvent> eventsForRun(RunId runId) {
        if (runId == null) {
            return Collections.emptyList();
        }
        Map<EventId, SimEvent> runEvents = storage.get(runId);
        if (runEvents == null || runEvents.isEmpty()) {
            return Collections.emptyList();
        }
        List<SimEvent> list = new ArrayList<>(runEvents.values());
        list.sort(Comparator.comparingLong(e -> e.id().value()));
        return Collections.unmodifiableList(list);
    }

    public void clear() {
        storage.clear();
    }
}
