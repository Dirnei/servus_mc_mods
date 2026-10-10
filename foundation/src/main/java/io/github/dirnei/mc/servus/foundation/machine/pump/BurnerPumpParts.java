package io.github.dirnei.mc.servus.foundation.machine.pump;

/**
 * The Burner Pump's parts in pixels, built facing north (front at z 0, the
 * viewer's right toward x 0), as {x1, y1, z1, x2, y2, z2}. The block shapes,
 * the datagen models and the renderer share them. Upper half boxes are
 * relative to the upper block.
 *
 * <p>The rod stands at x 1..3, z 3..5: beside the firebox, in front of the
 * west stub (z 5..11) and clear of the front, which never has a stub.
 */
public final class BurnerPumpParts {

    public static final float[] BASE = {1, 0, 1, 15, 2, 15};
    public static final float[] FIREBOX = {3, 2, 3, 13, 14, 13};
    public static final float[] DRUM_LOWER = {4, 14, 4, 12, 16, 12};
    /** The socket the pump rod dips into, on the base plate. */
    public static final float[] SOCKET = {0.5F, 2, 2.5F, 3.5F, 4, 5.5F};

    public static final float[] DRUM_UPPER = {4, 0, 4, 12, 3, 12};
    public static final float[] CYLINDER = {10, 3, 2.5F, 14, 9, 6.5F};
    public static final float[] POST = {7, 3, 3, 9, 12, 5};
    public static final float[] CHIMNEY = {3, 3, 9, 6, 15, 12};

    /** The beam across the top, upper half, and the point it rocks about. */
    public static final float[] BEAM = {0.5F, 12, 3, 15.5F, 13.5F, 5};
    public static final float PIVOT_X = 8;
    public static final float PIVOT_Y = 12.75F;
    public static final float PIVOT_Z = 4;
    /** Where the pump rod and the piston rod hang from the beam, in x. */
    public static final float ROD_X = 2;
    public static final float PISTON_X = 12;

    /** The pump rod from the socket up to the beam, lower half frame, at rest. */
    public static final float[] ROD = {1.5F, 2.5F, 3.5F, 2.5F, 16 + PIVOT_Y, 4.5F};
    /** The piston rod from the cylinder up to the beam, upper half frame, at rest. */
    public static final float[] PISTON = {11.5F, 9, 3.5F, 12.5F, PIVOT_Y, 4.5F};

    /** Smoke leaves here, upper half frame. */
    public static final float SMOKE_X = 4.5F;
    public static final float SMOKE_Y = 15;
    public static final float SMOKE_Z = 10.5F;

    private BurnerPumpParts() {
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
