package io.github.dirnei.mc.servus.storage.site;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WirelessCostTest {

    @Test
    void costGrowsWithDistance() {
        assertEquals(14.0, WirelessCost.perItem(500, 4, 0.02), 1e-9);
        assertEquals(140, WirelessCost.cost(10, WirelessCost.perItem(500, 4, 0.02)));
    }

    @Test
    void fractionsRoundUp() {
        assertEquals(5, WirelessCost.cost(1, 4.2));
        assertEquals(42, WirelessCost.cost(10, 4.2));
        assertEquals(0, WirelessCost.cost(0, 4.2));
    }

    @Test
    void emptyBufferPaysNothing() {
        assertEquals(0, WirelessCost.affordable(0, 4, 64));
        assertEquals(64, WirelessCost.affordable(0, 0, 64), "free transfers need no FE");
    }

    @Test
    void bufferPaysWhatItCan() {
        assertEquals(10, WirelessCost.affordable(140, 14, 64));
        assertEquals(9, WirelessCost.affordable(139, 14, 64));
        assertEquals(64, WirelessCost.affordable(100000, 14, 64));
        assertEquals(4, WirelessCost.affordable(17, 4.2, 64), "4 items cost 17 FE rounded up");
    }

    @Test
    void movableIsTheSmallestLimit() {
        assertEquals(3, WirelessCost.movable(10, 20, 3, 8, 64, 64));
        assertEquals(2, WirelessCost.movable(10, 20, 8, 8, 2, 64));
        assertEquals(1, WirelessCost.movable(10, 20, 8, 8, 64, 1));
        assertEquals(5, WirelessCost.movable(10, 5, 8, 8, 64, 64));
        assertEquals(0, WirelessCost.movable(10, 5, -3, 8, 64, 64));
    }

    @Test
    void negativeInputsCountAsZero() {
        assertEquals(0.0, WirelessCost.perItem(-5, -1, -1), 1e-9);
    }
}
