package io.github.dirnei.mc.servus.foundation.drive;

/**
 * The Electric Motor's parts in pixels, built facing north: the shaft points
 * north (z 0), power comes in at the back (z 16). As {x1, y1, z1, x2, y2, z2}.
 * Steel body with copper windings standing 0.4 px proud on iron feet, the
 * Electric Pump's terminal box on top, a front plate, and a round grille on
 * the back with the fan behind it. Shaft, coupling and fan turn about the z
 * axis through {@link #AXIS_X}, {@link #AXIS_Y}; the coupling reaches one
 * pixel into the driven block so it meets a housing inset by a pixel. Pure Java.
 */
public final class ElectricMotorParts {

    public static final float AXIS_X = 8;
    public static final float AXIS_Y = 8;

    public static final float[] FEET = {4, 0, 3, 12, 3, 13};
    public static final float[] BODY = {3, 3, 2, 13, 13, 14.5F};
    public static final float[][] WINDINGS = {
            {2.6F, 2.6F, 4, 13.4F, 13.4F, 5.2F},
            {2.6F, 2.6F, 7.4F, 13.4F, 13.4F, 8.6F},
            {2.6F, 2.6F, 10.8F, 13.4F, 13.4F, 12}};
    /** Between the second and third winding, so it never cuts a band. */
    public static final float[] TERMINAL = {6, 13, 8.8F, 10, 14.5F, 10.6F};
    public static final float[] FRONT_PLATE = {5, 5, 1.5F, 11, 11, 2};
    /** The grille box on the back; only its back face shows the grille, with gaps through to the fan. */
    public static final float[] GRILLE = {4, 4, 14.5F, 12, 12, 16};

    public static final float[] SHAFT = {7.25F, 7.25F, 0, 8.75F, 8.75F, 1.5F};
    public static final float[] COUPLING = {6, 6, -1, 10, 10, 0};
    /** The fan: a thin plate with the blades painted on, spinning in its own plane inside the grille. */
    public static final float[] FAN = {5.2F, 5.2F, 15.2F, 10.8F, 10.8F, 15.3F};

    /** Sparks jump here, at the shaft in front of the front plate. */
    public static final float SPARK_X = 9.5F;
    public static final float SPARK_Y = 9.5F;
    public static final float SPARK_Z = 1;

    private ElectricMotorParts() {
    }

    /** The parts that do not move, for the model. */
    public static float[][] staticParts() {
        return new float[][] {FEET, BODY, WINDINGS[0], WINDINGS[1], WINDINGS[2], TERMINAL, FRONT_PLATE, GRILLE};
    }

    /** The parts of the shape, all within the block: the coupling's tip stays out. */
    public static float[][] shapeParts() {
        return new float[][] {FEET, BODY, WINDINGS[0], WINDINGS[1], WINDINGS[2], TERMINAL, FRONT_PLATE, GRILLE, SHAFT};
    }
}
