package io.github.dirnei.mc.servus.foundation.block;

import io.github.dirnei.mc.servus.foundation.side.RelativeSide;

/** The menu button ids every side panel uses: one per relative side. */
public final class SideConfigMenus {

    public static final int BUTTON_FIRST = 1000;

    private SideConfigMenus() {
    }

    /** The relative side a menu button id stands for, or null when it is not a side button. */
    public static RelativeSide sideOf(int buttonId) {
        int offset = buttonId - BUTTON_FIRST;
        return offset >= 0 && offset < RelativeSide.values().length ? RelativeSide.byOrdinal(offset) : null;
    }

    public static int buttonOf(RelativeSide side) {
        return BUTTON_FIRST + side.ordinal();
    }
}
