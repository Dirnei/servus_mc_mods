package io.github.dirnei.mc.servus.delicacies.crock;

/**
 * A fermentation timed on game ticks: when it started and how long it takes. Only the current game time decides
 * whether it is done, so time that passes while nobody looks at the crock counts as well.
 */
public final class Fermentation {

    public static final int TICKS_PER_HOUR = 1000;
    public static final int TICKS_PER_DAY = 24000;
    private static final long NONE = -1;

    private long startedAt = NONE;
    private int duration;

    public void start(long now, int duration) {
        this.startedAt = now;
        this.duration = Math.max(0, duration);
    }

    public void stop() {
        startedAt = NONE;
        duration = 0;
    }

    public boolean active() {
        return startedAt != NONE;
    }

    public boolean done(long now) {
        return active() && now - startedAt >= duration;
    }

    /** Ticks until it is done; 0 when done or not active. */
    public long remaining(long now) {
        return active() ? Math.max(0, startedAt + duration - now) : 0;
    }

    /** In-game hours until it is done, rounded up. */
    public long hoursLeft(long now) {
        return (remaining(now) + TICKS_PER_HOUR - 1) / TICKS_PER_HOUR;
    }

    /** Ticks until the next look: when it is done, but no later than {@code interval}, and at least one. */
    public int nextCheck(long now, int interval) {
        return (int) Math.max(1, Math.min(remaining(now), interval));
    }

    /** Moves the start back, as if {@code ticks} of game time had passed. */
    public void backdate(long ticks) {
        if (active()) {
            startedAt -= ticks;
        }
    }

    /** Puts back a saved fermentation; a negative {@code startedAt} means none. */
    public void restore(long startedAt, int duration) {
        if (startedAt < 0) {
            stop();
        } else {
            this.startedAt = startedAt;
            this.duration = Math.max(0, duration);
        }
    }

    /** The game tick it started at, or a negative value when not active. */
    public long startedAt() {
        return startedAt;
    }

    public int duration() {
        return duration;
    }

    /** {@code ticks} in in-game days. */
    public static double days(int ticks) {
        return ticks / (double) TICKS_PER_DAY;
    }
}
