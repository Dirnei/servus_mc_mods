package io.github.dirnei.mc.servus.shuttles.field;

/**
 * The pose of a Lumber shuttle's felling arm: the turret turns toward the
 * trunk while the two part arm unfolds from its folded rest, lower part
 * forward and upper part folded back over it, until the felling head reaches
 * the trunk. Both parts telescope to the same length, solved as a two link
 * arm with the elbow up. Lengths and positions are in one unit the caller
 * picks; angles are degrees in the arm's vertical plane from the horizontal
 * toward the trunk, positive up. Pure Java.
 */
public final class ArmPose {

    /** How much longer than half the reach each part is at full reach, so the elbow stays bent. */
    private static final double BEND = 1.2;
    private static final double REST_LOWER = 0;
    private static final double REST_UPPER = 180;

    /**
     * @param yaw         the turret's turn in degrees, from facing forward toward the trunk
     * @param lower       the lower part's angle
     * @param upper       the upper part's angle
     * @param length      the length of each part
     * @param clawOpening how far the claws are open, 1 wide open and 0 closed on the trunk
     * @param headX       the head's distance out from the shoulder toward the trunk
     * @param headY       the head's height above the shoulder
     */
    public record Pose(double yaw, double lower, double upper, double length, double clawOpening, double headX, double headY) {
    }

    private ArmPose() {
    }

    /**
     * @param distance   how far the trunk is out from the shoulder, horizontally
     * @param drop       how far the cut lies below the shoulder
     * @param yaw        the turret's turn toward the trunk, in degrees
     * @param progress   0 folded at rest to 1 at the trunk
     * @param grip       0 claws open to 1 closed
     * @param rest       the length of each part folded
     * @param maxLength  the longest each part telescopes; farther trunks are reached for in their direction only
     */
    public static Pose of(double distance, double drop, double yaw, double progress, double grip, double rest, double maxLength) {
        double eased = ease(Math.clamp(progress, 0, 1));
        double reach = Math.min(Math.hypot(distance, drop), 2 * maxLength);
        double length = Math.clamp(reach / 2 * BEND, rest, maxLength);
        double toward = Math.toDegrees(Math.atan2(-drop, distance));
        double bend = Math.toDegrees(Math.acos(Math.min(1, reach / (2 * length))));
        double lower = lerp(REST_LOWER, toward + bend, eased);
        double upper = lerp(REST_UPPER, toward - bend, eased);
        double now = lerp(rest, length, eased);
        double headX = now * (Math.cos(Math.toRadians(lower)) + Math.cos(Math.toRadians(upper)));
        double headY = now * (Math.sin(Math.toRadians(lower)) + Math.sin(Math.toRadians(upper)));
        return new Pose(yaw * eased, lower, upper, now, 1 - Math.clamp(grip, 0, 1), headX, headY);
    }

    /** Slow at both ends. */
    private static double ease(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }
}
