package io.github.dirnei.mc.servus.logistics.duct;

/**
 * Which part of a duct a click landed on, from the click point in block
 * pixels (0 to 16 on each axis). Arms are tested first, so a click on the
 * face an arm shares with the centre counts as the arm. Pure Java; sides are
 * numbered like {@link DuctShapes}.
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
     * @return the side of the hit arm, {@link #CENTER} or {@link #NONE}
     */
    public static int hit(double x, double y, double z, int centerRadius, int[] armRadius) {
        for (int side = 0; side < 6 && side < armRadius.length; side++) {
            if (armRadius[side] > 0 && contains(DuctShapes.arm(side, armRadius[side]), x, y, z)) {
                return side;
            }
        }
        return contains(DuctShapes.center(centerRadius), x, y, z) ? CENTER : NONE;
    }

    private static boolean contains(DuctShapes.Box box, double x, double y, double z) {
        return x >= box.x0() - EPSILON && x <= box.x1() + EPSILON
                && y >= box.y0() - EPSILON && y <= box.y1() + EPSILON
                && z >= box.z0() - EPSILON && z <= box.z1() + EPSILON;
    }
}
