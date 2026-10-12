package io.github.dirnei.mc.servus.foundation.machine.pump;

/**
 * The Electric Pump's own parts in pixels on the shared {@link BeamPumpParts}
 * frame, built facing north (front at z 0, the viewer's right toward x 0), as
 * {x1, y1, z1, x2, y2, z2}. The lower half carries the motor housing with
 * copper windings, a terminal box and a motor cap; the upper half a cap, the
 * crank wheel that drives the beam through the con rod, and a fan grille.
 * Upper half boxes are relative to the upper block.
 */
public final class ElectricPumpParts {

    // Lower half.
    public static final float[] HOUSING = {3, 2, 3, 13, 13, 13};
    /** The copper winding bands around the housing, standing 0.4 px proud. */
    public static final float[][] WINDINGS = {
            {2.6F, 4, 2.6F, 13.4F, 5.2F, 13.4F},
            {2.6F, 7, 2.6F, 13.4F, 8.2F, 13.4F},
            {2.6F, 10, 2.6F, 13.4F, 11.2F, 13.4F}};
    /** The terminal box with the red power accent, on the front where no stub ever sits. */
    public static final float[] TERMINAL = {6, 8, 1.5F, 10, 11.5F, 3};
    public static final float[] MOTOR_CAP = {4, 13, 4, 12, 16, 12};
    /** Sparks jump here, at the brushes on the top winding on the crank side, lower half frame. */
    public static final float SPARK_X = 13.6F;
    public static final float SPARK_Y = 10.6F;
    public static final float SPARK_Z = 8;

    // Upper half.
    public static final float[] CAP = {4, 0, 4, 12, 2, 12};
    /** Carries the shared post down to the cap. */
    public static final float[] POST_FOOT = {7, 2, 3, 9, 3, 5};

    /** The crank wheel turns about this point, under the beam's drive end. */
    public static final float CRANK_X = BeamPumpParts.PISTON_X;
    public static final float CRANK_Y = 5;
    public static final float CRANK_R = 3;
    /** The crank pin's distance from the wheel centre. */
    public static final float PIN_R = 2;
    /** The wheel disc, one pixel thick, behind the beam. */
    public static final float[] WHEEL = {CRANK_X - CRANK_R, CRANK_Y - CRANK_R, 6, CRANK_X + CRANK_R, CRANK_Y + CRANK_R, 7};
    /** The crank pin at rest (crank angle 0), sticking out of the wheel toward the con rod. */
    public static final float[] PIN = {CRANK_X + PIN_R - 0.5F, CRANK_Y - 0.5F, 5, CRANK_X + PIN_R + 0.5F, CRANK_Y + 0.5F, 6};
    /**
     * The con rod drawn straight up from the pin at its top to the level beam
     * at {@link BeamPumpParts#PISTON_X}; the renderer stretches and tilts it
     * from wherever the pin is to wherever the beam end is.
     */
    public static final float[] CON_ROD = {CRANK_X - 0.5F, CRANK_Y + PIN_R, 5.1F, CRANK_X + 0.5F, BeamPumpParts.PIVOT_Y, 5.9F};

    /** The fan grille: a hollow front with slats facing west, and the solid box behind it. */
    public static final float[] GRILLE_FRAME = {3, 2, 9, 4.5F, 8, 13};
    public static final float[] GRILLE_BOX = {4.5F, 2, 9, 7, 8, 13};
    /** The fan spins about the x axis through this point, behind the slats. */
    public static final float FAN_X = 3.6F;
    public static final float FAN_Y = 5;
    public static final float FAN_Z = 11;
    /** The two blades, crossed. */
    public static final float[] FAN_BLADE_Y = {3.4F, FAN_Y - 1.8F, FAN_Z - 0.3F, 3.8F, FAN_Y + 1.8F, FAN_Z + 0.3F};
    public static final float[] FAN_BLADE_Z = {3.4F, FAN_Y - 0.3F, FAN_Z - 1.8F, 3.8F, FAN_Y + 0.3F, FAN_Z + 1.8F};

    private ElectricPumpParts() {
    }

    /** The solid parts of the lower half, for its shape and its model. */
    public static float[][] lowerParts() {
        return new float[][] {BeamPumpParts.BASE, HOUSING, WINDINGS[0], WINDINGS[1], WINDINGS[2], TERMINAL, MOTOR_CAP};
    }

    /** The parts of the upper half that do not move, for its shape and its model. */
    public static float[][] upperParts() {
        return new float[][] {CAP, POST_FOOT, BeamPumpParts.POST, GRILLE_FRAME, GRILLE_BOX, WHEEL};
    }
}
