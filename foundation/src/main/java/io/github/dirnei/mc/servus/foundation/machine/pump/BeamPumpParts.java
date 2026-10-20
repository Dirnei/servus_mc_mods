package io.github.dirnei.mc.servus.foundation.machine.pump;

/**
 * The frame both beam pumps share, in pixels, built facing north (front at
 * z 0, the viewer's right toward x 0), as {x1, y1, z1, x2, y2, z2}: base
 * plate, rod socket, post, beam and pump rod. The block shapes, the datagen
 * models and the renderers share them. Upper half boxes are relative to the
 * upper block.
 *
 * <p>The rod stands at x 1..3, z 3..5: beside the engine body, in front of the
 * west stub (z 5..11) and clear of the front, which never has a stub.
 */
public final class BeamPumpParts {

    public static final float[] BASE = {1, 0, 1, 15, 2, 15};
    /** The socket the pump rod dips into, on the base plate. */
    public static final float[] SOCKET = {0.5F, 2, 2.5F, 3.5F, 4, 5.5F};

    public static final float[] POST = {7, 3, 3, 9, 12, 5};

    /** The beam across the top, upper half, and the point it rocks about. */
    public static final float[] BEAM = {0.5F, 12, 3, 15.5F, 13.5F, 5};
    public static final float PIVOT_X = 8;
    public static final float PIVOT_Y = 12.75F;
    public static final float PIVOT_Z = 4;
    /** Where the pump rod and the drive rod (piston or con rod) hang from the beam, in x. */
    public static final float ROD_X = 2;
    public static final float PISTON_X = 12;

    /** The pump rod from the socket up to the beam, lower half frame, at rest. */
    public static final float[] ROD = {1.5F, 2.5F, 3.5F, 2.5F, 16 + PIVOT_Y, 4.5F};

    /** Where water splashes when the rod dips, lower half frame: the top of the socket. */
    public static final float SPLASH_X = 2;
    public static final float SPLASH_Y = 4;
    public static final float SPLASH_Z = 4;

    private BeamPumpParts() {
    }
}
