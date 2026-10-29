package io.github.dirnei.mc.servus.shuttles.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Which moving shuttles sound and at what pitch: only the few nearest the
 * listener play their drive, and each plays it a little higher or lower than
 * the others, higher the faster it goes.
 */
public final class ShuttleSoundPick {

    public static final float MIN_PITCH = 0.8F;
    public static final float MAX_PITCH = 1.4F;
    public static final float MIN_JITTER = 0.95F;
    public static final float MAX_JITTER = 1.05F;

    /** A shuttle drawn this tick: its key, where it is and its speed in blocks per tick. */
    public record Mover<K>(K key, double x, double y, double z, double speed) {

        double distanceSquared(double lx, double ly, double lz) {
            double dx = x - lx;
            double dy = y - ly;
            double dz = z - lz;
            return dx * dx + dy * dy + dz * dz;
        }
    }

    private ShuttleSoundPick() {
    }

    /**
     * The keys of the at most {@code limit} moving shuttles nearest the
     * listener, nearest first. A shuttle with no speed is never picked;
     * shuttles at the same distance keep their order in {@code movers}.
     */
    public static <K> List<K> nearest(List<Mover<K>> movers, double lx, double ly, double lz, int limit) {
        List<Mover<K>> moving = new ArrayList<>();
        for (Mover<K> mover : movers) {
            if (mover.speed() > 0) {
                moving.add(mover);
            }
        }
        moving.sort(Comparator.comparingDouble(mover -> mover.distanceSquared(lx, ly, lz)));
        List<K> keys = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, moving.size()); i++) {
            keys.add(moving.get(i).key());
        }
        return keys;
    }

    /**
     * The drive's pitch at that speed: the square root of its share of the
     * reference speed, kept between {@link #MIN_PITCH} and {@link #MAX_PITCH},
     * times the shuttle's own jitter.
     */
    public static float pitch(double speed, double reference, float jitter) {
        double bend = reference <= 0 ? 1 : Math.sqrt(Math.max(0, speed) / reference);
        return (float) Math.clamp(bend, MIN_PITCH, MAX_PITCH) * jitter;
    }

    /** A shuttle's own pitch factor between {@link #MIN_JITTER} and {@link #MAX_JITTER}, always the same for the same key hash. */
    public static float jitter(long keyHash) {
        long mixed = mix(keyHash);
        double share = (mixed >>> 11) * 0x1.0p-53;
        return (float) (MIN_JITTER + (MAX_JITTER - MIN_JITTER) * share);
    }

    /** SplitMix64's finaliser: spreads close hashes far apart. */
    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
