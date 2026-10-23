package io.github.dirnei.mc.servus.shuttles.garage;

/**
 * When the roll-up shutter of a garage's doorway is open, worked out by the
 * client from where the garage's shuttles are drawn. A shuttle's way through
 * the doorway starts or ends at the garage's own cell, path node 0 or the
 * last. Pure Java.
 */
public final class GarageShutter {

    /** Ticks the shutter takes from shut to open and back; a little less than the door time, so a leaving shuttle never meets it shut. */
    public static final double TRAVEL_TICKS = 8;
    /** Blocks a shuttle on its way out is past the garage's cell before the shutter rolls down behind it. */
    public static final double CLEAR = 1.5;

    private GarageShutter() {
    }

    /**
     * Whether a shuttle wants the shutter open: on its way out until it is
     * {@link #CLEAR} past the cell, on its way in from as far ahead as it
     * drives at its top speed while the shutter opens until it stands inside.
     *
     * @param startsInside whether its path starts in the garage's cell
     * @param endsInside   whether its path ends there
     * @param nodes        nodes on the path
     * @param at           where it is on the path: segment plus progress
     * @param top          its top speed in blocks per tick, 0 while it stands
     */
    public static boolean wantsOpen(boolean startsInside, boolean endsInside, int nodes, double at, double top) {
        if (startsInside && at < CLEAR) {
            return true;
        }
        return endsInside && pullingIn(nodes, at, top);
    }

    /**
     * Whether a shuttle on a path into the garage's cell is close enough for
     * the shutter to open for it: from {@link #CLEAR} plus what it drives at
     * its top speed while the shutter opens, until it stands inside. The
     * server keeps such a shuttle from new jobs until it has parked.
     *
     * @param nodes nodes on the path
     * @param at    where it is on the path: segment plus progress
     * @param top   its top speed in blocks per tick
     */
    public static boolean pullingIn(int nodes, double at, double top) {
        double left = (nodes - 1) - at;
        return left > 1e-6 && left <= CLEAR + top * TRAVEL_TICKS;
    }

    /** The shutter's openness, 0 shut to 1 open, after moving towards open or shut for that many ticks. */
    public static double step(double openness, boolean open, double ticks) {
        double change = Math.max(0, ticks) / TRAVEL_TICKS;
        return open ? Math.min(1, openness + change) : Math.max(0, openness - change);
    }
}
