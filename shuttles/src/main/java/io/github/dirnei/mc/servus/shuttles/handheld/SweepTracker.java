package io.github.dirnei.mc.servus.shuttles.handheld;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Follows each player's held-use sweep across shelves: the shelf scanned
 * last and how many shelves the sweep has scanned. A repeat on the same shelf
 * (any of its bays gives the same key) is skipped; a quiet gap without any
 * scan starts a new sweep. Pure Java.
 *
 * @param <P> the player key
 * @param <S> the shelf key, for example the shelf's leftmost bay
 */
public final class SweepTracker<P, S> {

    /** The result for a repeat on the shelf scanned last. */
    public static final int SKIP = -1;

    private record Sweep<S>(S shelf, int count, long lastTick) {
    }

    private final int quietTicks;
    private final Map<P, Sweep<S>> sweeps = new HashMap<>();

    /** @param quietTicks ticks without a scan after which the next scan starts a new sweep */
    public SweepTracker(int quietTicks) {
        this.quietTicks = quietTicks;
    }

    /**
     * Notes a scan of {@code shelf} at tick {@code now}. Returns {@link #SKIP}
     * when it repeats the shelf scanned last within the sweep, else the shelf's
     * index in the sweep, 0 for the first shelf.
     */
    public int scan(P player, S shelf, long now) {
        Sweep<S> sweep = sweeps.get(player);
        boolean going = sweep != null && now - sweep.lastTick() <= quietTicks;
        if (going && Objects.equals(sweep.shelf(), shelf)) {
            sweeps.put(player, new Sweep<>(shelf, sweep.count(), now));
            return SKIP;
        }
        int index = going ? sweep.count() : 0;
        sweeps.put(player, new Sweep<>(shelf, index + 1, now));
        return index;
    }

    /** Forgets a player, for example on logout. */
    public void forget(P player) {
        sweeps.remove(player);
    }
}
