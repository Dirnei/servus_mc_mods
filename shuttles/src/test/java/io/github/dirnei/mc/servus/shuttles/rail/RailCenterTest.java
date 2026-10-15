package io.github.dirnei.mc.servus.shuttles.rail;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.shuttles.rail.RailCenter.Shape;
import org.junit.jupiter.api.Test;

class RailCenterTest {

    @Test
    void loneRailRunsNorthSouth() {
        assertEquals(new RailCenter(Shape.STRAIGHT, 0), RailCenter.of(false, false, false, false));
    }

    @Test
    void straightRunsFollowTheirAxis() {
        assertEquals(new RailCenter(Shape.STRAIGHT, 0), RailCenter.of(true, false, true, false));
        assertEquals(new RailCenter(Shape.STRAIGHT, 90), RailCenter.of(false, true, false, true));
    }

    @Test
    void singleSideRunsAlongItsAxis() {
        assertEquals(new RailCenter(Shape.STRAIGHT, 0), RailCenter.of(false, false, true, false));
        assertEquals(new RailCenter(Shape.STRAIGHT, 90), RailCenter.of(false, false, false, true));
    }

    @Test
    void cornersCurveClockwiseFromNorthEast() {
        assertEquals(new RailCenter(Shape.CURVE, 0), RailCenter.of(true, true, false, false));
        assertEquals(new RailCenter(Shape.CURVE, 90), RailCenter.of(false, true, true, false));
        assertEquals(new RailCenter(Shape.CURVE, 180), RailCenter.of(false, false, true, true));
        assertEquals(new RailCenter(Shape.CURVE, 270), RailCenter.of(true, false, false, true));
    }

    @Test
    void threeOrFourSidesCross() {
        assertEquals(new RailCenter(Shape.CROSS, 0), RailCenter.of(true, true, false, true));
        assertEquals(new RailCenter(Shape.CROSS, 0), RailCenter.of(true, true, true, true));
    }
}
