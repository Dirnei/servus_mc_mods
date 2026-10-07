package io.github.dirnei.mc.servus.logistics.duct;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ArmHitTest {

    /** Thin centre, thin arms north and east. */
    private static final int[] ARMS = {0, 0, 2, 0, 0, 2};

    @Test
    void pointInTheNorthArm() {
        assertEquals(DuctShapes.NORTH, ArmHit.hit(8, 8, 1, 2, ARMS));
        assertEquals(DuctShapes.NORTH, ArmHit.hit(6, 9, 3, 2, ARMS), "a point on the arm's west face counts");
    }

    @Test
    void pointInTheCentre() {
        assertEquals(ArmHit.CENTER, ArmHit.hit(8, 8, 8, 2, ARMS));
        assertEquals(ArmHit.CENTER, ArmHit.hit(8, 10, 8, 2, ARMS), "the centre's top face");
    }

    @Test
    void sharedFaceGoesToTheArm() {
        assertEquals(DuctShapes.NORTH, ArmHit.hit(8, 8, 6, 2, ARMS), "the face the north arm shares with the centre is the arm");
        assertEquals(DuctShapes.EAST, ArmHit.hit(10, 8, 8, 2, ARMS));
    }

    @Test
    void pointOnASideWithoutAnArm() {
        assertEquals(ArmHit.NONE, ArmHit.hit(8, 8, 14, 2, ARMS), "south has no arm");
        assertEquals(ArmHit.NONE, ArmHit.hit(2, 8, 8, 2, ARMS), "west has no arm");
        assertEquals(ArmHit.NONE, ArmHit.hit(1, 1, 1, 3, new int[6]), "a corner of the block is nothing");
    }

    @Test
    void thickArmsUseTheirOwnRadius() {
        int[] thick = {0, 0, 3, 0, 0, 0};
        assertEquals(DuctShapes.NORTH, ArmHit.hit(5, 8, 2, 3, thick));
        assertEquals(ArmHit.NONE, ArmHit.hit(5, 8, 2, 2, ARMS), "outside a thin arm");
    }
}
