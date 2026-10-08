package io.github.dirnei.mc.servus.foundation.logistics.duct;

import io.github.dirnei.mc.servus.foundation.logistics.network.FluidChannelNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The fluid handler a utility duct shows to one neighbour. One tank of
 * capacity zero that never holds anything: filled fluid goes straight on to
 * the blocks on the filling side's channel of the Fluid Pipe network, never
 * back to the filling neighbour. Nothing can be drained.
 */
public final class DuctFluidHandler implements IFluidHandler {

    private final UtilityDuctBlockEntity duct;
    private final Direction side;

    DuctFluidHandler(UtilityDuctBlockEntity duct, Direction side) {
        this.duct = duct;
        this.side = side;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !duct.has(Line.FLUID) || !duct.mode(Line.FLUID, side).allowsInsert()) {
            return 0;
        }
        BlockPos origin = duct.getBlockPos().relative(side);
        int channel = duct.channel(Line.FLUID, side);
        return duct.<FluidChannelNetwork>network(Line.FLUID)
                .map(network -> network.fill(resource, origin, channel, action))
                .orElse(0);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }
}
