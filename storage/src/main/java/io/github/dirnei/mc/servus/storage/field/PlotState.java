package io.github.dirnei.mc.servus.storage.field;

/** What one plot of a field holds, as far as field work cares. Pure, no Minecraft imports. */
public enum PlotState {
    /** Something else is there: a block on the plot, or no soil to till or plant under it. */
    BLOCKED,
    /** Empty or overgrown with short grass, on dirt or a grass block that a hoe turns into farmland. */
    TILLABLE,
    /** Empty or overgrown, on farmland. */
    PLANTABLE,
    /** An age crop that is not ripe yet. */
    GROWING,
    /** An age crop at its last stage. */
    RIPE;

    /** The state from the old planted and ripe flags; anything unplanted counts as blocked. */
    public static PlotState of(boolean planted, boolean ripe) {
        return ripe ? RIPE : planted ? GROWING : BLOCKED;
    }
}
