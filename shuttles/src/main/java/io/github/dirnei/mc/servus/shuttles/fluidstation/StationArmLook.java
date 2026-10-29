package io.github.dirnei.mc.servus.shuttles.fluidstation;

import io.github.dirnei.mc.servus.shuttles.shuttle.module.TankerLook;

/**
 * The swing hose arm of a fluid station, in block pixels of the lower half
 * for a station facing north, with {@code out} measured from the front face
 * toward the rail. A copper stub rises out of the tank top in front of the
 * face to a copper swivel; the steel boom turns on the swivel from standing
 * up against the upper half to lying level over the rail block, then a dark
 * hose lowers its copper head over the tanker's filler cap. In the boom's
 * own frame {@code u} runs along it from the pivot and {@code v} is up when
 * it lies level. Pure Java.
 */
public final class StationArmLook {

    /** Ticks the boom takes to swing between upright and level. */
    public static final int SWING_TICKS = 8;
    /** Ticks the hose takes to lower the head onto the cap or raise it. */
    public static final int LOWER_TICKS = 6;

    /** The swivel's axis, along x through the arm's centre plane. */
    public static final float CENTRE_X = 8;
    public static final float PIVOT_Y = 19;
    public static final float PIVOT_OUT = 1.25F;
    /** The copper swivel: half its length along x and half its height and depth. */
    public static final float SWIVEL_HALF_X = 1.3F;
    public static final float SWIVEL_HALF = 1.2F;
    /** The copper stub from the tank top up into the swivel. */
    public static final float STUB_HALF = 0.6F;
    public static final float STUB_Y0 = 15;
    public static final float STUB_Y1 = 18;
    /** The steel boom: half its square section, how far it reaches behind the pivot and its length in front of it. */
    public static final float BOOM_HALF = 0.7F;
    public static final float BOOM_BACK = 0.5F;
    public static final float BOOM_LENGTH = 7.45F;
    /** Where the hose hangs from the boom: over the rail block's centre when the boom lies level. */
    public static final float HOSE_AT = 8 - PIVOT_OUT;
    /** The dark hose: half its section and how far it reaches up into the boom and down into the head. */
    public static final float HOSE_HALF = 0.45F;
    public static final float HOSE_TOP = -0.5F;
    public static final float HOSE_INTO_HEAD = 0.3F;
    /** The copper head: half its width and depth, its height and its gap under the boom when raised. */
    public static final float HEAD_HALF = 1.1F;
    public static final float HEAD_HEIGHT = 1.2F;
    public static final float HEAD_GAP = 0.05F;
    /** How far the docked head's bottom reaches down past the top of the filler cap, around it. */
    public static final float HEAD_SINK = 0.4F;

    private StationArmLook() {
    }

    /** The top of the tanker's filler cap in pixels of the rail's block, which stands level with the lower half. */
    public static float capTop() {
        return TankerLook.FLOOR + TankerLook.SCALE * TankerLook.CAP_TOP;
    }

    /** The head's top below the boom's axis, in the boom's frame, with the hose let out by {@code drop}. */
    public static float headTop(float drop) {
        return -BOOM_HALF - HEAD_GAP - drop;
    }

    /** How far the hose lets the head down when the boom lies level: until it sits {@link #HEAD_SINK} over the cap. */
    public static float hoseDrop() {
        return PIVOT_Y + headTop(0) - HEAD_HEIGHT - (capTop() - HEAD_SINK);
    }

    /** Ticks to the nearer end of a stop of {@code ticks} after {@code elapsed}: how far the arm may be out. */
    private static float reach(float ticks, float elapsed) {
        return ticks <= 0 ? 0 : Math.max(0, Math.min(elapsed, ticks - elapsed));
    }

    /** How far the boom has swung, from 0 upright to 1 level over the rail, a stop of {@code ticks} after {@code elapsed}. */
    public static float swing(float ticks, float elapsed) {
        return ease(reach(ticks, elapsed) / SWING_TICKS);
    }

    /** How far the hose is let down, from 0 raised to 1 on the cap; it moves only while the boom lies level. */
    public static float lower(float ticks, float elapsed) {
        return ease((reach(ticks, elapsed) - SWING_TICKS) / LOWER_TICKS);
    }

    /** The boom's angle above level in degrees: 90 upright, 0 level. */
    public static float angle(float swing) {
        return 90 * (1 - swing);
    }

    /**
     * The new length of a stop of {@code ticks} that ends early after {@code
     * elapsed}: the arm goes back from where it is now, as fast as it came
     * out, so its pose does not jump. A stop already over keeps its length.
     */
    public static int endedEarly(int ticks, long elapsed) {
        if (elapsed >= ticks || elapsed < 0) {
            return ticks;
        }
        return (int) (elapsed + reach(ticks, elapsed));
    }

    private static float ease(float value) {
        float clamped = Math.max(0, Math.min(1, value));
        return clamped * clamped * (3 - 2 * clamped);
    }
}
