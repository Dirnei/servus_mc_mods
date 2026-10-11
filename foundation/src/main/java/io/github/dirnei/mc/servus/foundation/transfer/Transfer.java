package io.github.dirnei.mc.servus.foundation.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Old style one shot operations (simulate flag, stacks in and out) on any
 * transfer API handler, for code that moves things between handlers from a
 * block entity tick. Every call opens and closes its own root transaction, so
 * none of them may be called while a transaction is open.
 */
public final class Transfer {

    private Transfer() {
    }

    // Items

    /** A copy of the stack in a slot. */
    public static ItemStack stackInSlot(ResourceHandler<ItemResource> handler, int slot) {
        return ItemUtil.getStack(handler, slot);
    }

    /** Inserts into one slot; returns what did not fit. */
    public static ItemStack insertItem(ResourceHandler<ItemResource> handler, int slot, ItemStack stack, boolean simulate) {
        return ItemUtil.insertItemReturnRemaining(handler, slot, stack, simulate, null);
    }

    /** Inserts into the handler slot by slot from the first; returns what did not fit. */
    public static ItemStack insertItem(ResourceHandler<ItemResource> handler, ItemStack stack, boolean simulate) {
        return ItemUtil.insertItemReturnRemaining(handler, stack, simulate, null);
    }

    /** Takes up to {@code amount} of whatever is in a slot. */
    public static ItemStack extractItem(ResourceHandler<ItemResource> handler, int slot, int amount, boolean simulate) {
        ItemResource resource = handler.getResource(slot);
        if (resource.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = handler.extract(slot, resource, amount, tx);
            if (!simulate) {
                tx.commit();
            }
            return extracted <= 0 ? ItemStack.EMPTY : resource.toStack(extracted);
        }
    }

    /** Takes up to {@code amount} of one item from anywhere in the handler. */
    public static int extractItem(ResourceHandler<ItemResource> handler, ItemResource resource, int amount, boolean simulate) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = handler.extract(resource, amount, tx);
            if (!simulate) {
                tx.commit();
            }
            return extracted;
        }
    }

    // Fluids

    /** A copy of the fluid in a tank. */
    public static FluidStack fluidInTank(ResourceHandler<FluidResource> handler, int tank) {
        FluidResource resource = handler.getResource(tank);
        return resource.isEmpty() ? FluidStack.EMPTY : resource.toStack(handler.getAmountAsInt(tank));
    }

    /** Fills the handler; returns the amount taken. */
    public static int fill(ResourceHandler<FluidResource> handler, FluidStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int filled = handler.insert(FluidResource.of(stack), stack.getAmount(), tx);
            if (!simulate) {
                tx.commit();
            }
            return filled;
        }
    }

    /** Drains the given fluid; returns what came out. */
    public static FluidStack drain(ResourceHandler<FluidResource> handler, FluidStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return FluidStack.EMPTY;
        }
        FluidResource resource = FluidResource.of(stack);
        try (Transaction tx = Transaction.openRoot()) {
            int drained = handler.extract(resource, stack.getAmount(), tx);
            if (!simulate) {
                tx.commit();
            }
            return drained <= 0 ? FluidStack.EMPTY : resource.toStack(drained);
        }
    }

    /** Drains up to {@code maxDrain} of the first fluid found in the handler. */
    public static FluidStack drain(ResourceHandler<FluidResource> handler, int maxDrain, boolean simulate) {
        if (maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        for (int tank = 0; tank < handler.size(); tank++) {
            FluidResource resource = handler.getResource(tank);
            if (resource.isEmpty() || handler.getAmountAsLong(tank) <= 0) {
                continue;
            }
            FluidStack drained = drain(handler, resource.toStack(maxDrain), simulate);
            if (!drained.isEmpty()) {
                return drained;
            }
        }
        return FluidStack.EMPTY;
    }

    // Energy

    /** Gives energy to the handler; returns the amount taken. */
    public static int receiveEnergy(EnergyHandler handler, int amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int inserted = handler.insert(amount, tx);
            if (!simulate) {
                tx.commit();
            }
            return inserted;
        }
    }

    /** Takes energy out of the handler; returns the amount taken. */
    public static int extractEnergy(EnergyHandler handler, int amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = handler.extract(amount, tx);
            if (!simulate) {
                tx.commit();
            }
            return extracted;
        }
    }
}
