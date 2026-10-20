package io.github.dirnei.mc.servus.brewery.brewhouse;

import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The Brew Kettle's fluids as automation sees them: filling goes into the water tank, draining comes from the wort tank.
 * The wort is reported as tank 0, the one {@link LegacyFluidHandler} drains and fills through, so pipes that drain by
 * index find it; every tank accepts what the water tank takes.
 */
public final class KettleFluids extends LegacyFluidHandler {

    private final FluidSlot water;
    private final FluidSlot wort;

    public KettleFluids(FluidSlot water, FluidSlot wort) {
        this.water = water;
        this.wort = wort;
    }

    @Override
    public int getTanks() {
        return 2;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank == 0 ? wort.getFluid() : water.getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank == 0 ? wort.getCapacity() : water.getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return water.isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, boolean simulate) {
        return water.isFluidValid(resource) ? water.fill(resource, simulate) : 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean simulate) {
        return wort.drain(resource, simulate);
    }

    @Override
    public FluidStack drain(int maxDrain, boolean simulate) {
        return wort.drain(maxDrain, simulate);
    }
}
