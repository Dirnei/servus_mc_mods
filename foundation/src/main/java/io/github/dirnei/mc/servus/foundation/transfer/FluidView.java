package io.github.dirnei.mc.servus.foundation.transfer;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

/**
 * View of any fluid handler (usually one read from a capability) with
 * simulate flag methods. Every change opens
 * its own transaction, nested in the one open on this thread if there is one.
 *
 * @param handler the wrapped handler
 */
public record FluidView(ResourceHandler<FluidResource> handler) {

    /** A view of {@code handler}, or null when there is no handler. */
    public static @Nullable FluidView of(@Nullable ResourceHandler<FluidResource> handler) {
        return handler == null ? null : new FluidView(handler);
    }

    public int getTanks() {
        return handler.size();
    }

    /** A copy of the fluid in the tank. */
    public FluidStack getFluidInTank(int tank) {
        return Transfer.fluidInTank(handler, tank);
    }

    public int getTankCapacity(int tank) {
        return handler.getCapacityAsInt(tank, handler.getResource(tank));
    }

    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.isEmpty() || handler.isValid(tank, FluidResource.of(stack));
    }

    public int fill(FluidStack stack, boolean simulate) {
        return Transfer.fill(handler, stack, simulate);
    }

    public FluidStack drain(FluidStack stack, boolean simulate) {
        return Transfer.drain(handler, stack, simulate);
    }

    public FluidStack drain(int maxDrain, boolean simulate) {
        return Transfer.drain(handler, maxDrain, simulate);
    }
}
