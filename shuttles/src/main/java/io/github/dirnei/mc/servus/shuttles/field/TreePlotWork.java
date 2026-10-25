package io.github.dirnei.mc.servus.shuttles.field;

/** What a Lumber shuttle does on one plot of a Trees field. Pure Java. */
public final class TreePlotWork {

    /** The steps on one plot, in this order: fell, plant. */
    public record Action(boolean fell, boolean plant) {

        public static final Action NONE = new Action(false, false);

        public boolean any() {
            return fell || plant;
        }
    }

    private TreePlotWork() {
    }

    /**
     * @param state            what the plot holds
     * @param plantMode        the field has a sapling set, is not fell only, and the plot is a spot or a plot of a square planted now
     * @param saplingsCarried  saplings the shuttle carries, those kept from the tree's drops included
     * @param saplingsNeeded   saplings planting needs at once: one for a spot, the empty plots of its square for a square
     * @param harvest          the field fells; while it is paused grown trees are left standing
     */
    public static Action plan(PlotState state, boolean plantMode, int saplingsCarried, int saplingsNeeded, boolean harvest) {
        boolean fell = state == PlotState.RIPE && harvest;
        boolean open = state == PlotState.PLANTABLE || fell;
        boolean plant = open && plantMode && saplingsNeeded > 0 && saplingsCarried >= saplingsNeeded;
        return new Action(fell, plant);
    }
}
