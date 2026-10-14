package io.github.dirnei.mc.servus.foundation.logistics.duct;

/**
 * Which part of a duct a click landed on, from the click point in block
 * pixels (0 to 16 on each axis). Arms are tested first, so a click on the
 * face an arm shares with the centre counts as the arm. Pure Java; sides are numbered like
 * {@link DuctShapes}.
 */
public final class ArmHit {

    /** The click is on the centre cube. */
    public static final int CENTER = -1;
    /** The click is on no part of the duct. */
    public static final int NONE = -2;

    /** Tolerance in pixels, because a hit location sits on a face and rounds either way. */
    private static final double EPSILON = 0.01;

    private ArmHit() {
    }

    /**
     * @param x            click x in pixels
     * @param y            click y in pixels
     * @param z            click z in pixels
     * @param centerRadius radius of the centre cube
     * @param armRadius    radius of the arm per side, zero where there is no arm
     * @param pose         the duct's resting pose, see {@link DuctPose}
     * @param faces        where the arms meet their faces, see {@link DuctPose#faces}
     * @return the side of the hit arm, {@link #CENTER} or {@link #NONE}
     */
    public static int hit(double x, double y, double z, int centerRadius, int[] armRadius, int pose, int faces) {
        DuctShapes.Box centre = DuctShapes.center(centerRadius, pose, faces, armRadius);
        for (int side = 0; side < 6 && side < armRadius.length; side++) {
            if (armRadius[side] > 0 && DuctShapes.arm(side, armRadius[side], centre, faces).contains(x, y, z, EPSILON)) {
                return side;
            }
        }
        return centre.contains(x, y, z, EPSILON) ? CENTER : NONE;
    }
}
