package io.github.dirnei.mc.servus.shuttles.field;

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
    /** The buffer is empty and the warehouse holds none of the fertilizer. */
    NO_STOCK,
    /** The buffer is empty and the warehouse holds the fertilizer only at or below the reserve. */
    BELOW_RESERVE,
    /** The buffer is empty, stock is there, but no shuttle without a tool can bring it. */
    NO_SUPPLY_SHUTTLE,
    /** The buffer is empty and a supply run brings fertilizer. */
    SUPPLY_COMING,
    /** The stock of the produce is at the fertilize level, or harvesting is paused at the limit. */
    NOT_NEEDED,
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
        return this == NO_SPREADER || this == NO_WAY || this == NO_SUPPLY_SHUTTLE;
    }

    /**
     * The status while the stock levels may make fertilizing unneeded: every
     * status that is not off, a problem or a run at work says so instead.
     */
    public FertilizeStatus whileNeeded(boolean needed) {
        return needed || this == OFF || this == NOT_LINKED || this == WORKING || problem() ? this : NOT_NEEDED;
    }

    /**
     * Picks the status in the order the warehouse checks it.
     *
     * @param on        a fertilizer is chosen and the mode is not off
     * @param linked    the field's rails are on a warehouse with one controller
     * @param spreader  a garage of the warehouse holds a Spreader shuttle
     * @param reachable some Spreader shuttle reaches the field without a lift
     * @param working   a fertilize run for this field is queued or under way
     * @param buffered  fertilizer items the head's buffer holds
     * @param stock     fertilizer items the warehouse holds
     * @param reserve   items the field keeps in stock
     * @param supplier  a shuttle without a tool can drive to the head's rail
     */
    public static FertilizeStatus of(boolean on, boolean linked, boolean spreader, boolean reachable, boolean working, int buffered,
                                     long stock, int reserve, boolean supplier) {
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
        if (buffered > 0) {
            return WAITING;
        }
        if (stock <= 0) {
            return NO_STOCK;
        }
        if (stock <= reserve) {
            return BELOW_RESERVE;
        }
        return supplier ? SUPPLY_COMING : NO_SUPPLY_SHUTTLE;
    }
}
