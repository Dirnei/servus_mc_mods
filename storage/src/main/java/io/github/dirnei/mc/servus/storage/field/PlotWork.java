package io.github.dirnei.mc.servus.storage.field;

/** What a harvester does on one plot. Pure Java. */
public final class PlotWork {

    /** The steps on one plot, in this order: harvest, till, plant. */
    public record Action(boolean harvest, boolean till, boolean plant) {

        public static final Action NONE = new Action(false, false, false);

        public boolean any() {
            return harvest || till || plant;
        }
    }

    private PlotWork() {
    }

    /**
     * @param state     what the plot holds
     * @param plantMode the field has a crop set and is not in harvest only mode
     * @param haveSeed  the harvester carries the crop item; a harvested plot replants from its own drops
     * @param harvest   the field harvests; while it is paused ripe plots are left standing
     */
    public static Action plan(PlotState state, boolean plantMode, boolean haveSeed, boolean harvest) {
        return switch (state) {
            case RIPE -> harvest ? new Action(true, false, plantMode) : Action.NONE;
            case TILLABLE -> plantMode ? new Action(false, true, haveSeed) : Action.NONE;
            case PLANTABLE -> plantMode && haveSeed ? new Action(false, false, true) : Action.NONE;
            case GROWING, BLOCKED -> Action.NONE;
        };
    }
}
