package io.github.dirnei.mc.servus.shuttles.fluid;

import net.minecraft.core.BlockPos;

/**
 * One fluid store of a warehouse: the tank of a fluid station, named by the
 * station's lower half and the tank {@link #STATION}, or a Fluid Rack
 * shelf, named by its port bay and the tank {@link #RACK}.
 */
public record FluidStoreRef(BlockPos pos, int tank) {

    /** The tank index that marks a Fluid Rack shelf. */
    public static final int RACK = -1;
    /** The tank index that marks a fluid station's tank. */
    public static final int STATION = 0;

    public static FluidStoreRef rack(BlockPos port) {
        return new FluidStoreRef(port, RACK);
    }

    public static FluidStoreRef station(BlockPos lower) {
        return new FluidStoreRef(lower, STATION);
    }

    public boolean isRack() {
        return tank == RACK;
    }
}
