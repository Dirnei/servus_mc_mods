package io.github.dirnei.mc.servus.manufacturing.machine.pump;

/**
 * The infinite water rule of pumps, as plain Java: the block below must be a
 * water source and at least a minimum number of its four horizontal
 * neighbours must be water sources too.
 */
public final class PumpSource {

    private PumpSource() {
    }

    /**
     * @param sourceBelow    whether the block below the pump is a water source
     * @param neighbours     source flags of the horizontal neighbours of that block
     * @param minNeighbours  how many of them must be sources
     */
    public static boolean isInfinite(boolean sourceBelow, boolean[] neighbours, int minNeighbours) {
        return sourceBelow && count(neighbours) >= minNeighbours;
    }

    /** How many of the flags are set. */
    public static int count(boolean[] neighbours) {
        int sources = 0;
        for (boolean neighbour : neighbours) {
            if (neighbour) {
                sources++;
            }
        }
        return sources;
    }
}
