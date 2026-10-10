package io.github.dirnei.mc.servus.foundation.logistics.duct;

/**
 * Which panel of a duct a click landed on, from the click point in block
 * pixels (0 to 16 on each axis). A click inside a hollow panel's hole is not
 * on the panel. Pure Java; sides are numbered like {@link DuctShapes}.
 */
public final class PanelHit {

    /** The click is on no panel. */
    public static final int NONE = -1;

    /** Tolerance in pixels, because a hit location sits on a face and rounds either way. */
    private static final double EPSILON = 0.01;

    private PanelHit() {
    }

    /**
     * @param clickedFace the side of the clicked face, preferred when its panel contains the point
     * @param solidMask   six bit mask of the sides with a solid panel
     * @param hollowMask  six bit mask of the sides with a hollow panel
     * @return the side of the hit panel or {@link #NONE}
     */
    public static int face(double x, double y, double z, int clickedFace, int solidMask, int hollowMask) {
        if (clickedFace >= 0 && clickedFace < 6 && contains(clickedFace, x, y, z, solidMask, hollowMask)) {
            return clickedFace;
        }
        for (int side = 0; side < 6; side++) {
            if (contains(side, x, y, z, solidMask, hollowMask)) {
                return side;
            }
        }
        return NONE;
    }

    private static boolean contains(int side, double x, double y, double z, int solidMask, int hollowMask) {
        boolean solid = (solidMask & (1 << side)) != 0;
        boolean hollow = (hollowMask & (1 << side)) != 0;
        if (!solid && !hollow) {
            return false;
        }
        for (DuctShapes.Box box : DuctShapes.panel(side, !solid)) {
            if (x >= box.x0() - EPSILON && x <= box.x1() + EPSILON
                    && y >= box.y0() - EPSILON && y <= box.y1() + EPSILON
                    && z >= box.z0() - EPSILON && z <= box.z1() + EPSILON) {
                return true;
            }
        }
        return false;
    }
}
