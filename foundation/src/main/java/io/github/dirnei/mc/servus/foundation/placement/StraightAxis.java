package io.github.dirnei.mc.servus.foundation.placement;

import java.util.OptionalInt;

/**
 * Keeps a sneak walk on the axis of a straight run, like Create's shafts: a
 * hit on a face across the run goes towards the half of the block it lands
 * in. Pure Java; sides are numbered like {@code Direction.ordinal()} (down,
 * up, north, south, west, east) and a connection mask has bit {@code 1 << side}.
 */
public final class StraightAxis {

    private StraightAxis() {
    }

    /**
     * @param connections mask of the sides the clicked block's run connects to
     * @param face        the clicked face
     * @param x           hit x inside the block, 0 to 1
     * @param y           hit y inside the block, 0 to 1
     * @param z           hit z inside the block, 0 to 1
     * @return the side to walk towards, or empty when the connections are not on
     *         exactly one axis or the face lies on that axis
     */
    public static OptionalInt along(int connections, int face, double x, double y, double z) {
        int axis = -1;
        for (int side = 0; side < 6; side++) {
            if ((connections & (1 << side)) == 0) {
                continue;
            }
            if (axis >= 0 && axis != axis(side)) {
                return OptionalInt.empty();
            }
            axis = axis(side);
        }
        if (axis < 0 || axis == axis(face)) {
            return OptionalInt.empty();
        }
        double along = switch (axis) {
            case 0 -> y;
            case 1 -> z;
            default -> x;
        };
        int negative = axis * 2;
        return OptionalInt.of(along >= 0.5 ? negative + 1 : negative);
    }

    /** 0 for down and up, 1 for north and south, 2 for west and east. */
    private static int axis(int side) {
        return side / 2;
    }
}
