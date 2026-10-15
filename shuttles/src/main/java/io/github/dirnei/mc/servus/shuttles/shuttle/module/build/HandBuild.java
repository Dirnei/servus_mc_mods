package io.github.dirnei.mc.servus.shuttles.shuttle.module.build;

/**
 * The order of a build by hand as the server sees it: which step it is on,
 * which report it accepts and when it is done. The client plays the steps;
 * the server only counts them. Pure Java.
 */
public final class HandBuild {

    private HandBuild() {
    }

    /** The step after a report: one further when the reported step is the current one, else unchanged. */
    public static int next(int current, int reported, int total) {
        return reported == current && current < total ? current + 1 : current;
    }

    /** Whether every step is done and the build waits for its name. */
    public static boolean done(int step, int total) {
        return step >= total;
    }

    /** A saved step kept within 0 and the step count, for loads and changed datapacks. */
    public static int clamp(int step, int total) {
        return Math.max(0, Math.min(Math.max(0, total), step));
    }

    /** The seed of a board, from the workshop's position and the step, so a reopened GUI shows the same layout. */
    public static long seed(long position, int step) {
        return position * 31L + step * 1_000_003L;
    }
}
