package com.afterfall;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Verifies that the Spring application context starts. */
@SpringBootTest
@ActiveProfiles("test")
class AfterFallApplicationTests {

    @Test
    void contextLoads() {
    }
}
