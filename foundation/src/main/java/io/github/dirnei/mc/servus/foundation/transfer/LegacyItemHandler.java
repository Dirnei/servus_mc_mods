package io.github.dirnei.mc.servus.foundation.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Base of item handlers written with slot style {@code insertItem} and
 * {@code extractItem} and a simulate flag, exposed through the transfer API. Inside a transaction the handler only
 * simulates; the real insert or extract runs when the root transaction
 * commits (see {@link DeferredOps}). Use it for views whose effect cannot be
 * rolled back, such as filtered side views or network entry points.
 */
public abstract class LegacyItemHandler implements ResourceHandler<ItemResource> {

    private record Op(boolean insert, int slot, ItemResource resource, int amount) {
    }

    private final DeferredOps<Op> ops = new DeferredOps<>(op -> {
        if (op.insert()) {
            insertItem(op.slot(), op.resource().toStack(op.amount()), false);
        } else {
            extractItem(op.slot(), op.amount(), false);
        }
    });

    public abstract int getSlots();

    /** Items of the resource this handler accepted in open transactions and inserts when they commit, over all slots. */
    protected int pendingInserts(ItemResource resource) {
        return ops.sum(op -> op.insert() && op.resource().equals(resource), Op::amount);
    }

    public abstract ItemStack getStackInSlot(int slot);

    /** Inserts into a slot; returns what did not fit. */
    public abstract ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    /** Takes up to {@code amount} out of a slot. */
    public abstract ItemStack extractItem(int slot, int amount, boolean simulate);

    public abstract int getSlotLimit(int slot);

    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public int size() {
        return getSlots();
    }

    @Override
    public ItemResource getResource(int index) {
        return ItemResource.of(getStackInSlot(index));
    }

    @Override
    public long getAmountAsLong(int index) {
        return getStackInSlot(index).getCount();
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        int limit = getSlotLimit(index);
        return resource.isEmpty() ? limit : Math.min(limit, resource.getMaxStackSize());
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return resource.isEmpty() || isItemValid(index, resource.toStack());
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0) {
            return 0;
        }
        int pending = ops.sum(op -> op.insert() && op.slot() == index && op.resource().equals(resource), Op::amount);
        int total = DeferredOps.cappedSum(pending, amount);
        ItemStack remainder = insertItem(index, resource.toStack(total), true);
        int accepted = Math.max(0, Math.min(amount, total - remainder.getCount() - pending));
        if (accepted > 0) {
            ops.add(new Op(true, index, resource, accepted), transaction);
        }
        return accepted;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0 || !resource.matches(getStackInSlot(index))) {
            return 0;
        }
        int pending = ops.sum(op -> !op.insert() && op.slot() == index, Op::amount);
        ItemStack available = extractItem(index, DeferredOps.cappedSum(pending, amount), true);
        if (!resource.matches(available)) {
            return 0;
        }
        int taken = Math.max(0, Math.min(amount, available.getCount() - pending));
        if (taken > 0) {
            ops.add(new Op(false, index, resource, taken), transaction);
        }
        return taken;
    }
}
