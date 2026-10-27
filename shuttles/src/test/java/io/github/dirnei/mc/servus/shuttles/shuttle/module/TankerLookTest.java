package io.github.dirnei.mc.servus.shuttles.shuttle.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TankerLookTest {

    @Test
    void couplingSlidesOutHoldsAndSlidesBack() {
        assertEquals(0, TankerLook.extension(40, 0), 1e-6);
        assertEquals(0.5F, TankerLook.extension(40, TankerLook.COUPLING_TICKS / 2F), 1e-6);
        assertEquals(1, TankerLook.extension(40, 20), 1e-6);
        assertEquals(0, TankerLook.extension(40, 40), 1e-6);
    }

    @Test
    void shortStopSplitsItsTime() {
        assertEquals(1, TankerLook.extension(8, 4), 1e-6);
        assertEquals(0, TankerLook.extension(0, 0), 1e-6);
    }

    @Test
    void extendedHeadEndsJustShortOfTheBlockEdge() {
        float outerFace = TankerLook.HEAD_X0 - TankerLook.reach();
        float inBlock = 8 + (outerFace - 8) * TankerLook.SCALE;
        assertEquals(-TankerLook.COUPLING_GAP, inBlock, 1e-4);
    }

    @Test
    void stowedHeadSitsInsideTheCollar() {
        assertTrue(TankerLook.HEAD_X0 > TankerLook.COLLAR_X0 && TankerLook.HEAD_X1 < TankerLook.COLLAR_X1);
        assertTrue(TankerLook.TUBE_INNER > TankerLook.COLLAR_X0 && TankerLook.TUBE_INNER < TankerLook.COLLAR_X1);
        assertTrue(TankerLook.TUBE_HALF < TankerLook.HEAD_HALF && TankerLook.HEAD_HALF < TankerLook.COLLAR_HALF);
    }
}
