package com.afterfall.application;

import com.afterfall.application.persistence.RunId;
import com.afterfall.domain.EventId;
import com.afterfall.events.SimEvent;
import com.afterfall.infrastructure.persistence.async.AsyncEventRecorder;
import com.afterfall.infrastructure.persistence.inmemory.InMemoryEventRecorder;
import com.afterfall.infrastructure.persistence.inmemory.InMemoryEvidenceDiscoveryRepository;
import com.afterfall.infrastructure.persistence.inmemory.InMemoryRunRepository;
import com.afterfall.narrator.StoryCaption;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test verifying that SimulationService registers EventRecorder on EventBus
 * so every persisted caption's sourceEventIds resolve to persisted canonical events.
 *
 * <p>The EventBus dispatches asynchronously; {@code drainAll()} is called after stepping
 * to ensure all subscriber callbacks complete before assertions.
 */
class EventToCaptionPersistenceTest {

    @Test
    void persistedCaptionsResolveToPersistedCanonicalEvents() throws InterruptedException {
        InMemoryRunRepository runRepo = new InMemoryRunRepository();
        InMemoryEventRecorder eventRecorder = new InMemoryEventRecorder();
        InMemoryEvidenceDiscoveryRepository evidenceRepo = new InMemoryEvidenceDiscoveryRepository();

        SimulationService service = new SimulationService(runRepo, eventRecorder, evidenceRepo);
        for (int i = 0; i < 5; i++) {
            service.stepTick();
        }

        // Drain async EventBus queues so all subscriber callbacks have completed
        boolean drained = service.eventBus().drainAll(5, TimeUnit.SECONDS);
        assertThat(drained).as("EventBus drained within timeout").isTrue();

        RunId runId = RunId.of("run-1");
        List<SimEvent> events = eventRecorder.eventsForRun(runId);
        List<StoryCaption> captions = runRepo.captionsForRun(runId);

        assertThat(events).isNotEmpty();
        assertThat(captions).isNotEmpty();

        Set<EventId> persistedEventIds = events.stream()
                .map(SimEvent::id)
                .collect(Collectors.toSet());
        // Idempotent store guarantees no duplicates — one entry per unique EventId
        assertThat(persistedEventIds).hasSize(events.size());

        for (StoryCaption caption : captions) {
            for (EventId sourceId : caption.sourceEventIds()) {
                assertThat(persistedEventIds)
                        .as("Caption %s references missing EventId %s", caption.captionId(), sourceId)
                        .contains(sourceId);
            }
        }
    }

    /**
     * Mirrors the production wiring: the recorder handed to SimulationService is the
     * AsyncEventRecorder bean whose configured default run id is "current-run", not "run-1".
     * Captions are stored under run-1, so canonical events must also land under run-1.
     */
    @Test
    void productionWiringWithAsyncRecorderPersistsEventsUnderTheSimulationRunId() throws Exception {
        InMemoryRunRepository runRepo = new InMemoryRunRepository();
        InMemoryEventRecorder primary = new InMemoryEventRecorder();
        InMemoryEventRecorder fallback = new InMemoryEventRecorder();
        InMemoryEvidenceDiscoveryRepository evidenceRepo = new InMemoryEvidenceDiscoveryRepository();

        try (AsyncEventRecorder async = new AsyncEventRecorder(
                primary, fallback, RunId.of("current-run"), 1000, 50, 10)) {
            SimulationService service = new SimulationService(runRepo, async, evidenceRepo);
            for (int i = 0; i < 20; i++) {
                service.stepTick();
            }
            assertThat(service.eventBus().drainAll(5, TimeUnit.SECONDS)).isTrue();
            async.flush();

            RunId runId = RunId.of("run-1");
            Set<EventId> persisted = primary.eventsForRun(runId).stream()
                    .map(SimEvent::id).collect(Collectors.toSet());
            List<StoryCaption> captions = runRepo.captionsForRun(runId);

            assertThat(persisted).as("canonical events under run-1").isNotEmpty();
            assertThat(captions).isNotEmpty();
            assertThat(primary.eventsForRun(RunId.of("current-run")))
                    .as("nothing recorded under the stray default run id").isEmpty();
            for (StoryCaption caption : captions) {
                assertThat(caption.sourceEventIds()).isNotEmpty();
                assertThat(persisted).containsAll(caption.sourceEventIds());
            }
        }
    }
}
