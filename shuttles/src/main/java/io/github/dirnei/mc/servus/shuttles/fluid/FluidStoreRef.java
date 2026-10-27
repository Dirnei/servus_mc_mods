package io.github.dirnei.mc.servus.shuttles.fluid;

import net.minecraft.core.BlockPos;

/**
 * One fluid store of a warehouse: a tank of a fluid station, named by the
 * station's lower half and the tank index, or a Fluid Rack section, named
 * by its port bay and the tank {@link #RACK}.
 */
public record FluidStoreRef(BlockPos pos, int tank) {

    /** The tank index that marks a Fluid Rack section. */
    public static final int RACK = -1;

    public static FluidStoreRef rack(BlockPos port) {
        return new FluidStoreRef(port, RACK);
    }

    public boolean isRack() {
        return tank == RACK;
    }
}
