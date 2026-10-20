package io.github.dirnei.mc.servus.brewery.brewhouse;

import java.util.OptionalInt;

/** The timing of a kiln tray. Pure Java. Progress counts ticks over a hot fire. */
public final class KilnClock {

    private KilnClock() {
    }

    /** Whether a tray in {@code mode} makes the dark result of a recipe that has {@code darkTime}. */
    public static boolean makesDark(KilnMode mode, OptionalInt darkTime) {
        return mode == KilnMode.DARK && darkTime.isPresent();
    }

    /** Ticks over a hot fire until the batch is done. */
    public static int target(int time, OptionalInt darkTime, KilnMode mode) {
        int pale = Math.max(1, time);
        return makesDark(mode, darkTime) ? pale + Math.max(0, darkTime.getAsInt()) : pale;
    }

    /** The progress after one tick: it counts only while the fire is hot. */
    public static int step(int progress, boolean hot) {
        return hot ? progress + 1 : progress;
    }

    public static boolean done(int progress, int target) {
        return progress >= target;
    }
}
