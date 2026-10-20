package io.github.dirnei.mc.servus.delicacies.meat;

/**
 * How a placed loaf is cut: it gives {@link #SLICES} slices, its {@code cut} state counts the slices already taken,
 * and each slice takes {@link #SLICE_WIDTH} pixels off its west end.
 */
public final class LoafSlices {

    /** Slices one loaf gives. */
    public static final int SLICES = 4;
    /** The highest {@code cut} state; the next cut takes the last slice. */
    public static final int MAX_CUT = SLICES - 1;
    /** Width of one slice in pixels. */
    public static final float SLICE_WIDTH = 2.5F;

    private static final float WEST = 3;
    private static final float EAST = WEST + SLICES * SLICE_WIDTH;
    private static final float HEIGHT = 5;
    private static final float NORTH = 5;
    private static final float SOUTH = 11;

    private LoafSlices() {
    }

    /** Slices still on a loaf with {@code cut} slices taken. */
    public static int left(int cut) {
        check(cut);
        return SLICES - cut;
    }

    /** Whether the next cut takes the last slice and so removes the loaf. */
    public static boolean isLastSlice(int cut) {
        check(cut);
        return cut == MAX_CUT;
    }

    /** The {@code cut} state after one more cut; the last slice has no next state. */
    public static int next(int cut) {
        if (isLastSlice(cut)) {
            throw new IllegalArgumentException("cut " + cut + " leaves no loaf");
        }
        return cut + 1;
    }

    /** The loaf's box in pixels, built facing north: {@code x0, y0, z0, x1, y1, z1}. */
    public static float[] box(int cut) {
        check(cut);
        return new float[] {WEST + cut * SLICE_WIDTH, 0, NORTH, EAST, HEIGHT, SOUTH};
    }

    private static void check(int cut) {
        if (cut < 0 || cut > MAX_CUT) {
            throw new IllegalArgumentException("cut " + cut + " is outside 0 to " + MAX_CUT);
        }
    }
}
