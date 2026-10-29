package io.github.dirnei.mc.servus.shuttles.handheld;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Follows each player's held-use sweep across bays: the bay scanned last and
 * how many bays the sweep has scanned. A repeat on the same bay is skipped;
 * a quiet gap without any scan starts a new sweep. Pure Java.
 *
 * @param <P> the player key
 * @param <B> the bay key
 */
public final class SweepTracker<P, B> {

    /** The result for a repeat on the bay scanned last. */
    public static final int SKIP = -1;

    private record Sweep<B>(B bay, int count, long lastTick) {
    }

    private final int quietTicks;
    private final Map<P, Sweep<B>> sweeps = new HashMap<>();

    /** @param quietTicks ticks without a scan after which the next scan starts a new sweep */
    public SweepTracker(int quietTicks) {
        this.quietTicks = quietTicks;
    }

    /**
     * Notes a scan of {@code bay} at tick {@code now}. Returns {@link #SKIP}
     * when it repeats the bay scanned last within the sweep, else the bay's
     * index in the sweep, 0 for the first bay.
     */
    public int scan(P player, B bay, long now) {
        Sweep<B> sweep = sweeps.get(player);
        boolean going = sweep != null && now - sweep.lastTick() <= quietTicks;
        if (going && Objects.equals(sweep.bay(), bay)) {
            sweeps.put(player, new Sweep<>(bay, sweep.count(), now));
            return SKIP;
        }
        int index = going ? sweep.count() : 0;
        sweeps.put(player, new Sweep<>(bay, index + 1, now));
        return index;
    }

    /** Forgets a player, for example on logout. */
    public void forget(P player) {
        sweeps.remove(player);
    }
}
