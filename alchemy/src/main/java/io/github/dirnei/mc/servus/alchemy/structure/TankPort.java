package io.github.dirnei.mc.servus.alchemy.structure;

import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;

/** One tank of a build as a port: it either only fills with the fluid the tank takes or only drains. */
public final class TankPort extends LegacyFluidHandler {

    private final FluidSlot tank;
    private final boolean fills;

    private TankPort(FluidSlot tank, boolean fills) {
        this.tank = tank;
        this.fills = fills;
    }

    /** A port that fills {@code tank} and is never drained from outside. */
    public static TankPort fillOnly(FluidSlot tank) {
        return new TankPort(tank, true);
    }

    /** A port that drains {@code tank} and is never filled from outside. */
    public static TankPort drainOnly(FluidSlot tank) {
        return new TankPort(tank, false);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int index) {
        return tank.getFluid();
    }

    @Override
    public int getTankCapacity(int index) {
        return tank.getCapacity();
    }

    @Override
    public boolean isFluidValid(int index, FluidStack stack) {
        return fills && tank.isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, boolean simulate) {
        return fills && tank.isFluidValid(resource) ? tank.fill(resource, simulate) : 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean simulate) {
        return !fills && tank.isFluidValid(resource) ? tank.drain(resource, simulate) : FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, boolean simulate) {
        return fills ? FluidStack.EMPTY : tank.drain(maxDrain, simulate);
    }
}
