package io.github.dirnei.mc.servus.logistics.duct;

import io.github.dirnei.mc.servus.logistics.network.ItemChannelNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The item handler a utility duct shows to one neighbour. One virtual slot that
 * never holds anything: inserted items are routed to another inventory on the
 * item channel network right away, never back to the inserting neighbour.
 */
public final class DuctItemHandler implements IItemHandler {

    private final UtilityDuctBlockEntity duct;
    private final Direction side;

    DuctItemHandler(UtilityDuctBlockEntity duct, Direction side) {
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
        if (stack.isEmpty() || !duct.mode(Channel.ITEM, side).allowsInsert()) {
            return stack;
        }
        BlockPos origin = duct.getBlockPos().relative(side);
        return duct.<ItemChannelNetwork>network(Channel.ITEM)
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
