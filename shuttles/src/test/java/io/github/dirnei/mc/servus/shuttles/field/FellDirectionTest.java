package io.github.dirnei.mc.servus.shuttles.field;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FellDirectionTest {

    @Test
    void bothSidesOfANorthSouthRail() {
        assertEquals(FellDirection.Way.EAST, FellDirection.away(2, 1, false));
        assertEquals(FellDirection.Way.WEST, FellDirection.away(-2, 0, false));
    }

    @Test
    void eastWestRail() {
        assertEquals(FellDirection.Way.NORTH, FellDirection.away(0, -2, true));
        assertEquals(FellDirection.Way.SOUTH, FellDirection.away(-1, 3, true));
    }

    @Test
    void cornerPlotFollowsTheLargerOffset() {
        assertEquals(FellDirection.Way.SOUTH, FellDirection.away(1, 2, false));
        assertEquals(FellDirection.Way.WEST, FellDirection.away(-3, -2, true));
    }

    @Test
    void tieGoesAcrossTheRun() {
        assertEquals(FellDirection.Way.EAST, FellDirection.away(2, -2, false));
        assertEquals(FellDirection.Way.NORTH, FellDirection.away(2, -2, true));
    }
}
