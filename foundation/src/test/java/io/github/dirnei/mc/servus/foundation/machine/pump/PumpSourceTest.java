package io.github.dirnei.mc.servus.foundation.machine.pump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PumpSourceTest {

    private static final int MIN = 2;

    private static boolean[] neighbours(int sources) {
        boolean[] flags = new boolean[4];
        for (int i = 0; i < sources; i++) {
            flags[i] = true;
        }
        return flags;
    }

    @Test
    void noNeighbourIsNotInfinite() {
        assertFalse(PumpSource.isInfinite(true, neighbours(0), MIN));
    }

    @Test
    void oneNeighbourIsNotInfinite() {
        assertFalse(PumpSource.isInfinite(true, neighbours(1), MIN));
    }

    @Test
    void twoNeighboursAreInfinite() {
        assertTrue(PumpSource.isInfinite(true, neighbours(2), MIN));
    }

    @Test
    void threeNeighboursAreInfinite() {
        assertTrue(PumpSource.isInfinite(true, neighbours(3), MIN));
    }

    @Test
    void fourNeighboursAreInfinite() {
        assertTrue(PumpSource.isInfinite(true, neighbours(4), MIN));
    }

    @Test
    void noSourceBelowIsNeverInfinite() {
        assertFalse(PumpSource.isInfinite(false, neighbours(4), MIN));
    }

    @Test
    void neighboursCountInAnyPosition() {
        assertEquals(2, PumpSource.count(new boolean[] {false, true, false, true}));
        assertTrue(PumpSource.isInfinite(true, new boolean[] {false, true, false, true}, MIN));
    }
}
