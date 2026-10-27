package io.github.dirnei.mc.servus.shuttles.fluidstation;

import io.github.dirnei.mc.servus.foundation.side.FluidSideRules;
import io.github.dirnei.mc.servus.foundation.side.FluidSides;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyFluidHandler;
import java.util.List;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * All tanks of a fluid station for buckets by hand: a fill goes in by the
 * fill order of a side on All, whatever the station's sides are set to, and
 * a drain takes from the first tank that holds the asked amount of the fluid.
 */
final class FluidStationHandler extends LegacyFluidHandler {

    private final FluidStationBlockEntity station;
    private final FluidSides all;

    FluidStationHandler(FluidStationBlockEntity station) {
        this.station = station;
        this.all = new FluidSides(station.tankCount(), List.of(SideMode.BOTH), SideMode.BOTH);
    }

    @Override
    public int getTanks() {
        return station.tankCount();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return station.tank(tank).getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return station.tank(tank).getCapacity();
    }

    /** The handler picks the tank itself, so a move through any tank index fills or drains the station. */
    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return insert(resource, amount, transaction);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return extract(resource, amount, transaction);
    }

    @Override
    public int fill(FluidStack resource, boolean simulate) {
        if (resource.isEmpty()) {
            return 0;
        }
        int filled = 0;
        for (int tank : FluidSideRules.fillOrder(all, RelativeSide.BACK, station.sideTankStates(), FluidResource.of(resource),
                station::sideTankAccepts)) {
            filled += station.tank(tank).fill(resource.copyWithAmount(resource.getAmount() - filled), simulate);
            if (filled >= resource.getAmount()) {
                break;
            }
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean simulate) {
        FluidSlot tank = firstHolding(resource);
        return tank == null ? FluidStack.EMPTY : tank.drain(resource, simulate);
    }

    @Override
    public FluidStack drain(int maxDrain, boolean simulate) {
        for (FluidSlot tank : station.tanks()) {
            if (tank.getFluidAmount() >= maxDrain) {
                return tank.drain(maxDrain, simulate);
            }
        }
        return FluidStack.EMPTY;
    }

    /** The first tank holding at least the asked amount of the fluid, else the first holding any of it. */
    private FluidSlot firstHolding(FluidStack resource) {
        FluidSlot partial = null;
        for (FluidSlot tank : station.tanks()) {
            if (!FluidStack.isSameFluidSameComponents(tank.getFluid(), resource)) {
                continue;
            }
            if (tank.getFluidAmount() >= resource.getAmount()) {
                return tank;
            }
            partial = partial == null ? tank : partial;
        }
        return partial;
    }
}
