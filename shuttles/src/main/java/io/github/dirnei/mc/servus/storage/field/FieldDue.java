package io.github.dirnei.mc.servus.storage.field;

/** Whether a field has work worth sending a harvester for. Pure Java. */
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
}
