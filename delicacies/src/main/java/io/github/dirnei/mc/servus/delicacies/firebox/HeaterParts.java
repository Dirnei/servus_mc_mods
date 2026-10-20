package io.github.dirnei.mc.servus.delicacies.firebox;

/**
 * The boxes a Heating Element adds to a firebox, in pixels of a model built facing north ({@code x0, y0, z0, x1, y1, z1}).
 * Pure Java. The thermometer stands on the north face; the needle model turns about its pivot.
 */
public final class HeaterParts {

    /** The Tiled Stove's thermometer above the door on the door tile. */
    public static final float[] STOVE_DIAL = {5.5F, 10.5F, -0.25F, 10.5F, 15.5F, 0};
    /** The Tiled Stove's power port on the west end face of the port tile. */
    public static final float[] STOVE_PORT = {-1, 4, 5, 0, 10, 11};

    /** The needle model, pointing straight up from its pivot, in front of the plane z 0. */
    public static final float[] NEEDLE = {7.8F, 7.6F, -0.15F, 8.2F, 10.2F, -0.1F};
    /** The brass hub over the needle's foot. */
    public static final float[] HUB = {7.55F, 7.55F, -0.27F, 8.45F, 8.45F, -0.19F};
    /** The needle model's pivot. */
    public static final float PIVOT_X = 8;
    public static final float PIVOT_Y = 8;

    private HeaterParts() {
    }

    /** The middle of a dial's front face, as {x, y, z}: where the needle's pivot goes. */
    public static float[] dialCentre(float[] dial) {
        return new float[] {(dial[0] + dial[3]) / 2, (dial[1] + dial[4]) / 2, dial[2]};
    }
}
