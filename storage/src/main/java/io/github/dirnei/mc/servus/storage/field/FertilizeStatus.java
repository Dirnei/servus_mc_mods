package io.github.dirnei.mc.servus.storage.field;

/**
 * What the fertilizing of a field is doing, or the first thing that keeps a
 * Spreader away, for the Fertilizer tab and the field command. Pure Java.
 */
public enum FertilizeStatus {
    /** No fertilizer chosen or the mode is off. */
    OFF,
    /** The rails are not on a warehouse with exactly one controller. */
    NOT_LINKED,
    /** No garage of the warehouse holds a Spreader shuttle. */
    NO_SPREADER,
    /** No Spreader shuttle reaches the field without riding a lift car. */
    NO_WAY,
    /** A fertilize run is queued or under way. */
    WORKING,
    /** The warehouse holds none of the fertilizer. */
    NO_STOCK,
    /** The warehouse holds the fertilizer only at or below the reserve. */
    BELOW_RESERVE,
    /** Nothing to dose right now, or the call interval has not passed. */
    WAITING;

    private static final FertilizeStatus[] VALUES = values();

    public static FertilizeStatus byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : OFF;
    }

    /**
     * Whether the Fertilizer tab marks this status as a problem. An empty stock or one at the reserve is not
     * a problem: the head simply waits until there is fertilizer to spare.
     */
    public boolean problem() {
        return this == NO_SPREADER || this == NO_WAY;
    }

    /**
     * Picks the status in the order the warehouse checks it.
     *
     * @param on        a fertilizer is chosen and the mode is not off
     * @param linked    the field's rails are on a warehouse with one controller
     * @param spreader  a garage of the warehouse holds a Spreader shuttle
     * @param reachable some Spreader shuttle reaches the field without a lift
     * @param working   a fertilize run for this field is queued or under way
     * @param stock     fertilizer items the warehouse holds
     * @param reserve   items the field keeps in stock
     */
    public static FertilizeStatus of(boolean on, boolean linked, boolean spreader, boolean reachable, boolean working, long stock, int reserve) {
        if (!on) {
            return OFF;
        }
        if (!linked) {
            return NOT_LINKED;
        }
        if (!spreader) {
            return NO_SPREADER;
        }
        if (!reachable) {
            return NO_WAY;
        }
        if (working) {
            return WORKING;
        }
        if (stock <= 0) {
            return NO_STOCK;
        }
        if (stock <= reserve) {
            return BELOW_RESERVE;
        }
        return WAITING;
    }
}
