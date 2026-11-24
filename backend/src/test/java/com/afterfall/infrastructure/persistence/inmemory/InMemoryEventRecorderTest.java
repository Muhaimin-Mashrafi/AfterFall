package com.afterfall.infrastructure.persistence.inmemory;

import com.afterfall.application.persistence.EventRecorder;
import com.afterfall.infrastructure.persistence.AbstractEventRecorderContractTest;

class InMemoryEventRecorderTest extends AbstractEventRecorderContractTest {
    @Override
    protected EventRecorder createRecorder() {
        return new InMemoryEventRecorder();
    }
}
