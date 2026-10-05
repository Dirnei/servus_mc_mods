package io.github.dirnei.mc.servus.storage.rack;

/**
 * Maps a hit on a rack to one of its four compartments. Pure, no Minecraft
 * imports. Facing follows Minecraft's 2D data order: 0 south, 1 west, 2 north,
 * 3 east. Compartments are 0 top left, 1 top right, 2 bottom left, 3 bottom
 * right as seen by a player looking at the front.
 */
public final class RackCompartments {

    public static final int COUNT = 4;

    private RackCompartments() {
    }

    /**
     * @param facing2d the block's facing as 2D data value
     * @param x        hit x within the block, 0 to 1
     * @param y        hit y within the block, 0 to 1
     * @param z        hit z within the block, 0 to 1
     */
    public static int index(int facing2d, double x, double y, double z) {
        double fromLeft = fromViewerLeft(facing2d, x, z);
        int column = fromLeft < 0.5 ? 0 : 1;
        int row = y >= 0.5 ? 0 : 1;
        return row * 2 + column;
    }

    /** Fraction across the front face measured from the viewer's left edge. */
    static double fromViewerLeft(int facing2d, double x, double z) {
        return switch (facing2d) {
            case 0 -> x;          // front faces south, viewer looks north: left edge is west
            case 1 -> z;          // front faces west, viewer looks east: left edge is north
            case 2 -> 1.0 - x;    // front faces north, viewer looks south: left edge is east
            default -> 1.0 - z;   // front faces east, viewer looks west: left edge is south
        };
    }

    /** Horizontal centre of a compartment measured from the viewer's left, 0 to 1. */
    public static double centreFromLeft(int index) {
        return (index % 2 == 0) ? 0.25 : 0.75;
    }

    /** Vertical centre of a compartment, 0 to 1 from the bottom. */
    public static double centreY(int index) {
        return index < 2 ? 0.75 : 0.25;
    }
}
