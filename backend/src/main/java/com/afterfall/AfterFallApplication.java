package com.afterfall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for AfterFall.
 *
 * <p>Spring is the application/infrastructure layer only. The simulation core
 * ({@code com.afterfall.domain} and {@code com.afterfall.simulation}) is plain Java
 * and must not import Spring, JPA or PostgreSQL classes.
 */
@SpringBootApplication
public class AfterFallApplication {

    public static void main(String[] args) {
        SpringApplication.run(AfterFallApplication.class, args);
    }
}
