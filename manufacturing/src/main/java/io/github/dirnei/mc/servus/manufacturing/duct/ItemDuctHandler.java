package io.github.dirnei.mc.servus.manufacturing.duct;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The item handler a duct shows to one neighbour. It has one virtual slot that
 * never holds anything: inserted items are routed to another inventory on the
 * network right away, never back to the inserting neighbour.
 */
final class ItemDuctHandler implements IItemHandler {

    private final ItemDuctBlockEntity duct;
    private final Direction side;

    ItemDuctHandler(ItemDuctBlockEntity duct, Direction side) {
        this.duct = duct;
        this.side = side;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        BlockPos origin = side == null ? null : duct.getBlockPos().relative(side);
        return duct.network()
                .map(network -> network.route(stack, origin, simulate))
                .orElse(stack);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }
}
