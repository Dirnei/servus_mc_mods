package io.github.dirnei.mc.servus.manufacturing.alchemy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AshCountTest {

    @Test
    void wholeAsh() {
        assertEquals(3, AshCount.ash(300, 100));
        assertEquals(300, AshCount.saltFor(3, 300, 100));
    }

    @Test
    void remainderStays() {
        assertEquals(2, AshCount.ash(250, 100));
        assertEquals(200, AshCount.saltFor(5, 250, 100));
    }

    @Test
    void nothingBelowOneAsh() {
        assertEquals(0, AshCount.ash(99, 100));
        assertEquals(0, AshCount.saltFor(1, 99, 100));
        assertEquals(0, AshCount.ash(0, 100));
    }

    @Test
    void neverNegative() {
        assertEquals(0, AshCount.ash(-50, 100));
        assertEquals(0, AshCount.ash(500, 0));
        assertEquals(0, AshCount.saltFor(-2, 500, 100));
        assertEquals(0, AshCount.saltFor(2, 500, -100));
    }
}
