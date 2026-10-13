package io.github.dirnei.mc.servus.storage.field;

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
    /** Farmland waits for seeds the warehouse does not hold. */
    NO_SEEDS,
    /** Everything is planted and growing. */
    GROWING,
    /** No plot beside the rails can be tilled or planted. */
    NO_PLOTS;

    private static final FieldWork[] VALUES = values();

    /** Whether this status is a problem: drawn in the warning colour, a red tab marker and a red bar on the head. */
    public boolean problem() {
        return switch (this) {
            case WORKING, HARVEST_DUE, READY, GROWING, WAITING_RIPE -> false;
            default -> true;
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
     * @param seeds      the warehouse holds the crop item
     */
    public static FieldWork of(boolean linked, boolean online, boolean cropSet, boolean harvester, boolean reachable, boolean working,
            boolean harvestDue, boolean plantMode, boolean hydrated, int tillable, int plantable, int planted, boolean seeds) {
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
            if (tillable > 0 || seeds) {
                return READY;
            }
            return NO_SEEDS;
        }
        return planted > 0 ? GROWING : NO_PLOTS;
    }
}
