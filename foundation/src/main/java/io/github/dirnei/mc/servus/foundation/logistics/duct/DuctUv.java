package io.github.dirnei.mc.servus.foundation.logistics.duct;

/**
 * Where a point on a duct part's face samples its texture, in pixels of a 16
 * pixel texture. On a side face u runs along the part's run axis, one texture
 * per block, and v is 8 plus the point's offset from the part's middle on the
 * face's other axis, always in world direction: so a face of thickness t
 * shows rows {@code 8 - t/2} to {@code 8 + t/2}, the pattern is the same on
 * all four sides and is never mirrored, and bands keep their world order. An
 * end face shows the part's cross-section: v follows the across axis the
 * bands lie on, as on the faces that show every band, so the bands stand side
 * by side there too. A
 * fitting face is instead mapped onto the square {@code 8 - r} to
 * {@code 8 + r} of its texture. Positions are in pixels, axes are
 * {@link DuctPose#X} to {@link DuctPose#Z}. Pure Java.
 */
public final class DuctUv {

    /** A texture position in pixels. */
    public record Uv(float u, float v) {
    }

    private DuctUv() {
    }

    /**
     * The texture position of a point on a side face of a part.
     *
     * @param point      the point
     * @param normalAxis the axis the face points along
     * @param runAxis    the axis the part runs along
     * @param middle     the middle of the part
     * @return the position; on an end face, whose normal is the run axis, the cross-section
     */
    public static Uv side(float[] point, int normalAxis, int runAxis, float[] middle) {
        if (normalAxis == runAxis) {
            int vAxis = across(runAxis);
            int uAxis = 3 - normalAxis - vAxis;
            return new Uv(8 + point[uAxis] - middle[uAxis], 8 + point[vAxis] - middle[vAxis]);
        }
        int vAxis = 3 - normalAxis - runAxis;
        return new Uv(point[runAxis], 8 + point[vAxis] - middle[vAxis]);
    }

    /** The horizontal axis the bands of a part lie side by side on: Z for a run along X, else X. */
    public static int across(int runAxis) {
        return runAxis == DuctPose.X ? DuctPose.Z : DuctPose.X;
    }

    /**
     * The texture position of a point on a fitting face spanning {@code min}
     * to {@code max}, stretched onto the texture's square of radius r.
     *
     * @param normalAxis the axis the face points along
     */
    public static Uv fitting(float[] point, int normalAxis, float[] min, float[] max, int radius) {
        int uAxis = normalAxis == DuctPose.X ? DuctPose.Z : DuctPose.X;
        int vAxis = 3 - normalAxis - uAxis;
        return new Uv(stretch(point[uAxis], min[uAxis], max[uAxis], radius), stretch(point[vAxis], min[vAxis], max[vAxis], radius));
    }

    private static float stretch(float value, float min, float max, int radius) {
        float span = max - min;
        float t = span <= 0 ? 0.5F : (value - min) / span;
        return 8 - radius + t * 2 * radius;
    }
}
