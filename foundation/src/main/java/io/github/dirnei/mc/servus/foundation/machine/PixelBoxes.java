package io.github.dirnei.mc.servus.foundation.machine;

/**
 * Turns part boxes and points given in pixels, {x1, y1, z1, x2, y2, z2}, of
 * models built facing north the way a blockstate's y rotation turns the model.
 * Pure Java.
 */
public final class PixelBoxes {

    private PixelBoxes() {
    }

    /**
     * A box turned clockwise seen from above by {@code quarters} quarter turns,
     * like a blockstate's y rotation: north goes to east.
     */
    public static float[] rotated(float[] box, int quarters) {
        float x1 = box[0];
        float z1 = box[2];
        float x2 = box[3];
        float z2 = box[5];
        for (int i = 0; i < Math.floorMod(quarters, 4); i++) {
            float nx1 = 16 - z2;
            float nx2 = 16 - z1;
            z1 = x1;
            z2 = x2;
            x1 = nx1;
            x2 = nx2;
        }
        return new float[] {x1, box[1], z1, x2, box[4], z2};
    }

    /** A point turned like {@link #rotated}, as {x, z}. */
    public static float[] rotatedPoint(float x, float z, int quarters) {
        for (int i = 0; i < Math.floorMod(quarters, 4); i++) {
            float nx = 16 - z;
            z = x;
            x = nx;
        }
        return new float[] {x, z};
    }
}
