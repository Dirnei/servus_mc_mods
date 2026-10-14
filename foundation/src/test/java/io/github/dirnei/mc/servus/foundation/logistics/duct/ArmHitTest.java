package io.github.dirnei.mc.servus.foundation.logistics.duct;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ArmHitTest {

    /** Thin centre, thin arms north and east. */
    private static final int[] ARMS = {0, 0, 2, 0, 0, 2};

    @Test
    void pointInTheNorthArm() {
        assertEquals(DuctShapes.NORTH, ArmHit.hit(8, 8, 1, 2, ARMS, DuctPose.CENTRED, 0));
        assertEquals(DuctShapes.NORTH, ArmHit.hit(6, 9, 3, 2, ARMS, DuctPose.CENTRED, 0), "a point on the arm's west face counts");
    }

    @Test
    void pointInTheCentre() {
        assertEquals(ArmHit.CENTER, ArmHit.hit(8, 8, 8, 2, ARMS, DuctPose.CENTRED, 0));
        assertEquals(ArmHit.CENTER, ArmHit.hit(8, 10, 8, 2, ARMS, DuctPose.CENTRED, 0), "the centre's top face");
    }

    @Test
    void sharedFaceGoesToTheArm() {
        assertEquals(DuctShapes.NORTH, ArmHit.hit(8, 8, 6, 2, ARMS, DuctPose.CENTRED, 0), "the face the north arm shares with the centre is the arm");
        assertEquals(DuctShapes.EAST, ArmHit.hit(10, 8, 8, 2, ARMS, DuctPose.CENTRED, 0));
    }

    @Test
    void pointOnASideWithoutAnArm() {
        assertEquals(ArmHit.NONE, ArmHit.hit(8, 8, 14, 2, ARMS, DuctPose.CENTRED, 0), "south has no arm");
        assertEquals(ArmHit.NONE, ArmHit.hit(2, 8, 8, 2, ARMS, DuctPose.CENTRED, 0), "west has no arm");
        assertEquals(ArmHit.NONE, ArmHit.hit(1, 1, 1, 3, new int[6], DuctPose.CENTRED, 0), "a corner of the block is nothing");
    }

    @Test
    void thickArmsUseTheirOwnRadius() {
        int[] thick = {0, 0, 3, 0, 0, 0};
        assertEquals(DuctShapes.NORTH, ArmHit.hit(5, 8, 2, 3, thick, DuctPose.CENTRED, 0));
        assertEquals(ArmHit.NONE, ArmHit.hit(5, 8, 2, 2, ARMS, DuctPose.CENTRED, 0), "outside a thin arm");
    }

    /** A thin floor duct with an arm east into a chest: the arm steps up to the middle of the face. */
    private static final int FLOOR = DuctPose.of(DuctPose.Height.LOW, DuctPose.Rank.ANCHORED);
    private static final int EAST_ENDPOINT = DuctPose.faces(FLOOR, 1 << DuctShapes.EAST, 1 << DuctShapes.EAST, new int[6]);
    private static final int[] EAST_ARM = {0, 0, 0, 0, 0, 2};

    @Test
    void hitOnStepCountsAsArm() {
        assertEquals(DuctShapes.EAST, ArmHit.hit(12, 8, 8, 2, EAST_ARM, FLOOR, EAST_ENDPOINT), "above the low centre, on the step");
    }

    @Test
    void hitOnLowSegmentCountsAsArm() {
        int[] west = {0, 0, 0, 0, 2, 0};
        int faces = DuctPose.faces(FLOOR, 1 << DuctShapes.WEST, 0, new int[] {0, 0, 0, 0, FLOOR, 0});
        assertEquals(DuctShapes.WEST, ArmHit.hit(1, 1, 8, 2, west, FLOOR, faces), "the arm toward a floor neighbour lies low");
        assertEquals(ArmHit.NONE, ArmHit.hit(1, 8, 8, 2, west, FLOOR, faces), "nothing in the middle of the face");
        assertEquals(DuctShapes.EAST, ArmHit.hit(15.5, 8, 8, 2, EAST_ARM, FLOOR, EAST_ENDPOINT), "the centred end at the chest");
    }

    @Test
    void hitAtMovedCentreIsCentre() {
        assertEquals(ArmHit.CENTER, ArmHit.hit(8, 2, 8, 2, EAST_ARM, FLOOR, EAST_ENDPOINT));
        assertEquals(ArmHit.NONE, ArmHit.hit(8, 8, 8, 2, EAST_ARM, FLOOR, EAST_ENDPOINT), "the middle of the block is empty");
    }
}
