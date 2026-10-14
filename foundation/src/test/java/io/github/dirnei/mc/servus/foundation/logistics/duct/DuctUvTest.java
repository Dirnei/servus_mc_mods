package io.github.dirnei.mc.servus.foundation.logistics.duct;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctUv.Uv;
import org.junit.jupiter.api.Test;

class DuctUvTest {

    private static final int X = DuctPose.X;
    private static final int Y = DuctPose.Y;
    private static final int Z = DuctPose.Z;
    private static final float[] MIDDLE = {8, 8, 8};

    private static Uv side(float x, float y, float z, int normal, int run, float[] middle) {
        return DuctUv.side(new float[] {x, y, z}, normal, run, middle);
    }

    /** The east edge of one block and the west edge of the next sample the two ends of one tile. */
    @Test
    void uContinuesAcrossBlockBorder() {
        assertEquals(16, side(16, 10, 8, Y, X, MIDDLE).u());
        assertEquals(0, side(0, 10, 8, Y, X, MIDDLE).u());
    }

    /** An arm starting on the centre's face samples the same column as the centre's edge there. */
    @Test
    void uContinuesAtArmToCentreJoint() {
        float[] armMiddle = {13, 8, 8};
        assertEquals(side(10, 10, 8, Y, X, MIDDLE).u(), side(10, 10, 8, Y, X, armMiddle).u());
        assertEquals(side(10, 10, 8, Y, X, MIDDLE).v(), side(10, 10, 8, Y, X, armMiddle).v());
    }

    /** The four side faces of a run along X sample the same u at the same point along the run, with v in world direction. */
    @Test
    void sameOnAllFourSides() {
        Uv top = side(5, 10, 6, Y, X, MIDDLE);
        Uv bottom = side(5, 6, 6, Y, X, MIDDLE);
        Uv north = side(5, 6, 6, Z, X, MIDDLE);
        Uv south = side(5, 6, 10, Z, X, MIDDLE);
        assertEquals(5, top.u());
        assertEquals(top.u(), bottom.u());
        assertEquals(top.u(), north.u());
        assertEquals(top.u(), south.u());
        assertEquals(top.v(), bottom.v(), "top and bottom share the across direction");
        assertEquals(north.v(), south.v(), "north and south share the up direction");
        assertEquals(6, north.v(), "a lower point samples a lower row on both sides");
    }

    /** Faces 4, 6 and 8 px thick show the rows around row 8, at the middle and at the low pose. */
    @Test
    void stripRowsByThickness() {
        for (int thickness : new int[] {4, 6, 8}) {
            float half = thickness / 2F;
            for (float low : new float[] {8 - half, DuctShapes.LIFT}) {
                float[] middle = {8, low + half, 8};
                assertEquals(8 - half, side(4, low, 8 - half, Z, X, middle).v(), 1e-4, "bottom row at thickness " + thickness);
                assertEquals(8 + half, side(4, low + thickness, 8 - half, Z, X, middle).v(), 1e-4, "top row at thickness " + thickness);
            }
        }
    }

    /** The first band, on the negative across side, samples the top rows from above and from below. */
    @Test
    void bandOrderFollowsWorldAcross() {
        Uv topFirst = side(4, 12, 4, Y, X, new float[] {8, 8, 8});
        Uv bottomFirst = side(4, 4, 4, Y, X, new float[] {8, 8, 8});
        assertEquals(4, topFirst.v());
        assertEquals(topFirst.v(), bottomFirst.v());
        Uv vertical = side(4, 3, 8, Z, Y, MIDDLE);
        assertEquals(4, vertical.v(), "a run along Y has its bands across X");
        assertEquals(3, vertical.u(), "and runs along Y");
    }

    /** The end face of a run along Z shows its bands side by side across X, in the same order as the top face. */
    @Test
    void endFaceShowsBandsSideBySide() {
        Uv end = side(5, 7, 16, Z, Z, MIDDLE);
        Uv top = side(5, 11, 3, Y, Z, MIDDLE);
        assertEquals(5, end.v(), "the first band lies on the negative X side");
        assertEquals(top.v(), end.v(), "the same band row as on top");
        assertEquals(7, end.u());
        Uv endOfX = side(16, 7, 4, X, X, MIDDLE);
        assertEquals(4, endOfX.v(), "a run along X has its bands across Z");
    }

    /** A grown fitting face 4 px wide and 10 px tall covers the whole square of radius 2. */
    @Test
    void fittingFaceCoversItsTexture() {
        float[] min = {6, DuctShapes.LIFT, 6};
        float[] max = {10, 10, 10};
        Uv corner = DuctUv.fitting(new float[] {6, DuctShapes.LIFT, 10}, Z, min, max, 2);
        Uv opposite = DuctUv.fitting(new float[] {10, 10, 10}, Z, min, max, 2);
        assertEquals(6, corner.u(), 1e-4);
        assertEquals(6, corner.v(), 1e-4);
        assertEquals(10, opposite.u(), 1e-4);
        assertEquals(10, opposite.v(), 1e-4);
    }
}
