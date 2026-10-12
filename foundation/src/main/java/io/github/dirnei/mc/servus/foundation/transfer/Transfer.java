package io.github.dirnei.mc.servus.foundation.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * Old style one shot operations (simulate flag, stacks in and out) on any
 * transfer API handler, for code that moves things between handlers from a
 * block entity tick or from a legacy style handler. Every call opens its own
 * transaction, nested in the one open on this thread if there is one.
 */
public final class Transfer {

    private Transfer() {
    }

    /**
     * The transaction open on this thread, or null. Simulations run by the
     * legacy style handlers happen inside the caller's transaction, so the
     * helpers below nest into it instead of opening a root they may not open.
     */
    @SuppressWarnings("deprecation")
    public static @Nullable TransactionContext current() {
        return Transaction.getCurrentOpenedTransaction();
    }

    /**
     * Opens a transaction nested in the current one, or a root transaction when
     * none is open. A committed nested transaction only takes effect when the
     * outer one commits; an uncommitted one (a simulation) is always undone.
     */
    public static Transaction open() {
        return Transaction.open(current());
    }

    // Items

    /** A copy of the stack in a slot. */
    public static ItemStack stackInSlot(ResourceHandler<ItemResource> handler, int slot) {
        return ItemUtil.getStack(handler, slot);
    }

    /** Inserts into one slot; returns what did not fit. */
    public static ItemStack insertItem(ResourceHandler<ItemResource> handler, int slot, ItemStack stack, boolean simulate) {
        return ItemUtil.insertItemReturnRemaining(handler, slot, stack, simulate, Transfer.current());
    }

    /** Inserts into the handler slot by slot from the first; returns what did not fit. */
    public static ItemStack insertItem(ResourceHandler<ItemResource> handler, ItemStack stack, boolean simulate) {
        return ItemUtil.insertItemReturnRemaining(handler, stack, simulate, Transfer.current());
    }

    /** Inserts into slots that already hold the item first, then into empty ones; returns what did not fit. */
    public static ItemStack insertItemStacked(ResourceHandler<ItemResource> handler, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        try (Transaction tx = Transfer.open()) {
            int inserted = ResourceHandlerUtil.insertStacking(handler, ItemResource.of(stack), stack.getCount(), tx);
            if (!simulate) {
                tx.commit();
            }
            int left = stack.getCount() - inserted;
            return left <= 0 ? ItemStack.EMPTY : stack.copyWithCount(left);
        }
    }

    /** Takes up to {@code amount} of whatever is in a slot. */
    public static ItemStack extractItem(ResourceHandler<ItemResource> handler, int slot, int amount, boolean simulate) {
        ItemResource resource = handler.getResource(slot);
        if (resource.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        try (Transaction tx = Transfer.open()) {
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
        try (Transaction tx = Transfer.open()) {
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
        try (Transaction tx = Transfer.open()) {
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
        try (Transaction tx = Transfer.open()) {
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
        try (Transaction tx = Transfer.open()) {
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
        try (Transaction tx = Transfer.open()) {
            int extracted = handler.extract(amount, tx);
            if (!simulate) {
                tx.commit();
            }
            return extracted;
        }
    }

    /**
     * Fills a fluid container (a bucket) from a handler, what
     * {@code FluidUtil.tryFillContainer} did: the filled container, or empty
     * when nothing moved.
     */
    public static ItemStack fillContainer(ItemStack container, ResourceHandler<FluidResource> source) {
        return moveWithContainer(container, source, true);
    }

    /** Empties a fluid container into a handler: the emptied container, or empty when nothing moved. */
    public static ItemStack emptyContainer(ItemStack container, ResourceHandler<FluidResource> destination) {
        return moveWithContainer(container, destination, false);
    }

    private static ItemStack moveWithContainer(ItemStack container, ResourceHandler<FluidResource> handler, boolean fill) {
        ItemSlots slot = new ItemSlots(1);
        slot.setStackInSlot(0, container.copyWithCount(1));
        ResourceHandler<FluidResource> item = ItemAccess.forHandlerIndex(slot, 0).getCapability(Capabilities.Fluid.ITEM);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        boolean moved = fill
                ? ResourceHandlerUtil.moveFirst(handler, item, resource -> true, Integer.MAX_VALUE, current()) != null
                : ResourceHandlerUtil.moveFirst(item, handler, resource -> true, Integer.MAX_VALUE, current()) != null;
        return moved ? slot.getStackInSlot(0).copy() : ItemStack.EMPTY;
    }

}
