package io.github.dirnei.mc.servus.foundation.transfer;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Base of fluid handlers written in the style of the old NeoForge fluid
 * handler ({@code fill} and {@code drain} with a simulate flag, no tank index)
 * that are exposed through the transfer API. Inside a transaction the handler
 * only simulates; the real fill or drain runs when the root transaction
 * commits (see {@link DeferredOps}).
 *
 * <p>Fills and drains go through tank index 0 only, because the old handler
 * picked the tank itself; the other indices only report contents.
 */
public abstract class LegacyFluidHandler implements ResourceHandler<FluidResource> {

    private record Op(boolean fill, FluidResource resource, int amount) {
    }

    private final DeferredOps<Op> ops = new DeferredOps<>(op -> {
        if (op.fill()) {
            fill(op.resource().toStack(op.amount()), false);
        } else {
            drain(op.resource().toStack(op.amount()), false);
        }
    });

    public abstract int getTanks();

    public abstract FluidStack getFluidInTank(int tank);

    public abstract int getTankCapacity(int tank);

    public boolean isFluidValid(int tank, FluidStack stack) {
        return true;
    }

    /** Fills the handler; returns the amount taken. */
    public abstract int fill(FluidStack resource, boolean simulate);

    /** Drains the given fluid; returns what came out. */
    public abstract FluidStack drain(FluidStack resource, boolean simulate);

    /** Drains any fluid; returns what came out. */
    public abstract FluidStack drain(int maxDrain, boolean simulate);

    @Override
    public int size() {
        return Math.max(1, getTanks());
    }

    @Override
    public FluidResource getResource(int index) {
        return index < getTanks() ? FluidResource.of(getFluidInTank(index)) : FluidResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        return index < getTanks() ? getFluidInTank(index).getAmount() : 0;
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return index < getTanks() ? getTankCapacity(index) : 0;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return resource.isEmpty() || (index < getTanks() && isFluidValid(index, resource.toStack(1)));
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return index == 0 ? insert(resource, amount, transaction) : 0;
    }

    @Override
    public int insert(FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0) {
            return 0;
        }
        int pending = ops.sum(op -> op.fill() && op.resource().equals(resource), Op::amount);
        int filled = fill(resource.toStack(DeferredOps.add(pending, amount)), true);
        int accepted = Math.max(0, Math.min(amount, filled - pending));
        if (accepted > 0) {
            ops.add(new Op(true, resource, accepted), transaction);
        }
        return accepted;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return index == 0 ? extract(resource, amount, transaction) : 0;
    }

    @Override
    public int extract(FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0) {
            return 0;
        }
        int pending = ops.sum(op -> !op.fill() && op.resource().equals(resource), Op::amount);
        FluidStack drained = drain(resource.toStack(DeferredOps.add(pending, amount)), true);
        if (drained.isEmpty() || !resource.matches(drained)) {
            return 0;
        }
        int taken = Math.max(0, Math.min(amount, drained.getAmount() - pending));
        if (taken > 0) {
            ops.add(new Op(false, resource, taken), transaction);
        }
        return taken;
    }
}
