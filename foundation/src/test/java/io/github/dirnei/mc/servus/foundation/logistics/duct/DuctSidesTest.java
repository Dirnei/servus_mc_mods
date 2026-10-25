package io.github.dirnei.mc.servus.foundation.logistics.duct;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DuctSidesTest {

    private static final int UP = 1;
    private static final int NORTH = 2;
    private static final int EAST = 5;

    private static boolean[] selectable(int... sides) {
        boolean[] flags = new boolean[6];
        for (int side : sides) {
            flags[side] = true;
        }
        return flags;
    }

    @Test
    void clickedSelectableSideWins() {
        assertEquals(NORTH, DuctSides.startSide(selectable(UP, NORTH), NORTH));
    }

    @Test
    void clickedSideNotSelectableFallsToFirst() {
        assertEquals(UP, DuctSides.startSide(selectable(UP, NORTH), EAST));
    }

    @Test
    void centreClickTakesFirst() {
        assertEquals(UP, DuctSides.startSide(selectable(NORTH, UP), DuctSides.NONE));
    }

    @Test
    void noneSelectable() {
        assertEquals(DuctSides.NONE, DuctSides.startSide(selectable(), NORTH));
        assertEquals(DuctSides.NONE, DuctSides.startSide(selectable(), DuctSides.NONE));
    }
}
