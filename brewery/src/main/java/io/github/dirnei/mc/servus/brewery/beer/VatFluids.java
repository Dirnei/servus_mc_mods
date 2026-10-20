package io.github.dirnei.mc.servus.brewery.beer;

import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyFluidHandler;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A Fermenting Vat's tank as automation and buckets see it: wort goes in only while no batch runs, and only what is
 * not a wort (the finished beer) comes out.
 */
public final class VatFluids extends LegacyFluidHandler {

    private final FluidSlot tank;
    private final BooleanSupplier fermenting;
    private final Predicate<FluidStack> isWort;

    /**
     * @param fermenting whether a batch runs
     * @param isWort     whether a fluid is something the vat ferments
     */
    public VatFluids(FluidSlot tank, BooleanSupplier fermenting, Predicate<FluidStack> isWort) {
        this.tank = tank;
        this.fermenting = fermenting;
        this.isWort = isWort;
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
        return !fermenting.getAsBoolean() && tank.isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, boolean simulate) {
        return fermenting.getAsBoolean() ? 0 : tank.fill(resource, simulate);
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean simulate) {
        return holdsWort() ? FluidStack.EMPTY : tank.drain(resource, simulate);
    }

    @Override
    public FluidStack drain(int maxDrain, boolean simulate) {
        return holdsWort() ? FluidStack.EMPTY : tank.drain(maxDrain, simulate);
    }

    private boolean holdsWort() {
        return !tank.isEmpty() && isWort.test(tank.getFluid());
    }
}
