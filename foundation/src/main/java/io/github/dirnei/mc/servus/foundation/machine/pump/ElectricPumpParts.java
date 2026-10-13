package io.github.dirnei.mc.servus.foundation.machine.pump;

/**
 * The Electric Pump's own parts in pixels on the shared {@link BeamPumpParts}
 * frame, built facing north (front at z 0, the viewer's right toward x 0), as
 * {x1, y1, z1, x2, y2, z2}. The lower half carries the motor housing with
 * copper windings, a terminal box, a motor cap and the motor shaft for the
 * drive belt; the upper half a cap, the crank wheel that drives the beam
 * through the con rod, its axle in a bearing on a column, and a fan grille.
 * The drive belt runs on the back from the motor pulley up to the axle pulley.
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
    /**
     * The motor pulley's centre on the back, lower half frame: below and right
     * of the back stub (x, y 5..11), so pipes and cables never cut the belt.
     */
    public static final float MOTOR_X = 13;
    public static final float MOTOR_Y = 3.2F;
    public static final float MOTOR_PULLEY_R = 0.9F;
    /** The motor shaft stub out of the housing back to the motor pulley. */
    public static final float[] MOTOR_SHAFT = {MOTOR_X - 0.4F, MOTOR_Y - 0.4F, 12.6F, MOTOR_X + 0.4F, MOTOR_Y + 0.4F, 13.5F};

    // Upper half.
    /** The cap, widened toward the crank side as the mounting plate under the wheel. */
    public static final float[] CAP = {4, 0, 4, 14, 2, 12};
    /** Carries the shared post down to the cap. */
    public static final float[] POST_FOOT = {7, 2, 3, 9, 3, 5};

    /** The crank wheel turns about this point, under the beam's drive end. */
    public static final float CRANK_X = BeamPumpParts.PISTON_X;
    public static final float CRANK_Y = 5;
    public static final float CRANK_R = 3;
    /** The crank pin's distance from the wheel centre. */
    public static final float PIN_R = 2;
    /** The wheel disc, two pixels thick, behind the beam. */
    public static final float[] WHEEL = {CRANK_X - CRANK_R, CRANK_Y - CRANK_R, 6, CRANK_X + CRANK_R, CRANK_Y + CRANK_R, 8};
    /** Segments of the rim band around the wheel; the renderer turns one segment by 360 / this each time. */
    public static final int RIM_SEGMENTS = 12;
    /** One rim segment at crank angle 0: tangent to the wheel's edge, as thick as the wheel. */
    public static final float[] RIM_SEGMENT = {CRANK_X + CRANK_R - 0.6F, CRANK_Y - 0.8F, WHEEL[2], CRANK_X + CRANK_R, CRANK_Y + 0.8F, WHEEL[5]};
    /** The iron axle from the wheel back through the bearing to the axle pulley; it turns with the wheel. */
    public static final float[] AXLE = {CRANK_X - 0.5F, CRANK_Y - 0.5F, WHEEL[5], CRANK_X + 0.5F, CRANK_Y + 0.5F, 14.8F};
    /** The bearing block holding the axle, on its column down to the cap. */
    public static final float[] BEARING = {10.5F, 3.5F, 10, 13.5F, 6.5F, 11.5F};
    public static final float[] BEARING_COLUMN = {11, 2, 10.25F, 13, 3.5F, 11.25F};

    /** The drive belt's plane on the back, clear of the windings (z 13.4), and its thickness. */
    public static final float BELT_Z1 = 13.5F;
    public static final float BELT_Z2 = 14.5F;
    public static final float BELT_T = 0.4F;
    public static final float AXLE_PULLEY_R = 1.2F;
    /** The axle pulley on the crank axle and the motor pulley (lower half frame), drawn as discs with spokes. */
    public static final float[] AXLE_PULLEY = {CRANK_X - AXLE_PULLEY_R, CRANK_Y - AXLE_PULLEY_R, BELT_Z1,
            CRANK_X + AXLE_PULLEY_R, CRANK_Y + AXLE_PULLEY_R, BELT_Z2};
    public static final float[] MOTOR_PULLEY = {MOTOR_X - MOTOR_PULLEY_R, MOTOR_Y - MOTOR_PULLEY_R, BELT_Z1,
            MOTOR_X + MOTOR_PULLEY_R, MOTOR_Y + MOTOR_PULLEY_R, BELT_Z2};
    /**
     * One belt strand modelled standing straight up from (8, 0) for a block;
     * the renderer moves, tilts and stretches it from tangent point to tangent point.
     */
    public static final float[] BELT = {8 - BELT_T / 2, 0, BELT_Z1, 8 + BELT_T / 2, 16, BELT_Z2};
    /** The lacing mark, centred on (8, 0) and a little proud of the strand, moved along it by the renderer. */
    public static final float[] BELT_MARK = {8 - BELT_T / 2 - 0.05F, -0.4F, BELT_Z1 - 0.05F, 8 + BELT_T / 2 + 0.05F, 0.4F, BELT_Z2 + 0.05F};
    /** The belt running over both pulleys: motor pulley drives, axle pulley driven, lower half frame. */
    public static final BeltDrive DRIVE = new BeltDrive(MOTOR_X, MOTOR_Y, MOTOR_PULLEY_R + BELT_T / 2,
            CRANK_X, 16 + CRANK_Y, AXLE_PULLEY_R + BELT_T / 2);
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
    /** The fan: a thin plate with the blades painted on, spinning in its own plane. */
    public static final float[] FAN = {FAN_X - 0.05F, FAN_Y - 1.8F, FAN_Z - 1.8F, FAN_X + 0.05F, FAN_Y + 1.8F, FAN_Z + 1.8F};

    private ElectricPumpParts() {
    }

    /** The solid parts of the lower half, for its shape and its model. */
    public static float[][] lowerParts() {
        return new float[][] {BeamPumpParts.BASE, HOUSING, WINDINGS[0], WINDINGS[1], WINDINGS[2], TERMINAL, MOTOR_CAP, MOTOR_SHAFT};
    }

    /** The parts of the upper half that do not move, for its shape and its model. */
    public static float[][] upperParts() {
        return new float[][] {CAP, POST_FOOT, BeamPumpParts.POST, GRILLE_FRAME, GRILLE_BOX, WHEEL, BEARING_COLUMN, BEARING};
    }
}
