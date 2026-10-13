package io.github.dirnei.mc.servus.storage.field;

/** When a Field Head calls a Spreader to its field. Pure Java. */
public enum FertilizeMode {
    /** Never. */
    OFF,
    /** Plots planted since their last dose that are not ripe get one round of doses. */
    AFTER_PLANTING,
    /** While the ripe share is below the head's fertilize threshold, unripe planted plots get doses. */
    UNTIL_RIPE;

    private static final FertilizeMode[] VALUES = values();

    public static FertilizeMode byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : OFF;
    }
}
