package com.afterfall.infrastructure.persistence.jpa;

import com.afterfall.application.persistence.EventRecorder;
import com.afterfall.application.persistence.RunRepository;
import com.afterfall.infrastructure.persistence.AbstractEventRecorderContractTest;
import com.afterfall.infrastructure.persistence.AbstractRunRepositoryContractTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verified integration test running against the actual PostgreSQL 17 instance.
 * Uses the default/production profile properties pointing to PostgreSQL.
 */
@SpringBootTest
@Transactional
class Postgres17LiveIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JpaEventRecorder jpaEventRecorder;

    @Autowired
    private JpaPostgresRunRepository jpaPostgresRunRepository;

    @Test
    void verifiesRealPostgres17DatabaseMetadata() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String dbName = metaData.getDatabaseProductName();
            String dbVersion = metaData.getDatabaseProductVersion();
            assertTrue(dbName.toLowerCase().contains("postgresql"), "Must be connected to real PostgreSQL: " + dbName);
            assertTrue(dbVersion.contains("17"), "Must be PostgreSQL version 17.x: " + dbVersion);
        }
    }

    @Nested
    class PostgresEventRecorderContractTest extends AbstractEventRecorderContractTest {
        @Override
        protected EventRecorder createRecorder() {
            return jpaEventRecorder;
        }
    }

    @Nested
    class PostgresRunRepositoryContractTest extends AbstractRunRepositoryContractTest {
        @Override
        protected RunRepository createRepository() {
            return jpaPostgresRunRepository;
        }
    }
}
