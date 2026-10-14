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

    private static int[] neighbours(int side, int pose, Object... more) {
        int[] result = new int[6];
        result[side] = pose;
        for (int i = 0; i < more.length; i += 2) {
            result[(Integer) more[i]] = (Integer) more[i + 1];
        }
        return result;
    }

    /**
     * Floor run going east, a climb at the step's west wall, then east on top
     * of the step: the duct at the top of the climb takes the wall from below
     * and the floor height from the east.
     */
    @Test
    void stepEdgeDrapesBothAxes() {
        int foot = DuctPose.base(WEST | UP, DOWN | EAST, false);
        assertAxis(foot, X, Side.POS, Rank.ANCHORED);
        assertAxis(foot, Y, Side.NEG, Rank.ANCHORED);
        int onStep = DuctPose.base(WEST | EAST, DOWN, false);
        int topBase = DuctPose.base(DOWN | EAST, 0, false);
        assertEquals(DuctPose.CENTRED, topBase);

        int top = DuctPose.resting(topBase, DOWN | EAST, neighbours(DuctShapes.DOWN, foot, DuctShapes.EAST, onStep));
        assertAxis(top, X, Side.POS, Rank.DRAPED);
        assertAxis(top, Y, Side.NEG, Rank.DRAPED);
        assertAxis(top, Z, Side.MID, Rank.FREE);
    }

    @Test
    void disagreeingNeighboursStayMiddle() {
        int eastWall = DuctPose.base(UP | DOWN | NORTH, EAST, false);
        int westWall = DuctPose.base(UP | DOWN | SOUTH, WEST, false);
        int pose = DuctPose.resting(DuctPose.CENTRED, NORTH | SOUTH,
                neighbours(DuctShapes.NORTH, westWall, DuctShapes.SOUTH, eastWall));
        assertAxis(pose, X, Side.MID, Rank.FREE);
    }

    @Test
    void drapedNeighbourDoesNotRequest() {
        int draped = DuctPose.with(DuctPose.CENTRED, Y, Side.NEG, Rank.DRAPED);
        int pose = DuctPose.resting(DuctPose.CENTRED, WEST, neighbours(DuctShapes.WEST, draped));
        assertEquals(DuctPose.CENTRED, pose);
    }

    @Test
    void anchoredAxisIsNotDraped() {
        int floor = DuctPose.base(WEST | EAST, DOWN, false);
        int panelled = DuctPose.base(WEST, 0, true);
        int pose = DuctPose.resting(floor, WEST, neighbours(DuctShapes.WEST, panelled));
        assertAxis(pose, Y, Side.NEG, Rank.ANCHORED);
        assertAxis(pose, Z, Side.MID, Rank.DRAPED);
    }

    /** A run over a three block gap: both ends drape to the floor height, the middle one stays centred. */
    @Test
    void longSpanMiddleStaysCentred() {
        int floor = DuctPose.base(WEST | EAST, DOWN, false);
        int gap = DuctPose.base(WEST | EAST, 0, false);
        int first = DuctPose.resting(gap, WEST | EAST, neighbours(DuctShapes.WEST, floor, DuctShapes.EAST, gap));
        int middle = DuctPose.resting(gap, WEST | EAST, neighbours(DuctShapes.WEST, gap, DuctShapes.EAST, gap));
        int last = DuctPose.resting(gap, WEST | EAST, neighbours(DuctShapes.WEST, gap, DuctShapes.EAST, floor));
        assertAxis(first, Y, Side.NEG, Rank.DRAPED);
        assertEquals(DuctPose.CENTRED, middle);
        assertAxis(last, Y, Side.NEG, Rank.DRAPED);
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
