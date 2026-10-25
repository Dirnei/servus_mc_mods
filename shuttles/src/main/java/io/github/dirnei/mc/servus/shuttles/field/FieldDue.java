package io.github.dirnei.mc.servus.shuttles.field;

/** Whether a field has work worth sending a field shuttle for. Pure Java. */
public final class FieldDue {

    private FieldDue() {
    }

    /**
     * @param harvestDue       the head signals harvest due
     * @param plantMode        a crop is set and harvest only is off
     * @param hydrated         the field has water
     * @param tillable         empty plots on dirt or grass
     * @param plantable        empty plots on farmland
     * @param seedsInBuffer    the head's buffer holds the crop item
     */
    public static boolean due(boolean harvestDue, boolean plantMode, boolean hydrated, int tillable, int plantable, boolean seedsInBuffer) {
        return harvestDue || plantMode && hydrated && (tillable > 0 || plantable > 0 && seedsInBuffer);
    }

    /**
     * A Trees field needs no water and is never tilled: grown trees to fell,
     * or empty spots while the head's buffer holds saplings.
     *
     * @param harvestDue       the head signals harvest due
     * @param plantMode        a sapling is set and fell only is off
     * @param openSpots        empty spots and square plots to plant
     * @param saplingsInBuffer the head's buffer holds the sapling
     */
    public static boolean dueTrees(boolean harvestDue, boolean plantMode, int openSpots, boolean saplingsInBuffer) {
        return due(harvestDue, plantMode, true, 0, openSpots, saplingsInBuffer);
    }
}
