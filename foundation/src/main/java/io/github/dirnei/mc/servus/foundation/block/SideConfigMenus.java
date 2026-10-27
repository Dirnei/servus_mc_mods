package io.github.dirnei.mc.servus.foundation.block;

import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideLayer;

/** The menu button ids every side panel uses: one per relative side and layer, plus one per side for the tank choice. */
public final class SideConfigMenus {

    public static final int BUTTON_FIRST = 1000;
    /** Buttons cycling the Fluids mode follow the six Items buttons. */
    public static final int FLUID_BUTTON_FIRST = BUTTON_FIRST + 6;
    /** Buttons cycling the Fluids tank choice follow the six Fluids mode buttons. */
    public static final int FLUID_TANK_BUTTON_FIRST = FLUID_BUTTON_FIRST + 6;

    private SideConfigMenus() {
    }

    /** The relative side an Items button id stands for, or null when it is not one. */
    public static RelativeSide sideOf(int buttonId) {
        return side(buttonId - BUTTON_FIRST);
    }

    /** The relative side a Fluids mode button id stands for, or null when it is not one. */
    public static RelativeSide fluidSideOf(int buttonId) {
        return side(buttonId - FLUID_BUTTON_FIRST);
    }

    /** The relative side a Fluids tank button id stands for, or null when it is not one. */
    public static RelativeSide fluidTankSideOf(int buttonId) {
        return side(buttonId - FLUID_TANK_BUTTON_FIRST);
    }

    public static int buttonOf(RelativeSide side) {
        return BUTTON_FIRST + side.ordinal();
    }

    public static int buttonOf(SideLayer layer, RelativeSide side) {
        return (layer == SideLayer.FLUIDS ? FLUID_BUTTON_FIRST : BUTTON_FIRST) + side.ordinal();
    }

    public static int buttonOfTank(RelativeSide side) {
        return FLUID_TANK_BUTTON_FIRST + side.ordinal();
    }

    /**
     * Runs a side panel button on the server: cycles an Items mode, a Fluids mode or a Fluids tank
     * choice. Returns false when the id is no side button or the block lacks that layer.
     */
    public static boolean click(SidedBlockEntity block, int buttonId) {
        RelativeSide side = sideOf(buttonId);
        if (side != null && block.hasItemsLayer()) {
            block.cycleSide(side);
            return true;
        }
        if (block.fluidSides() == null) {
            return false;
        }
        RelativeSide fluidSide = fluidSideOf(buttonId);
        if (fluidSide != null) {
            block.cycleFluidMode(fluidSide);
            return true;
        }
        RelativeSide tankSide = fluidTankSideOf(buttonId);
        if (tankSide != null) {
            block.cycleFluidTank(tankSide);
            return true;
        }
        return false;
    }

    private static RelativeSide side(int offset) {
        return offset >= 0 && offset < RelativeSide.values().length ? RelativeSide.byOrdinal(offset) : null;
    }
}
