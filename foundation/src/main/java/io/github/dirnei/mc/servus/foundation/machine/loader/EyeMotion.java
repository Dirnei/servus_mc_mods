package io.github.dirnei.mc.servus.foundation.machine.loader;

/**
 * How the Chunk Loader's ender eye moves: it opens while the loader holds its
 * chunk and closes when it lets go, blinks now and then on a schedule of its
 * own, and turns its iris toward the nearest player in front of it. Pure, look
 * only; the renderer feeds it times in seconds and offsets in blocks.
 */
public final class EyeMotion {

    /** Openness gained or lost per second: about a second from shut to open. */
    public static final float OPEN_SPEED = 1.0F;
    /** Each blink window is this long; one blink falls into the first {@link #JITTER} seconds of each. */
    public static final double WINDOW = 6.0;
    public static final double JITTER = 3.0;
    /** How long one blink takes, closing and opening again. */
    public static final double BLINK = 0.15;
    /** The farthest the iris moves from the centre of the lens, in pixels. */
    public static final float IRIS_RADIUS = 1.0F;
    /** Players farther than this, in blocks, are not watched. */
    public static final double RANGE = 8.0;
    /** How fast the iris eases toward its target, per second. */
    public static final float EASE = 8.0F;

    private EyeMotion() {
    }

    /** The eye's openness, 0 shut to 1 open, after {@code dt} seconds of being active or not. */
    public static float openness(float previous, boolean active, float dt) {
        float next = previous + (active ? OPEN_SPEED : -OPEN_SPEED) * dt;
        return Math.max(0.0F, Math.min(1.0F, next));
    }

    /**
     * How far a blink has closed the lid at {@code time} seconds, 0 to 1. Each
     * window of {@link #WINDOW} seconds holds one blink at a point picked from
     * {@code seed} and the window number, so blinks come 3 to 9 seconds apart
     * and two eyes with different seeds blink at different times.
     */
    public static float blink(long seed, double time) {
        long window = (long) Math.floor(time / WINDOW);
        for (long w = window - 1; w <= window; w++) {
            double start = blinkStart(seed, w);
            if (time >= start && time < start + BLINK) {
                double half = (time - start) / BLINK * 2;
                return (float) (half <= 1 ? half : 2 - half);
            }
        }
        return 0.0F;
    }

    /** When the blink of a window starts, in seconds. */
    public static double blinkStart(long seed, long window) {
        return window * WINDOW + unit(seed, window) * JITTER;
    }

    /** A number from 0 to below 1 picked from the seed and the window, the same every time. */
    private static double unit(long seed, long window) {
        long h = seed * 0x9E3779B97F4A7C15L + window * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    /** The lid's closure: shut while closed, and briefly shut again while blinking. */
    public static float lid(float openness, float blink) {
        return 1.0F - openness * (1.0F - blink);
    }

    /**
     * The iris offset in pixels inside the lens, {x, y}, for a player at
     * {@code (dx, dy, dz)} blocks from the eye. {@code quarters} turns the
     * north built eye like the blockstate does (north 0, east 1, south 2, west
     * 3); x is the model's x, which for an eye facing north is the viewer's
     * left. Zero when the player stands behind the front or beyond the range.
     */
    public static float[] iris(double dx, double dy, double dz, int quarters) {
        // Turn the offset back into the north built frame, where the eye looks toward -z.
        double x = dx;
        double z = dz;
        for (int i = 0; i < Math.floorMod(quarters, 4); i++) {
            double nx = z;
            z = -x;
            x = nx;
        }
        double distance = Math.sqrt(x * x + dy * dy + z * z);
        if (z >= 0 || distance > RANGE || distance < 1e-6) {
            return new float[] {0, 0};
        }
        return new float[] {(float) (IRIS_RADIUS * x / distance), (float) (IRIS_RADIUS * dy / distance)};
    }

    /** Eases a value toward a target over {@code dt} seconds. */
    public static float smooth(float previous, float target, float dt) {
        return previous + (target - previous) * (float) (1 - Math.exp(-EASE * dt));
    }
}
