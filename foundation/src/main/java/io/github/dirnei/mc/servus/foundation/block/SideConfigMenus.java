package io.github.dirnei.mc.servus.foundation.block;

import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideLayer;

/** The menu button ids every side panel uses: one per relative side and layer. */
public final class SideConfigMenus {

    public static final int BUTTON_FIRST = 1000;
    /** Buttons of the Fluids layer follow the six Items buttons. */
    public static final int FLUID_BUTTON_FIRST = BUTTON_FIRST + 6;

    private SideConfigMenus() {
    }

    /** The relative side an Items button id stands for, or null when it is not one. */
    public static RelativeSide sideOf(int buttonId) {
        return side(buttonId - BUTTON_FIRST);
    }

    /** The relative side a Fluids button id stands for, or null when it is not one. */
    public static RelativeSide fluidSideOf(int buttonId) {
        return side(buttonId - FLUID_BUTTON_FIRST);
    }

    public static int buttonOf(RelativeSide side) {
        return BUTTON_FIRST + side.ordinal();
    }

    public static int buttonOf(SideLayer layer, RelativeSide side) {
        return (layer == SideLayer.FLUIDS ? FLUID_BUTTON_FIRST : BUTTON_FIRST) + side.ordinal();
    }

    private static RelativeSide side(int offset) {
        return offset >= 0 && offset < RelativeSide.values().length ? RelativeSide.byOrdinal(offset) : null;
    }
}
