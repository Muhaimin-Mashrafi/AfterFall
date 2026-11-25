package com.afterfall.config;

import com.afterfall.application.SimulationService;
import com.afterfall.application.persistence.RunRepository;
import com.afterfall.infrastructure.persistence.inmemory.InMemoryRunRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** The "inmemory" profile boots with no database: the offline demo fallback. */
@SpringBootTest
@ActiveProfiles("inmemory")
class InMemoryModeStartupTest {

    @Autowired
    private RunRepository runRepository;

    @Autowired
    private SimulationService simulationService;

    @Test
    void contextStartsWithoutDatabaseAndUsesInMemoryAdapters() {
        assertInstanceOf(InMemoryRunRepository.class, runRepository);
        assertNotNull(simulationService);
    }
}
