package io.github.dirnei.mc.servus.shuttles.field;

/** Which way a felled tree falls: straight away from the rail the Lumber shuttle stands on. Pure Java. */
public final class FellDirection {

    /** A horizontal direction in world axes: x grows east, z grows south. */
    public enum Way {
        NORTH(0, -1), SOUTH(0, 1), EAST(1, 0), WEST(-1, 0);

        private final int dx;
        private final int dz;

        Way(int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
        }

        public int dx() {
            return dx;
        }

        public int dz() {
            return dz;
        }
    }

    private FellDirection() {
    }

    /**
     * The direction from the rail to the trunk along the larger of the two
     * offsets; on a tie, along the axis across the rail's run.
     *
     * @param dx          the trunk's x offset from the rail, in blocks
     * @param dz          the trunk's z offset from the rail, in blocks
     * @param runsAlongX  whether the rail's run goes east to west, so across it is north to south
     */
    public static Way away(double dx, double dz, boolean runsAlongX) {
        boolean alongX = Math.abs(dx) > Math.abs(dz) || Math.abs(dx) == Math.abs(dz) && !runsAlongX;
        if (alongX) {
            return dx < 0 ? Way.WEST : Way.EAST;
        }
        return dz < 0 ? Way.NORTH : Way.SOUTH;
    }
}
