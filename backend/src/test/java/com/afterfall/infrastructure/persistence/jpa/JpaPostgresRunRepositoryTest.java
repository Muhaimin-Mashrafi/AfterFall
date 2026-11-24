package com.afterfall.infrastructure.persistence.jpa;

import com.afterfall.application.persistence.RunRepository;
import com.afterfall.infrastructure.persistence.AbstractRunRepositoryContractTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaPostgresRunRepositoryTest extends AbstractRunRepositoryContractTest {

    @Autowired
    private JpaPostgresRunRepository jpaPostgresRunRepository;

    @Override
    protected RunRepository createRepository() {
        return jpaPostgresRunRepository;
    }
}
