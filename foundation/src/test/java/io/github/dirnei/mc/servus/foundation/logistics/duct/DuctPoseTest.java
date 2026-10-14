package io.github.dirnei.mc.servus.foundation.logistics.duct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctPose.Height;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctPose.Rank;
import org.junit.jupiter.api.Test;

class DuctPoseTest {

    private static final int DOWN = 1 << DuctShapes.DOWN;
    private static final int UP = 1 << DuctShapes.UP;
    private static final int NORTH = 1 << DuctShapes.NORTH;
    private static final int SOUTH = 1 << DuctShapes.SOUTH;
    private static final int WEST = 1 << DuctShapes.WEST;
    private static final int EAST = 1 << DuctShapes.EAST;

    private static final int FLOOR = DuctPose.base(WEST | EAST, 0, true, false);
    private static final int PANELLED = DuctPose.base(WEST | EAST, 0, false, true);

    private static void assertPose(int pose, Height height, Rank rank) {
        assertEquals(height, DuctPose.height(pose), "height");
        assertEquals(rank, DuctPose.rank(pose), "rank");
    }

    @Test
    void floorLiesLow() {
        assertPose(FLOOR, Height.LOW, Rank.ANCHORED);
    }

    @Test
    void noFloorStaysMiddle() {
        assertEquals(DuctPose.CENTRED, DuctPose.base(WEST | EAST, 0, false, false));
    }

    @Test
    void armIntoBelowIsNotFloor() {
        assertEquals(DuctPose.CENTRED, DuctPose.base(DOWN | EAST, 0, true, false));
    }

    /** A floor run going east turns up: the bend lies low with the run, so the run has no step before it. */
    @Test
    void floorRunTurningUpLiesLow() {
        int bend = DuctPose.base(WEST | UP, 0, true, false);
        assertPose(bend, Height.LOW, Rank.ANCHORED);
        assertEquals(Height.LOW, face(bend, FLOOR, DuctShapes.WEST), "the bend meets the floor run low");
        assertEquals(Height.LOW, face(FLOOR, bend, DuctShapes.EAST), "the floor run meets the bend low");
        int above = drape(DuctPose.base(UP | DOWN, 0, false, false), UP | DOWN, neighbours(DuctShapes.DOWN, bend));
        assertEquals(DuctPose.CENTRED, above, "the climb above the bend stays centred");
    }

    /** A duct on the floor feeds a machine to its east and runs straight up: it does not lie down. */
    @Test
    void machineOnFloorPipeGoesStraightUp() {
        int base = DuctPose.base(EAST | UP, EAST, true, false);
        assertEquals(DuctPose.CENTRED, base);
        assertEquals(DuctPose.CENTRED, DuctPose.resting(base, EAST | UP, UP, new int[6]));
    }

    /** A floor run going east ends in a machine and turns up in the same duct: that duct stays centred. */
    @Test
    void machineArmAndArmUpStaysMiddle() {
        int base = DuctPose.base(WEST | EAST | UP, EAST, true, false);
        assertEquals(DuctPose.CENTRED, base);
        assertEquals(DuctPose.CENTRED, DuctPose.resting(base, WEST | EAST | UP, WEST | UP, neighbours(DuctShapes.WEST, FLOOR)));
        assertEquals(Height.LOW, face(base, FLOOR, DuctShapes.WEST), "the floor run steps up into it");
    }

    @Test
    void machineArmAndArmUpDoesNotDrape() {
        int pose = DuctPose.resting(DuctPose.CENTRED, WEST | EAST | UP, WEST | UP, neighbours(DuctShapes.WEST, FLOOR));
        assertEquals(DuctPose.CENTRED, pose);
    }

    @Test
    void armUpDrapes() {
        assertPose(drape(DuctPose.CENTRED, WEST | UP, neighbours(DuctShapes.WEST, FLOOR)), Height.LOW, Rank.DRAPED);
    }

    @Test
    void armUpAloneOnFloorLiesLow() {
        assertPose(DuctPose.base(UP, 0, true, false), Height.LOW, Rank.ANCHORED);
    }

    @Test
    void verticalEndpointDoesNotKeepMiddle() {
        assertFalse(DuctPose.risesFromEndpoint(UP | WEST, UP), "an endpoint above is no machine beside the duct");
        assertTrue(DuctPose.risesFromEndpoint(UP | NORTH, NORTH));
        assertFalse(DuctPose.risesFromEndpoint(DOWN | NORTH, NORTH), "without an arm up the duct may lie down");
    }

    @Test
    void armDownOnlyStillLiesLow() {
        assertPose(DuctPose.base(DOWN | EAST, 0, false, false), Height.MID, Rank.FREE);
        assertPose(drape(DuctPose.CENTRED, DOWN | EAST, neighbours(DuctShapes.EAST, FLOOR)), Height.LOW, Rank.DRAPED);
    }

    /** {@link DuctPose#resting} for a duct whose every arm leads to another duct. */
    private static int drape(int base, int ductArms, int[] neighbourBase) {
        return DuctPose.resting(base, ductArms, ductArms, neighbourBase);
    }

    @Test
    void panelForcesMiddle() {
        assertPose(DuctPose.base(UP | DOWN, 0, true, true), Height.MID, Rank.ANCHORED);
        assertPose(PANELLED, Height.MID, Rank.ANCHORED);
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
     * Floor run going east, a climb beside the step's west wall, then east on
     * top of the step: the duct at the top of the climb takes the floor height
     * of the run on the step.
     */
    @Test
    void stepEdgeDrapesHeight() {
        int foot = DuctPose.base(WEST | UP, 0, true, false);
        assertPose(foot, Height.LOW, Rank.ANCHORED);
        int topBase = DuctPose.base(DOWN | EAST, 0, false, false);
        assertEquals(DuctPose.CENTRED, topBase);
        int top = drape(topBase, DOWN | EAST, neighbours(DuctShapes.DOWN, foot, DuctShapes.EAST, FLOOR));
        assertPose(top, Height.LOW, Rank.DRAPED);
    }

    @Test
    void verticalNeighbourDoesNotRequest() {
        assertEquals(DuctPose.CENTRED, drape(DuctPose.CENTRED, DOWN, neighbours(DuctShapes.DOWN, FLOOR)));
    }

    @Test
    void disagreeingNeighboursStayMiddle() {
        int pose = drape(DuctPose.CENTRED, NORTH | SOUTH, neighbours(DuctShapes.NORTH, FLOOR, DuctShapes.SOUTH, PANELLED));
        assertEquals(DuctPose.CENTRED, pose);
    }

    @Test
    void drapedNeighbourDoesNotRequest() {
        int draped = DuctPose.of(Height.LOW, Rank.DRAPED);
        assertEquals(DuctPose.CENTRED, drape(DuctPose.CENTRED, WEST, neighbours(DuctShapes.WEST, draped)));
    }

    @Test
    void anchoredIsNotDraped() {
        assertEquals(FLOOR, drape(FLOOR, WEST, neighbours(DuctShapes.WEST, PANELLED)));
    }

    /** A run over a three block gap: both ends drape to the floor height, the middle one stays centred. */
    @Test
    void longSpanMiddleStaysCentred() {
        int gap = DuctPose.base(WEST | EAST, 0, false, false);
        int first = drape(gap, WEST | EAST, neighbours(DuctShapes.WEST, FLOOR, DuctShapes.EAST, gap));
        int middle = drape(gap, WEST | EAST, neighbours(DuctShapes.WEST, gap, DuctShapes.EAST, gap));
        int last = drape(gap, WEST | EAST, neighbours(DuctShapes.WEST, gap, DuctShapes.EAST, FLOOR));
        assertPose(first, Height.LOW, Rank.DRAPED);
        assertEquals(DuctPose.CENTRED, middle);
        assertPose(last, Height.LOW, Rank.DRAPED);
    }

    private static Height face(int own, int other, int side) {
        int faces = DuctPose.faces(own, 1 << side, 0, neighbours(side, other));
        return DuctPose.faceHeight(faces, side);
    }

    @Test
    void anchoredBeatsFree() {
        assertEquals(Height.LOW, face(FLOOR, DuctPose.CENTRED, DuctShapes.EAST));
        assertEquals(Height.LOW, face(DuctPose.CENTRED, FLOOR, DuctShapes.WEST));
    }

    @Test
    void drapedBeatsFree() {
        int draped = DuctPose.of(Height.LOW, Rank.DRAPED);
        assertEquals(Height.LOW, face(DuctPose.CENTRED, draped, DuctShapes.SOUTH));
        assertEquals(Height.LOW, face(draped, DuctPose.CENTRED, DuctShapes.NORTH));
    }

    @Test
    void tieGoesToNegativeSide() {
        assertEquals(Height.LOW, face(FLOOR, PANELLED, DuctShapes.SOUTH), "the own duct lies north, on the negative side");
        assertEquals(Height.MID, face(FLOOR, PANELLED, DuctShapes.WEST), "the other duct lies west");
    }

    @Test
    void bothSidesAgreeOnFace() {
        int[] poses = {DuctPose.CENTRED, FLOOR, DuctPose.of(Height.LOW, Rank.DRAPED), DuctPose.of(Height.MID, Rank.DRAPED), PANELLED};
        for (int a : poses) {
            for (int b : poses) {
                for (int side = DuctShapes.NORTH; side <= DuctShapes.EAST; side++) {
                    assertEquals(face(a, b, side), face(b, a, side ^ 1), "poses " + a + " and " + b + " across side " + side);
                }
            }
        }
    }

    @Test
    void endpointFaceIsMiddle() {
        int faces = DuctPose.faces(FLOOR, EAST, EAST, new int[6]);
        assertEquals(Height.MID, DuctPose.faceHeight(faces, DuctShapes.EAST));
    }

    @Test
    void verticalArmsHaveNoFace() {
        int draped = DuctPose.of(Height.LOW, Rank.DRAPED);
        assertEquals(0, DuctPose.faces(draped, UP | DOWN, 0, new int[] {FLOOR, FLOOR, 0, 0, 0, 0}));
    }

    @Test
    void packRoundTrip() {
        for (Height height : Height.values()) {
            for (Rank rank : Rank.values()) {
                assertPose(DuctPose.of(height, rank), height, rank);
            }
        }
        assertEquals(DuctPose.CENTRED, DuctPose.of(Height.MID, Rank.FREE));
    }
}
