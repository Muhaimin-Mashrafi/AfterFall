package com.afterfall.config;

import com.afterfall.application.persistence.EventRecorder;
import com.afterfall.application.persistence.RunId;
import com.afterfall.application.persistence.RunRepository;
import com.afterfall.events.EventBus;
import com.afterfall.infrastructure.persistence.async.AsyncEventRecorder;
import com.afterfall.infrastructure.persistence.inmemory.InMemoryEventRecorder;
import com.afterfall.infrastructure.persistence.inmemory.InMemoryRunRepository;
import com.afterfall.infrastructure.persistence.jpa.JpaEventRecorder;
import com.afterfall.infrastructure.persistence.jpa.JpaPostgresRunRepository;
import com.afterfall.application.persistence.EvidenceDiscoveryRepository;
import com.afterfall.infrastructure.persistence.inmemory.InMemoryEvidenceDiscoveryRepository;
import com.afterfall.infrastructure.persistence.jpa.JpaEvidenceDiscoveryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for AfterFall persistence adapters and asynchronous event recording.
 */
@Configuration
public class PersistenceConfiguration {

    @Bean
    public InMemoryEventRecorder inMemoryEventRecorder() {
        return new InMemoryEventRecorder();
    }

    @Bean
    public InMemoryRunRepository inMemoryRunRepository() {
        return new InMemoryRunRepository();
    }

    @Bean
    public InMemoryEvidenceDiscoveryRepository inMemoryEvidenceDiscoveryRepository() {
        return new InMemoryEvidenceDiscoveryRepository();
    }

    @Bean
    @Primary
    public EvidenceDiscoveryRepository evidenceDiscoveryRepository(JpaEvidenceDiscoveryRepository jpaRepo) {
        return jpaRepo;
    }

    @Bean
    @Primary
    public RunRepository runRepository(JpaPostgresRunRepository jpaRepo) {
        return jpaRepo;
    }

    @Bean
    @Primary
    public EventRecorder eventRecorder(AsyncEventRecorder asyncEventRecorder) {
        return asyncEventRecorder;
    }

    @Bean
    public AsyncEventRecorder asyncEventRecorder(
            JpaEventRecorder jpaEventRecorder,
            InMemoryEventRecorder inMemoryEventRecorder,
            @Value("${afterfall.run.default-id:current-run}") String defaultRunIdStr,
            @Value("${afterfall.async-recorder.minor-capacity:1000}") int minorCapacity,
            @Value("${afterfall.async-recorder.batch-size:50}") int batchSize,
            @Value("${afterfall.async-recorder.flush-interval-ms:100}") long flushIntervalMs) {

        return new AsyncEventRecorder(
                jpaEventRecorder,
                inMemoryEventRecorder,
                RunId.of(defaultRunIdStr),
                minorCapacity,
                batchSize,
                flushIntervalMs
        );
    }

    @Bean
    public EventBus eventBus(AsyncEventRecorder asyncEventRecorder) {
        EventBus eventBus = new EventBus();
        // Automatically register AsyncEventRecorder as subscriber so all simulation events reach persistence
        eventBus.register(asyncEventRecorder);
        return eventBus;
    }
}
