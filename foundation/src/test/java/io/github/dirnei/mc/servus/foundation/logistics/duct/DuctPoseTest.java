package io.github.dirnei.mc.servus.foundation.logistics.duct;

import static io.github.dirnei.mc.servus.foundation.logistics.duct.DuctPose.X;
import static io.github.dirnei.mc.servus.foundation.logistics.duct.DuctPose.Y;
import static io.github.dirnei.mc.servus.foundation.logistics.duct.DuctPose.Z;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctPose.Rank;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctPose.Side;
import org.junit.jupiter.api.Test;

class DuctPoseTest {

    private static final int DOWN = 1 << DuctShapes.DOWN;
    private static final int UP = 1 << DuctShapes.UP;
    private static final int NORTH = 1 << DuctShapes.NORTH;
    private static final int SOUTH = 1 << DuctShapes.SOUTH;
    private static final int WEST = 1 << DuctShapes.WEST;
    private static final int EAST = 1 << DuctShapes.EAST;

    private static void assertAxis(int pose, int axis, Side side, Rank rank) {
        assertEquals(side, DuctPose.side(pose, axis), "side on axis " + axis);
        assertEquals(rank, DuctPose.rank(pose, axis), "rank on axis " + axis);
    }

    @Test
    void floorLiesLow() {
        int pose = DuctPose.base(WEST | EAST, DOWN, false);
        assertAxis(pose, Y, Side.NEG, Rank.ANCHORED);
        assertAxis(pose, X, Side.MID, Rank.FREE);
        assertAxis(pose, Z, Side.MID, Rank.FREE);
    }

    @Test
    void noFloorStaysMiddle() {
        assertEquals(DuctPose.CENTRED, DuctPose.base(WEST | EAST, 0, false));
        assertEquals(DuctPose.CENTRED, DuctPose.base(WEST | EAST, UP, false), "a ceiling does not lift the duct");
    }

    @Test
    void armIntoBelowIsNotFloor() {
        assertAxis(DuctPose.base(DOWN | EAST, DOWN, false), Y, Side.MID, Rank.FREE);
    }

    @Test
    void climbHugsWall() {
        int pose = DuctPose.base(UP | DOWN, WEST, false);
        assertAxis(pose, X, Side.NEG, Rank.ANCHORED);
        assertAxis(pose, Z, Side.MID, Rank.FREE);
        assertAxis(DuctPose.base(UP, SOUTH, false), Z, Side.POS, Rank.ANCHORED);
    }

    @Test
    void climbInCornerHugsBoth() {
        int pose = DuctPose.base(UP | DOWN, NORTH | WEST, false);
        assertAxis(pose, X, Side.NEG, Rank.ANCHORED);
        assertAxis(pose, Z, Side.NEG, Rank.ANCHORED);
    }

    @Test
    void oppositeWallsStayMiddle() {
        int pose = DuctPose.base(UP | DOWN, WEST | EAST, false);
        assertAxis(pose, X, Side.MID, Rank.ANCHORED);
        assertAxis(pose, Z, Side.MID, Rank.FREE);
    }

    @Test
    void horizontalRunIgnoresWall() {
        int pose = DuctPose.base(WEST | EAST, DOWN | NORTH, false);
        assertAxis(pose, Z, Side.MID, Rank.FREE);
        assertAxis(pose, Y, Side.NEG, Rank.ANCHORED);
    }

    @Test
    void armIntoWallIsNotWall() {
        int pose = DuctPose.base(UP | EAST, EAST, false);
        assertAxis(pose, X, Side.MID, Rank.FREE);
    }

    @Test
    void panelForcesMiddle() {
        int pose = DuctPose.base(UP | DOWN, DOWN | WEST, true);
        for (int axis = X; axis <= Z; axis++) {
            assertAxis(pose, axis, Side.MID, Rank.ANCHORED);
        }
    }

    @Test
    void packRoundTrip() {
        int pose = DuctPose.CENTRED;
        pose = DuctPose.with(pose, X, Side.POS, Rank.DRAPED);
        pose = DuctPose.with(pose, Y, Side.NEG, Rank.ANCHORED);
        pose = DuctPose.with(pose, Z, Side.MID, Rank.FREE);
        assertAxis(pose, X, Side.POS, Rank.DRAPED);
        assertAxis(pose, Y, Side.NEG, Rank.ANCHORED);
        assertAxis(pose, Z, Side.MID, Rank.FREE);
        pose = DuctPose.with(pose, X, Side.NEG, Rank.FREE);
        assertAxis(pose, X, Side.NEG, Rank.FREE);
        assertAxis(pose, Y, Side.NEG, Rank.ANCHORED);
        assertEquals(DuctPose.sides(pose), DuctPose.sides(DuctPose.with(pose, Y, Side.NEG, Rank.FREE)), "sides ignore ranks");
    }
}
