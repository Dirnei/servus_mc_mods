package io.github.dirnei.mc.servus.storage.power;

/**
 * The pay and wait decisions of the warehouse's moving parts: what a shuttle
 * segment costs, whether a shuttle may start it, and how a Crafting Station's
 * progress moves on a tick.
 */
public final class StepPower {

    private StepPower() {
    }

    /**
     * FE a segment costs: one block of rail, or a whole lift ride of
     * {@code height} blocks paid at once.
     */
    public static long segmentCost(boolean vertical, int height, double perBlock) {
        if (perBlock <= 0) {
            return 0;
        }
        int blocks = vertical ? Math.max(0, height) : 1;
        return (long) Math.ceil(blocks * perBlock);
    }

    /** A shuttle starts the segment only when its draw was paid; else it stands at the segment's start. */
    public static boolean mayStart(boolean paid) {
        return paid;
    }

    /** The craft's progress after a working tick: one further when the tick was paid, unchanged when not. */
    public static int advance(int progress, boolean paid) {
        return paid ? progress + 1 : progress;
    }
}
