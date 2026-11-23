package com.afterfall.domain;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link Resource}.
 * Covers: basic add/consume, never-negative invariant, and concurrent safety.
 */
class ResourceTest {

    @Test
    void initialQuantityIsRetained() {
        Resource r = new Resource(ResourceType.FOOD, 100);
        assertThat(r.quantity()).isEqualTo(100);
        assertThat(r.type()).isEqualTo(ResourceType.FOOD);
    }

    @Test
    void addIncreasesQuantity() {
        Resource r = new Resource(ResourceType.WATER, 10);
        r.add(20);
        assertThat(r.quantity()).isEqualTo(30);
    }

    @Test
    void consumeDecreasesQuantity() {
        Resource r = new Resource(ResourceType.MEDICINE, 50);
        int consumed = r.consume(20);
        assertThat(consumed).isEqualTo(20);
        assertThat(r.quantity()).isEqualTo(30);
    }

    @Test
    void consumeNeverGoesBelowZero() {
        Resource r = new Resource(ResourceType.FOOD, 5);
        int consumed = r.consume(10);
        assertThat(consumed).isEqualTo(5); // only 5 available
        assertThat(r.quantity()).isZero();
    }

    @Test
    void consumeAllReturnsExactAmount() {
        Resource r = new Resource(ResourceType.FOOD, 7);
        int consumed = r.consume(7);
        assertThat(consumed).isEqualTo(7);
        assertThat(r.quantity()).isZero();
    }

    @Test
    void isAvailableReflectsStock() {
        Resource r = new Resource(ResourceType.WATER, 1);
        assertThat(r.isAvailable()).isTrue();
        r.consume(1);
        assertThat(r.isAvailable()).isFalse();
    }

    @Test
    void rejectsNegativeInitialQuantity() {
        assertThatThrownBy(() -> new Resource(ResourceType.FOOD, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPositiveAdd() {
        Resource r = new Resource(ResourceType.FOOD, 10);
        assertThatThrownBy(() -> r.add(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> r.add(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPositiveConsume() {
        Resource r = new Resource(ResourceType.FOOD, 10);
        assertThatThrownBy(() -> r.consume(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> r.consume(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Race-condition test: 20 threads simultaneously consume from one Resource.
     * Quantity must never go below zero (ReentrantLock correctness).
     * See blueprint section 12.3.
     */
    @Test
    void raceConditionTestQuantityNeverNegative() throws InterruptedException {
        Resource r = new Resource(ResourceType.FOOD, 100);
        ExecutorService pool = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 50; i++) {
            pool.submit(() -> r.consume(3));
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        assertThat(r.quantity()).isGreaterThanOrEqualTo(0);
    }
}
