package io.github.dirnei.mc.servus.shuttles.field;

/**
 * What a served field is doing, or the first thing that keeps a harvester
 * away, for the Field Head GUI and the field command. Pure Java.
 */
public enum FieldWork {
    /** The farm rails are not part of any warehouse. */
    NOT_LINKED,
    /** The warehouse has no controller, or more than one. */
    NO_CONTROLLER,
    /** Neither a crop nor harvest only is set. */
    NO_CROP,
    /** No garage of the warehouse holds a shuttle with a Harvester. */
    NO_HARVESTER,
    /** No Harvester shuttle can reach the field without riding a lift car. */
    NO_WAY,
    /** A field run is queued or under way. */
    WORKING,
    /** Enough is ripe; a run goes out at the next check. */
    HARVEST_DUE,
    /** Harvest only, nothing worth a trip yet. */
    WAITING_RIPE,
    /** Planting needs water in the head. */
    DRY,
    /** Plots to till or plant; a run goes out at the next check. */
    READY,
    /** Farmland waits for seeds: the head's buffer and the warehouse hold none. */
    NO_SEEDS,
    /** Farmland waits for a supply run to bring seeds into the head's buffer. */
    SEEDS_COMING,
    /** Farmland waits for seeds the warehouse holds, but no shuttle without a tool can bring them. */
    NO_SUPPLY_SHUTTLE,
    /** Everything is planted and growing. */
    GROWING,
    /** No plot beside the rails can be tilled or planted. */
    NO_PLOTS,
    /** The warehouse holds the head's harvest limit of the produce: ripe crops wait on the field. */
    HARVEST_PAUSED,
    /** The head's To storage buffer holds items no plain shuttle can reach to put away. */
    COLLECT_NO_SHUTTLE,
    /** The head's To storage buffer holds items no rack has room for. */
    COLLECT_NO_ROOM,
    /** The head's To storage buffer has no free slot: no run goes out until a shuttle collects. */
    TO_STORAGE_FULL;

    private static final FieldWork[] VALUES = values();

    /** Where seeds for farmland come from. */
    public enum Seeds {
        /** The head's buffer holds seeds of the crop. */
        BUFFERED,
        /** The buffer is empty, the warehouse holds seeds and a shuttle without a tool can bring them. */
        COMING,
        /** The buffer is empty and the warehouse holds seeds, but no shuttle without a tool reaches the head. */
        NO_SHUTTLE,
        /** Neither the buffer nor the warehouse holds seeds. */
        NONE;

        /**
         * @param buffered the head's buffer holds seeds of the crop
         * @param inStock  the warehouse holds seeds of the crop
         * @param supplier a shuttle without a tool can drive to the head's rail
         */
        public static Seeds of(boolean buffered, boolean inStock, boolean supplier) {
            if (buffered) {
                return BUFFERED;
            }
            if (!inStock) {
                return NONE;
            }
            return supplier ? COMING : NO_SHUTTLE;
        }
    }

    /** Whether this status is a problem: drawn in the warning colour, a red tab marker and a red bar on the head. */
    public boolean problem() {
        return switch (this) {
            case WORKING, HARVEST_DUE, READY, GROWING, WAITING_RIPE, SEEDS_COMING, HARVEST_PAUSED -> false;
            default -> true;
        };
    }

    /**
     * The status while the harvest limit may be reached: a field that only
     * waits for crops to grow or ripen says its harvest is paused instead.
     * Call with the status picked for a harvest that is not due.
     */
    public FieldWork whilePaused(boolean paused) {
        return paused && (this == WAITING_RIPE || this == GROWING) ? HARVEST_PAUSED : this;
    }

    /** How the head's To storage buffer is being put away. */
    public enum Collect {
        /** Empty, or plain shuttles are putting it away with room to spare. */
        FINE,
        /** No slot is free; a plain shuttle will collect. */
        FULL,
        /** It holds items and no plain shuttle reaches the head. */
        NO_SHUTTLE,
        /** It holds items no rack has room for. */
        NO_ROOM;

        /**
         * @param holdsItems the buffer holds items
         * @param collector  a shuttle without a tool can drive to the head's rail
         * @param noRoom     the last put-away found no rack with room for some of it
         * @param freeSlot   some slot is empty and free
         */
        public static Collect of(boolean holdsItems, boolean collector, boolean noRoom, boolean freeSlot) {
            if (!holdsItems) {
                return FINE;
            }
            if (!collector) {
                return NO_SHUTTLE;
            }
            if (noRoom) {
                return NO_ROOM;
            }
            return freeSlot ? FINE : FULL;
        }
    }

    /**
     * The status while the To storage buffer waits: once the field could be
     * worked at all, a buffer that cannot be emptied or has no free slot is
     * what the head shows.
     */
    public FieldWork whileCollecting(Collect collect) {
        boolean fieldWorkable = switch (this) {
            case NOT_LINKED, NO_CONTROLLER, NO_CROP, NO_HARVESTER, NO_WAY -> false;
            default -> true;
        };
        if (!fieldWorkable) {
            return this;
        }
        return switch (collect) {
            case FINE -> this;
            case NO_SHUTTLE -> COLLECT_NO_SHUTTLE;
            case NO_ROOM -> COLLECT_NO_ROOM;
            case FULL -> TO_STORAGE_FULL;
        };
    }

    public static FieldWork byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : NOT_LINKED;
    }

    /**
     * Picks the reason in the order the warehouse checks it, so the first
     * missing piece is the one shown.
     *
     * @param linked     the field's rails belong to a warehouse
     * @param online     that warehouse has exactly one controller
     * @param cropSet    a crop or harvest only is set
     * @param harvester  a garage of the warehouse holds a Harvester shuttle
     * @param reachable  some Harvester shuttle reaches the field without a lift
     * @param working    a run for this field is queued or under way
     * @param harvestDue the ripe share reached the threshold
     * @param plantMode  a crop is set and harvest only is off
     * @param hydrated   the field has water
     * @param tillable   empty plots on dirt or grass
     * @param plantable  empty plots on farmland
     * @param planted    plots holding a crop
     * @param seeds      where seeds for farmland come from
     */
    public static FieldWork of(boolean linked, boolean online, boolean cropSet, boolean harvester, boolean reachable, boolean working,
            boolean harvestDue, boolean plantMode, boolean hydrated, int tillable, int plantable, int planted, Seeds seeds) {
        if (!linked) {
            return NOT_LINKED;
        }
        if (!online) {
            return NO_CONTROLLER;
        }
        if (!cropSet) {
            return NO_CROP;
        }
        if (!harvester) {
            return NO_HARVESTER;
        }
        if (!reachable) {
            return NO_WAY;
        }
        if (working) {
            return WORKING;
        }
        if (harvestDue) {
            return HARVEST_DUE;
        }
        if (!plantMode) {
            return WAITING_RIPE;
        }
        if (tillable > 0 || plantable > 0) {
            if (!hydrated) {
                return DRY;
            }
            if (tillable > 0 || seeds == Seeds.BUFFERED) {
                return READY;
            }
            return switch (seeds) {
                case COMING -> SEEDS_COMING;
                case NO_SHUTTLE -> NO_SUPPLY_SHUTTLE;
                default -> NO_SEEDS;
            };
        }
        return planted > 0 ? GROWING : NO_PLOTS;
    }
}
