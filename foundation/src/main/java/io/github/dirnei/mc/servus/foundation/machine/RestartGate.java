package io.github.dirnei.mc.servus.foundation.machine;

/**
 * Decides whether a machine that fills a buffer may run, with a restart threshold: once the buffer is full the
 * machine waits until the buffer holds less than {@code restartBelow} times its capacity, then runs until it is
 * full again. A threshold of 1 lets the machine run whenever the buffer is not full.
 */
public final class RestartGate {

    private boolean waiting;

    /**
     * Whether the machine may run this tick. Ask once per tick.
     *
     * @param full         whether the buffer cannot take another work cycle
     * @param stored       what the buffer holds
     * @param capacity     what the buffer can hold
     * @param restartBelow the fraction of {@code capacity} the buffer must drop below before the machine runs again
     */
    public boolean allows(boolean full, long stored, long capacity, double restartBelow) {
        if (full) {
            waiting = true;
            return false;
        }
        if (waiting && stored >= capacity * restartBelow) {
            return false;
        }
        waiting = false;
        return true;
    }

    /** Whether the machine waits for its buffer to drop below the threshold. */
    public boolean waiting() {
        return waiting;
    }

    /** Restores the waiting state, for example from a save. */
    public void setWaiting(boolean waiting) {
        this.waiting = waiting;
    }
}
