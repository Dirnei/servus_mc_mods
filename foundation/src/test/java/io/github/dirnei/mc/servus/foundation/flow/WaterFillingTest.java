package io.github.dirnei.mc.servus.foundation.flow;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class WaterFillingTest {

    @Test
    void equalSharesWithLeftoverToTheHungry() {
        assertArrayEquals(new int[] {50, 50}, WaterFilling.allocate(new int[] {100, 100}, 100));
        assertArrayEquals(new int[] {20, 80}, WaterFilling.allocate(new int[] {20, 100}, 100));
    }

    @Test
    void neverAboveDemand() {
        assertArrayEquals(new int[] {5, 0, 7}, WaterFilling.allocate(new int[] {5, 0, 7}, 100));
    }

    @Test
    void handlesTinyBudgets() {
        assertArrayEquals(new int[] {1, 0, 0}, WaterFilling.allocate(new int[] {5, 5, 5}, 1));
        assertArrayEquals(new int[] {2, 2, 1}, WaterFilling.allocate(new int[] {5, 5, 5}, 5));
        assertArrayEquals(new int[] {0, 0, 0}, WaterFilling.allocate(new int[] {0, 0, 0}, 5));
    }
}
