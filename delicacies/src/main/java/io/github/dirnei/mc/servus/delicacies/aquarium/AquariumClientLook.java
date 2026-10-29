package io.github.dirnei.mc.servus.delicacies.aquarium;

/**
 * What the client keeps to draw one aquarium: a swimming clock that runs slower in dirty water, and the fish floating
 * belly up after deaths. Plain data, written by the block entity and the renderer on the client.
 */
public final class AquariumClientLook {

    /** Ticks the dead fish float before they disappear. */
    public static final int FLOAT_TICKS = 160;
    /** Most fish that float after one cycle. */
    public static final int MAX_FLOATERS = 3;

    private double swimTime;
    private double lastClock = Double.NaN;
    private long floatersSince = Long.MIN_VALUE;
    private int floaters;

    /** Advances the swimming clock to {@code clock} (game time plus partial tick) at {@code speed} and returns it. */
    public double swim(double clock, double speed) {
        if (!Double.isNaN(lastClock) && clock > lastClock) {
            swimTime += (clock - lastClock) * speed;
        }
        lastClock = clock;
        return swimTime;
    }

    /** Records that {@code died} fish died at {@code gameTime}. */
    public void fishDied(long gameTime, int died) {
        floatersSince = gameTime;
        floaters = Math.max(1, Math.min(MAX_FLOATERS, died));
    }

    /** The fish floating at {@code gameTime}, 0 once they are gone. */
    public int floaters(long gameTime) {
        return gameTime - floatersSince < FLOAT_TICKS && gameTime >= floatersSince ? floaters : 0;
    }

    /** Ticks since the fish died, for their slow rise and fade. */
    public long floatingFor(long gameTime) {
        return gameTime - floatersSince;
    }
}
