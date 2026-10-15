package io.github.dirnei.mc.servus.alchemy.homunculus;

/** When the homunculus whispers next: after a delay drawn between two bounds, then again. Pure Java. */
public final class WhisperClock {

    /** The shortest pause between two whispers: one minute. */
    public static final int MIN_DELAY = 1200;
    /** The longest pause between two whispers: three minutes. */
    public static final int MAX_DELAY = 3600;

    private int ticksLeft = -1;

    /** A delay for a {@code roll} from 0 (inclusive) to 1 (exclusive). */
    public static int delay(double roll) {
        double clamped = Math.max(0, Math.min(roll, 1));
        return Math.min(MAX_DELAY, MIN_DELAY + (int) (clamped * (MAX_DELAY - MIN_DELAY + 1)));
    }

    /** Counts one tick; true when a whisper is due. {@code roll} draws the next delay when one starts. */
    public boolean tick(double roll) {
        if (ticksLeft < 0) {
            ticksLeft = delay(roll);
        }
        if (ticksLeft > 0) {
            ticksLeft--;
            return false;
        }
        ticksLeft = delay(roll);
        return true;
    }

    public int ticksLeft() {
        return ticksLeft;
    }
}
