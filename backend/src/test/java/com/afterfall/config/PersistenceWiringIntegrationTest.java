package com.afterfall.config;

import com.afterfall.application.persistence.EventRecorder;
import com.afterfall.application.persistence.RunId;
import com.afterfall.application.persistence.RunRepository;
import com.afterfall.events.SimEvent;
import com.afterfall.infrastructure.persistence.async.AsyncEventRecorder;
import com.afterfall.infrastructure.persistence.inmemory.InMemoryEventRecorder;
import com.afterfall.infrastructure.persistence.inmemory.InMemoryRunRepository;
import com.afterfall.infrastructure.persistence.jpa.JpaEventRecorder;
import com.afterfall.infrastructure.persistence.jpa.JpaPostgresRunRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PersistenceWiringIntegrationTest {

    @Autowired
    private RunRepository runRepository;

    @Autowired
    private EventRecorder eventRecorder;

    @Autowired
    private JpaPostgresRunRepository jpaPostgresRunRepository;

    @Autowired
    private JpaEventRecorder jpaEventRecorder;

    @Autowired
    private InMemoryRunRepository inMemoryRunRepository;

    @Autowired
    private InMemoryEventRecorder inMemoryEventRecorder;

    @Autowired
    private AsyncEventRecorder asyncEventRecorder;

    @Test
    void verifiesAllPersistenceBeansAreRegisteredAndWired() {
        assertNotNull(runRepository, "RunRepository bean should be injected");
        assertNotNull(eventRecorder, "EventRecorder bean should be injected");
        assertNotNull(jpaPostgresRunRepository, "JpaPostgresRunRepository should be present");
        assertNotNull(jpaEventRecorder, "JpaEventRecorder should be present");
        assertNotNull(inMemoryRunRepository, "InMemoryRunRepository should be present");
        assertNotNull(inMemoryEventRecorder, "InMemoryEventRecorder should be present");
        assertNotNull(asyncEventRecorder, "AsyncEventRecorder should be present");

        assertSame(jpaPostgresRunRepository, runRepository, "Primary RunRepository should be JpaPostgresRunRepository");
        assertSame(asyncEventRecorder, eventRecorder, "Primary EventRecorder should be AsyncEventRecorder");
    }

    @Test
    void asyncRecorderPersistsEventsUnderItsDefaultRunId() throws Exception {
        RunId defaultRunId = RunId.of("current-run");

        // Directly verify onEvents call
        SimEvent sample = new SimEvent(
                new com.afterfall.domain.EventId(9999L),
                50L,
                "TEST_FANOUT",
                List.of(new com.afterfall.domain.SurvivorId(1)),
                java.util.Optional.empty(),
                com.afterfall.events.EventSeverity.CRITICAL,
                "Sample summary",
                java.util.Map.of(),
                List.of()
        );

        asyncEventRecorder.onEvents(List.of(sample));
        asyncEventRecorder.flush();

        List<SimEvent> events = asyncEventRecorder.eventsForRun(defaultRunId);
        assertTrue(events.stream().anyMatch(e -> e.id().value() == 9999L),
                "SimEvent must be received and persisted by AsyncEventRecorder");
    }
}
