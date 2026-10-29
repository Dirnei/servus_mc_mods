package io.github.dirnei.mc.servus.shuttles.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LocationCodeTest {

    @Test
    void northEastUp() {
        assertEquals("N12.E5+2", LocationCode.of(5, 2, -12));
    }

    @Test
    void southWestDown() {
        assertEquals("S7.W3-1", LocationCode.of(-3, -1, 7));
    }

    @Test
    void zeroDistancesAreNorthAndEast() {
        assertEquals("N8.E0+0", LocationCode.of(0, 0, -8));
        assertEquals("N0.W4+0", LocationCode.of(-4, 0, 0));
        assertEquals("N0.E0+0", LocationCode.of(0, 0, 0));
    }

    @Test
    void zeroHeightKeepsItsSign() {
        assertEquals("S2.E1+0", LocationCode.of(1, 0, 2));
    }

    @Test
    void negativeHeight() {
        assertEquals("N1.E1-12", LocationCode.of(1, -12, -1));
    }
}
