package io.github.dirnei.mc.servus.foundation.placement;

/**
 * Picks the horizontal side whose edge is nearest to a hit on a top or bottom
 * face. Pure Java; north is towards negative z and east towards positive x,
 * as in Minecraft.
 */
public final class QuadrantDirection {

    /** A horizontal side of a block. */
    public enum Edge {
        NORTH, EAST, SOUTH, WEST
    }

    private QuadrantDirection() {
    }

    /**
     * @param x hit x inside the block, 0 to 1
     * @param z hit z inside the block, 0 to 1
     * @return the nearest edge; ties go to the first of north, east, south, west
     */
    public static Edge nearestEdge(double x, double z) {
        double[] distance = {z, 1.0 - x, 1.0 - z, x};
        int best = 0;
        for (int i = 1; i < distance.length; i++) {
            if (distance[i] < distance[best]) {
                best = i;
            }
        }
        return Edge.values()[best];
    }
}
