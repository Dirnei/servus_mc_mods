package io.github.dirnei.mc.servus.alchemy.structure;

/** The side the front of a build faces, with its x and z step. Pure Java, shared by every alchemy build. */
public enum Facing {
    NORTH(0, -1), EAST(1, 0), SOUTH(0, 1), WEST(-1, 0);

    private final int dx;
    private final int dz;

    Facing(int dx, int dz) {
        this.dx = dx;
        this.dz = dz;
    }

    /** The x step to the player's right when they look at a front facing this way. */
    public int rightX() {
        return dz;
    }

    /** The z step to the player's right when they look at a front facing this way. */
    public int rightZ() {
        return -dx;
    }

    /** The offset {@code right} to the player's right and {@code up} of a front facing this way. */
    public Offset offset(int right, int up) {
        return new Offset(right * rightX(), up, right * rightZ());
    }
}
