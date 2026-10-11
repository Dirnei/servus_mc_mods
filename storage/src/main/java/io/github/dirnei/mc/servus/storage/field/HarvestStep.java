package io.github.dirnei.mc.servus.storage.field;

/** The next thing a harvester on a field run does at each turn. Pure, no Minecraft imports. */
public enum HarvestStep {
    FETCH_SEEDS,
    WORK,
    PUT_AWAY,
    HOME;

    /**
     * @param serving     the field still has its head serving it
     * @param cargoFull   the last harvest did not fit into the cargo
     * @param seedsWanted crop items to pick from the warehouse first
     * @param room        whether the cargo has room for them
     * @param workLeft    some rail of the field has work the harvester can do
     * @param hasCargo    the harvester carries something to put away
     */
    public static HarvestStep next(boolean serving, boolean cargoFull, int seedsWanted, boolean room, boolean workLeft, boolean hasCargo) {
        if (!serving) {
            return hasCargo ? PUT_AWAY : HOME;
        }
        if (cargoFull) {
            return PUT_AWAY;
        }
        if (seedsWanted > 0) {
            if (room) {
                return FETCH_SEEDS;
            }
            if (hasCargo) {
                return PUT_AWAY;
            }
        }
        if (workLeft) {
            return WORK;
        }
        return hasCargo ? PUT_AWAY : HOME;
    }
}
