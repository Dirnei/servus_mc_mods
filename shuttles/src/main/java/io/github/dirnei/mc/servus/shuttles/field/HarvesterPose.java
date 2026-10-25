package io.github.dirnei.mc.servus.shuttles.field;

import io.github.dirnei.mc.servus.shuttles.field.HarvesterParts.Part;
import java.util.ArrayList;
import java.util.List;

/**
 * The pose of a Lumber shuttle's hold, knuckle-boom crane and harvester head,
 * in the shuttle model's pixels (x across, y up, z forward, the body from 2
 * to 14, one block 20 pixels at the shuttle's scale).
 *
 * @param door   how far the lid doors have swung over, 0 shut to 1 hanging outside the walls
 * @param side   how far the walls have folded down, 0 up to 1 resting on the ground
 * @param lift   how far the pillar has risen out of the hold
 * @param unfold how far the main boom has come from its stowed elbow to the solved one
 * @param wrist  where the rotator hangs from the stick
 * @param yaw    the head's turn about the vertical, radians, 0 facing forward
 * @param tilt   the head's tilt about its own x
 * @param roll   the head's roll about its own z
 * @param a1     each arm's inner link angle at its hinge, 0 hugging the log
 * @param a2     each arm's outer link angle at its elbow
 * @param saw    how far the chain bar has swung out, 0 parked to 1 across the log
 * @param size   the head's scale; it lies small in the hold and grows as it rises
 */
public record HarvesterPose(double door, double side, double lift, double unfold, double[] wrist, double yaw, double tilt, double roll,
                            double a1, double a2, double saw, double size) {

    /** One block in model pixels at the shuttle's 0.8 scale. */
    public static final double BLOCK = 20;
    /** The rotator's height above the plot's floor while it grips, so the head's bottom is at the cut. */
    public static final double GRIP_HEIGHT = 13.8;
    /** How far a wall folds about its bottom edge to rest its top on the ground. */
    public static final double SIDE_DOWN = Math.toRadians(135);
    /** How far a door swings up, over and down onto the outside of its wall. */
    public static final double DOOR_OVER = Math.toRadians(270);
    /** How far the chain bar turns at full swing. */
    public static final double BAR_SWING = 1.25;
    private static final double MIRROR_STEP = 0.005;
    /** The nearest the wrist comes to the shuttle's centre line while it backs off, so the head stays clear of the ring. */
    private static final double NEAREST_WRIST = 6;
    private static final double MAIN_BOOM = HarvesterParts.BOOM_LENGTH;
    private static final double[] PILLAR_DOWN = {8, HarvesterParts.COLLAR_LOW, 11.5};
    private static final double PILLAR_RISE = 8.5;
    private static final double[] ELBOW_STOWED = {8, 9, 4.55};
    private static final double[] FOLDED = {Math.toRadians(90), Math.toRadians(-90)};
    private static final double[] OPEN = {Math.toRadians(20), Math.toRadians(35)};
    /** The head's turn to the side, the turn to its back, when it fits in the hold. */
    private static final double QUARTER = Math.PI / 2;

    public static final HarvesterPose STOWED = new HarvesterPose(0, 0, 0, 0, new double[] {8, 8.7, 12.6}, QUARTER, 0, QUARTER,
            FOLDED[0], FOLDED[1], 0, 0.55);
    /** Unfolded over the hold, the head upright and full size with its arms folded. */
    public static final HarvesterPose RAISED = new HarvesterPose(1, 1, 1, 1, new double[] {8, 28, 16}, 0, 0, 0,
            FOLDED[0], FOLDED[1], 0, 1);

    /** A part where the renderer draws it: its model's points (a centred part's from its origin) go through the frame. */
    public record Placed(Part part, Frame frame) {
    }

    /**
     * The pose at a moment of the felling.
     *
     * @param log the base log's centre at the plot's floor, the one the head grips
     */
    public static HarvesterPose of(double[] log, HarvesterTimeline.Moment moment) {
        double t = smooth(moment.progress());
        HarvesterPose unfolded = STOWED.with(1, 1);
        HarvesterPose grip = grip(log);
        HarvesterPose open = grip.arms(OPEN);
        return switch (moment.stage()) {
            case DOORS -> STOWED.with(t, 0);
            case WALLS -> STOWED.with(1, t);
            case RISE -> mix(unfolded, RAISED, t);
            case REACH -> mix(RAISED, open.at(back(log, 6, 3)), t);
            case MOVE_IN -> mix(open.at(back(log, 6, 3)), open, t);
            case GRIP -> mix(open, grip, t);
            case CUT -> grip.sawing(smooth(moment.progress() * 1.4));
            case LET_GO -> mix(grip.sawing(1), open.at(back(log, 7, 2)), t);
            case WAIT -> open.at(back(log, 7, 2));
            case HOME -> mix(open.at(back(log, 7, 2)), RAISED, t);
        };
    }

    /**
     * The grip's wrist backed off {@code away} toward the rail and raised
     * {@code up}; for a log right beside the rail it backs off only as far as
     * keeps the head's back clear of the slewing ring.
     */
    private static double[] back(double[] log, double away, double up) {
        double[] toward = toward(log);
        double[] wrist = grip(log).wrist;
        double out = (wrist[0] - 8) * toward[0] + (wrist[2] - 8) * toward[2];
        double along = -Math.clamp(out - NEAREST_WRIST, 0, away);
        return new double[] {wrist[0] + toward[0] * along, wrist[1] + up, wrist[2] + toward[2] * along};
    }

    /**
     * Gripping the log: the rotator stands {@link HarvesterParts#LOG_NEAR}
     * short of the log's near face on the side toward the shuttle, centred on
     * it, the head facing it with every arm closed.
     */
    public static HarvesterPose grip(double[] log) {
        double[] toward = toward(log);
        double[] wrist = {log[0] - toward[0] * (HarvesterParts.LOG_HALF + HarvesterParts.LOG_NEAR), log[1] + GRIP_HEIGHT,
                log[2] - toward[2] * (HarvesterParts.LOG_HALF + HarvesterParts.LOG_NEAR)};
        return new HarvesterPose(1, 1, 1, 1, wrist, Math.atan2(toward[0], toward[2]), 0, 0, 0, 0, 0, 1);
    }

    /** The axis the head comes in along: the one of x and z the log lies farther out on from the shuttle's centre. */
    private static double[] toward(double[] log) {
        double dx = log[0] - 8;
        double dz = log[2] - 8;
        return Math.abs(dx) >= Math.abs(dz) ? new double[] {Math.signum(dx) == 0 ? 1 : Math.signum(dx), 0, 0}
                : new double[] {0, 0, Math.signum(dz)};
    }

    /** Of the base logs' centres, the index of the one nearest the point: a 2x2 tree is gripped by the log nearest the rail. */
    public static int nearestLog(List<double[]> logs, double x, double z) {
        int best = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < logs.size(); i++) {
            double distance = Math.hypot(logs.get(i)[0] - x, logs.get(i)[2] - z);
            if (distance < bestDistance - 1e-9) {
                best = i;
                bestDistance = distance;
            }
        }
        return best;
    }

    private HarvesterPose with(double door, double side) {
        return new HarvesterPose(door, side, lift, unfold, wrist, yaw, tilt, roll, a1, a2, saw, size);
    }

    private HarvesterPose arms(double[] angles) {
        return new HarvesterPose(door, side, lift, unfold, wrist, yaw, tilt, roll, angles[0], angles[1], saw, size);
    }

    private HarvesterPose at(double[] newWrist) {
        return new HarvesterPose(door, side, lift, unfold, newWrist, yaw, tilt, roll, a1, a2, saw, size);
    }

    private HarvesterPose sawing(double newSaw) {
        return new HarvesterPose(door, side, lift, unfold, wrist, yaw, tilt, roll, a1, a2, newSaw, size);
    }

    private static HarvesterPose mix(HarvesterPose a, HarvesterPose b, double t) {
        return new HarvesterPose(lerp(a.door, b.door, t), lerp(a.side, b.side, t), lerp(a.lift, b.lift, t), lerp(a.unfold, b.unfold, t),
                lerp(a.wrist, b.wrist, t), lerp(a.yaw, b.yaw, t), lerp(a.tilt, b.tilt, t), lerp(a.roll, b.roll, t), lerp(a.a1, b.a1, t),
                lerp(a.a2, b.a2, t), lerp(a.saw, b.saw, t), lerp(a.size, b.size, t));
    }

    /** The pillar's top, where the main boom hinges. */
    public double[] pillarTop() {
        return new double[] {PILLAR_DOWN[0], PILLAR_DOWN[1] + PILLAR_RISE * lift, PILLAR_DOWN[2]};
    }

    /** The knuckle between the main boom and the stick: blended from its stowed point to the two link solve. */
    public double[] elbow() {
        return lerp(ELBOW_STOWED, solveElbow(pillarTop(), wrist), unfold);
    }

    /** How far the stick with its telescope reaches from the knuckle to the wrist. */
    public double reach() {
        return distance(elbow(), wrist);
    }

    /**
     * The knuckle of a main boom of fixed length from the pillar top and a
     * stick reaching the wrist, the knuckle up. The stick's length is 0.8 of
     * the distance, at least 9.5, and as long as it takes to reach.
     */
    static double[] solveElbow(double[] top, double[] wrist) {
        double[] d = {wrist[0] - top[0], wrist[1] - top[1], wrist[2] - top[2]};
        double flat = Math.max(1e-6, Math.hypot(d[0], d[2]));
        double hx = d[0] / flat;
        double hz = d[2] / flat;
        double distance = Math.hypot(flat, d[1]);
        double stick = Math.max(9.5, Math.min(28, distance * 0.8));
        if (MAIN_BOOM + stick < distance) {
            stick = distance - MAIN_BOOM + 0.01;
        }
        if (MAIN_BOOM - stick > distance) {
            stick = MAIN_BOOM - distance + 0.01;
        }
        double phi = Math.atan2(d[1], flat);
        double cos = (MAIN_BOOM * MAIN_BOOM + distance * distance - stick * stick) / (2 * MAIN_BOOM * Math.max(1e-6, distance));
        double angle = phi + Math.acos(Math.clamp(cos, -1, 1));
        return new double[] {top[0] + MAIN_BOOM * Math.cos(angle) * hx, top[1] + MAIN_BOOM * Math.sin(angle),
                top[2] + MAIN_BOOM * Math.cos(angle) * hz};
    }

    /** The head's frame at the wrist: turned, tilted, rolled and scaled. */
    public Frame head() {
        return Frame.translation(wrist).then(Frame.rotY(yaw)).then(Frame.rotX(tilt)).then(Frame.rotZ(roll))
                .then(Frame.scale(size, size, size));
    }

    /** Every moving part where it is drawn; the left and back ones are the right and front ones turned half round. */
    public List<Placed> parts() {
        List<Placed> parts = new ArrayList<>();
        Frame half = Frame.about(new double[] {8, 0, 8}, Frame.rotY(Math.PI));
        parts.add(new Placed(Part.BODY, Frame.IDENTITY));
        Frame side = Frame.about(new double[] {HarvesterParts.OUTER, HarvesterParts.HINGE_Y, 0}, Frame.rotZ(-this.side * SIDE_DOWN));
        Frame door = side.then(Frame.about(new double[] {HarvesterParts.OUTER, HarvesterParts.HOLD_TOP, 0}, Frame.rotZ(-this.door * DOOR_OVER)));
        Frame end = Frame.about(new double[] {0, HarvesterParts.HINGE_Y, HarvesterParts.OUTER}, Frame.rotX(this.side * SIDE_DOWN));
        for (Frame turn : List.of(Frame.IDENTITY, half)) {
            parts.add(new Placed(Part.SIDE, turn.then(side)));
            parts.add(new Placed(Part.DOOR, turn.then(door)));
            parts.add(new Placed(Part.END, turn.then(end)));
        }
        crane(parts);
        head(parts);
        return parts;
    }

    private void crane(List<Placed> parts) {
        double[] top = pillarTop();
        double[] elbow = elbow();
        parts.add(new Placed(Part.RING, Frame.IDENTITY));
        double pillarScale = (top[1] + 0.5 - HarvesterParts.PILLAR_FOOT) / (HarvesterParts.PILLAR_LOW_TOP - HarvesterParts.PILLAR_FOOT);
        parts.add(new Placed(Part.PILLAR, Frame.about(new double[] {0, HarvesterParts.PILLAR_FOOT, 0}, Frame.scale(1, pillarScale, 1))));
        parts.add(new Placed(Part.COLLAR, Frame.translation(0, top[1] - HarvesterParts.COLLAR_LOW, 0)));
        parts.add(segment(Part.BOOM, top, elbow, HarvesterParts.BOOM_LENGTH));
        double[] mid = lerp(top, elbow, 0.55);
        parts.add(segment(Part.CYLINDER, new double[] {top[0], top[1] - 2.5, top[2] - 0.5}, new double[] {mid[0], mid[1] - 1.5, mid[2]},
                HarvesterParts.CYLINDER_LENGTH));
        parts.add(new Placed(Part.KNUCKLE, Frame.translation(elbow)));
        double reach = distance(elbow, wrist);
        double[] stickEnd = lerp(elbow, wrist, Math.min(1, HarvesterParts.STICK_LENGTH / Math.max(1e-6, reach)));
        parts.add(segment(Part.STICK, elbow, stickEnd, HarvesterParts.STICK_LENGTH));
        if (reach > HarvesterParts.STICK_LENGTH + 0.05) {
            parts.add(segment(Part.TELESCOPE, stickEnd, wrist, HarvesterParts.TELESCOPE_LENGTH));
        }
    }

    private void head(List<Placed> parts) {
        Frame head = head();
        parts.add(new Placed(Part.ROTATOR, head));
        parts.add(new Placed(Part.FRAME, head));
        parts.add(new Placed(Part.SAW_BOX, head));
        parts.add(new Placed(Part.BAR, head.then(Frame.translation(HarvesterParts.BAR_PIVOT)).then(Frame.rotY(-saw * BAR_SWING))));
        for (double[] knife : HarvesterParts.KNIVES) {
            arm(parts, head, knife[0], HarvesterParts.KNIFE_INNER, Part.KNIFE_INNER, Part.KNIFE_OUTER, 1);
            arm(parts, head, knife[0], HarvesterParts.KNIFE_INNER, Part.KNIFE_INNER_LEFT, Part.KNIFE_OUTER_LEFT, -1);
        }
        arm(parts, head, HarvesterParts.ROLLER[0], HarvesterParts.ROLLER_INNER, Part.ROLLER_INNER, Part.ROLLER_OUTER, 1);
        arm(parts, head, HarvesterParts.ROLLER[0], HarvesterParts.ROLLER_INNER, Part.ROLLER_INNER_LEFT, Part.ROLLER_OUTER_LEFT, -1);
    }

    /**
     * One arm on the right ({@code side} 1) or, mirrored, on the left (-1):
     * its inner link at the hinge, its outer link at the elbow. The left arm sits
     * a hair higher and forward so the hooks of both, crossing when folded, never
     * share a plane.
     */
    private void arm(List<Placed> parts, Frame head, double bottom, double inner, Part innerPart, Part outerPart, int side) {
        double step = side < 0 ? MIRROR_STEP : 0;
        Frame hinge = head.then(Frame.translation(side * HarvesterParts.ARM_HINGE_X, bottom + step, HarvesterParts.ARM_HINGE_Z + step))
                .then(Frame.rotY(side * a1));
        parts.add(new Placed(innerPart, hinge));
        parts.add(new Placed(outerPart, hinge.then(Frame.translation(side * inner, 0, 0)).then(Frame.rotY(side * a2))));
    }

    /** A part of model length {@code length} along +z, stretched from one point to the other. */
    private static Placed segment(Part part, double[] from, double[] to, double length) {
        return new Placed(part, Frame.along(from, to).then(Frame.scale(1, 1, distance(from, to) / length)));
    }

    private static double smooth(double t) {
        double c = Math.clamp(t, 0, 1);
        return c * c * (3 - 2 * c);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {lerp(a[0], b[0], t), lerp(a[1], b[1], t), lerp(a[2], b[2], t)};
    }

    private static double distance(double[] a, double[] b) {
        return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
    }
}
