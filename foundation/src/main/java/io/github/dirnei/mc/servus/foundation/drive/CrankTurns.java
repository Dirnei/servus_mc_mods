package io.github.dirnei.mc.servus.foundation.drive;

/**
 * Whether a player holds a crank, and its turns while held. Holding the use key
 * repeats a click every {@link #TICKS_PER_CLICK} ticks; each click drives the
 * crank until the next one is due, whether it arrives before or after the crank's
 * tick, and keeps it held a little longer, so one late click does not stop it.
 * Each driving tick hands out its share of a turn's work. Pure Java.
 */
public final class CrankTurns {

    /** Ticks between two clicks while the use key is held. */
    public static final int TICKS_PER_CLICK = 4;
    /** Ticks a click keeps the crank turning. */
    public static final int HOLD_TICKS = 2 * TICKS_PER_CLICK;
    /** Driving ticks of one full turn: four clicks. */
    public static final int TICKS_PER_TURN = 4 * TICKS_PER_CLICK;

    /**
     * One driving tick.
     *
     * @param work     this tick's share of the turn's work
     * @param turnDone whether this tick completed a full turn
     */
    public record Step(int work, boolean turnDone) {
    }

    private final int ticksPerTurn;
    private int ticks;
    private long lastClick = Long.MIN_VALUE / 2;

    public CrankTurns() {
        this(TICKS_PER_TURN);
    }

    public CrankTurns(int ticksPerTurn) {
        this.ticksPerTurn = Math.max(1, ticksPerTurn);
    }

    /** A click at game time {@code now}. */
    public void click(long now) {
        lastClick = now;
    }

    /** Whether the crank gives drive at game time {@code now}. */
    public boolean driving(long now) {
        return now - lastClick <= TICKS_PER_CLICK;
    }

    /** Whether the crank counts as held, and so turning, at game time {@code now}. */
    public boolean held(long now) {
        return now - lastClick <= HOLD_TICKS;
    }

    /** Counts one driving tick; over a full turn the shares add up to {@code workPerTurn}. */
    public Step step(int workPerTurn) {
        int before = workPerTurn * ticks / ticksPerTurn;
        ticks++;
        int after = workPerTurn * ticks / ticksPerTurn;
        boolean turnDone = ticks == ticksPerTurn;
        if (turnDone) {
            ticks = 0;
        }
        return new Step(after - before, turnDone);
    }

    /** Driving ticks done of the turn in progress. */
    public int ticks() {
        return ticks;
    }
}
