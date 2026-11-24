package com.afterfall.infrastructure.persistence.jpa;

import com.afterfall.application.persistence.EventRecorder;
import com.afterfall.infrastructure.persistence.AbstractEventRecorderContractTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaEventRecorderTest extends AbstractEventRecorderContractTest {

    @Autowired
    private JpaEventRecorder jpaEventRecorder;

    @Override
    protected EventRecorder createRecorder() {
        return jpaEventRecorder;
    }
}
