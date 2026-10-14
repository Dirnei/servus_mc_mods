package io.github.dirnei.mc.servus.manufacturing.alchemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void fitsUpToAFullStack() {
        assertTrue(AshCount.fits(6300, 100, 64, 100), "exactly 64 Ash fit");
        assertFalse(AshCount.fits(6300, 101, 64, 100), "1 mB over does not fit");
        assertTrue(AshCount.fits(0, 6400, 64, 100));
    }

    @Test
    void zeroRoomCountsAsAStack() {
        assertEquals(6400, AshCount.room(0, 100));
        assertEquals(6400, AshCount.room(-3, 100));
        assertTrue(AshCount.fits(6000, 400, 0, 100));
    }

    @Test
    void unevenSaltPerAsh() {
        assertEquals(4800, AshCount.room(64, 75));
        assertTrue(AshCount.fits(4750, 50, 64, 75));
        assertFalse(AshCount.fits(4750, 51, 64, 75));
        assertEquals(1600, AshCount.room(32, 50));
    }
}
