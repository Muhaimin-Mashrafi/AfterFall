package com.afterfall.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link ResourceStock}.
 */
class ResourceStockTest {

    @Test
    void defaultStockHasPositiveQuantities() {
        ResourceStock stock = ResourceStock.defaultStock();
        assertThat(stock.quantity(ResourceType.FOOD)).isPositive();
        assertThat(stock.quantity(ResourceType.WATER)).isPositive();
        assertThat(stock.quantity(ResourceType.MEDICINE)).isPositive();
    }

    @Test
    void customQuantitiesAreRetained() {
        ResourceStock stock = new ResourceStock(100, 80, 30);
        assertThat(stock.quantity(ResourceType.FOOD)).isEqualTo(100);
        assertThat(stock.quantity(ResourceType.WATER)).isEqualTo(80);
        assertThat(stock.quantity(ResourceType.MEDICINE)).isEqualTo(30);
    }

    @Test
    void isAvailableReflectsStock() {
        ResourceStock stock = new ResourceStock(1, 0, 0);
        assertThat(stock.isAvailable(ResourceType.FOOD)).isTrue();
        assertThat(stock.isAvailable(ResourceType.WATER)).isFalse();
        assertThat(stock.isAvailable(ResourceType.MEDICINE)).isFalse();
    }

    @Test
    void resourceReturnsCorrectInstance() {
        ResourceStock stock = new ResourceStock(10, 20, 5);
        assertThat(stock.resource(ResourceType.FOOD).type()).isEqualTo(ResourceType.FOOD);
        assertThat(stock.resource(ResourceType.WATER).type()).isEqualTo(ResourceType.WATER);
        assertThat(stock.resource(ResourceType.MEDICINE).type()).isEqualTo(ResourceType.MEDICINE);
    }

    @Test
    void consumeFromStockDecreasesQuantity() {
        ResourceStock stock = new ResourceStock(50, 50, 20);
        stock.resource(ResourceType.FOOD).consume(10);
        assertThat(stock.quantity(ResourceType.FOOD)).isEqualTo(40);
    }
}
