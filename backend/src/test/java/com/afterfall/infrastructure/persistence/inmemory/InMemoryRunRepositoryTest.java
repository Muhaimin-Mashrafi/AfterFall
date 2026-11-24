package com.afterfall.infrastructure.persistence.inmemory;

import com.afterfall.application.persistence.RunRepository;
import com.afterfall.infrastructure.persistence.AbstractRunRepositoryContractTest;

class InMemoryRunRepositoryTest extends AbstractRunRepositoryContractTest {
    @Override
    protected RunRepository createRepository() {
        return new InMemoryRunRepository();
    }
}
