package io.github.dirnei.mc.servus.shuttles.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AmountStepsTest {

    @Test
    void plusStackFillsTheStackFirst() {
        assertEquals(64, AmountSteps.plusStack(1, 64));
        assertEquals(128, AmountSteps.plusStack(64, 64));
        assertEquals(128, AmountSteps.plusStack(70, 64));
        assertEquals(64, AmountSteps.plusStack(0, 64));
        assertEquals(16, AmountSteps.plusStack(1, 16));
    }

    @Test
    void minusStackDropsToTheStackBelow() {
        assertEquals(64, AmountSteps.minusStack(70, 64));
        assertEquals(64, AmountSteps.minusStack(128, 64));
        assertEquals(1, AmountSteps.minusStack(64, 64));
        assertEquals(1, AmountSteps.minusStack(10, 64));
    }

    @Test
    void addStaysInRange() {
        assertEquals(11, AmountSteps.add(1, 10));
        assertEquals(1, AmountSteps.add(5, -10));
        assertEquals(1, AmountSteps.add(1, -1));
        assertEquals(AmountSteps.MAX, AmountSteps.add(99995, 10));
    }

    @Test
    void stacksStayInRange() {
        assertEquals(AmountSteps.MAX, AmountSteps.plusStack(99990, 64));
        assertEquals(2, AmountSteps.plusStack(1, 1));
    }
}
