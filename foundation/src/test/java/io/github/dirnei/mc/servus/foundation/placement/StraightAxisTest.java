package io.github.dirnei.mc.servus.foundation.placement;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class StraightAxisTest {

    private static final int DOWN = 0;
    private static final int UP = 1;
    private static final int NORTH = 2;
    private static final int SOUTH = 3;
    private static final int WEST = 4;
    private static final int EAST = 5;

    private static int mask(int... sides) {
        int mask = 0;
        for (int side : sides) {
            mask |= 1 << side;
        }
        return mask;
    }

    @Test
    void topOfAnEastWestRunFollowsTheHalf() {
        assertEquals(OptionalInt.of(EAST), StraightAxis.along(mask(WEST, EAST), UP, 0.7, 1.0, 0.1));
        assertEquals(OptionalInt.of(WEST), StraightAxis.along(mask(WEST, EAST), UP, 0.3, 1.0, 0.9));
        assertEquals(OptionalInt.of(EAST), StraightAxis.along(mask(WEST, EAST), NORTH, 0.5, 0.5, 0.0));
    }

    @Test
    void endCapGivesNothing() {
        assertEquals(OptionalInt.empty(), StraightAxis.along(mask(WEST, EAST), EAST, 1.0, 0.5, 0.5));
    }

    @Test
    void verticalRunSideHitGoesByHeight() {
        assertEquals(OptionalInt.of(UP), StraightAxis.along(mask(DOWN, UP), SOUTH, 0.5, 0.8, 1.0));
        assertEquals(OptionalInt.of(DOWN), StraightAxis.along(mask(DOWN, UP), SOUTH, 0.5, 0.2, 1.0));
    }

    @Test
    void noConnectionsGiveNothing() {
        assertEquals(OptionalInt.empty(), StraightAxis.along(0, UP, 0.7, 1.0, 0.5));
    }

    @Test
    void twoAxesGiveNothing() {
        assertEquals(OptionalInt.empty(), StraightAxis.along(mask(WEST, NORTH), UP, 0.7, 1.0, 0.5));
    }

    @Test
    void singleArmGivesItsAxis() {
        assertEquals(OptionalInt.of(SOUTH), StraightAxis.along(mask(NORTH), UP, 0.5, 1.0, 0.6));
        assertEquals(OptionalInt.of(NORTH), StraightAxis.along(mask(NORTH), WEST, 0.0, 0.5, 0.4));
    }
}
