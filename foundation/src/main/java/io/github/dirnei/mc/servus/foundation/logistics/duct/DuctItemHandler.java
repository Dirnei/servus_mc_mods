package io.github.dirnei.mc.servus.foundation.logistics.duct;

import io.github.dirnei.mc.servus.foundation.logistics.network.ItemChannelNetwork;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/**
 * The item handler a utility duct shows to one neighbour. One virtual slot that
 * never holds anything: inserted items are routed right away to another
 * inventory on the inserting side's channel of the Item Pipe network, never
 * back to the inserting neighbour.
 */
public final class DuctItemHandler extends LegacyItemHandler {

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
        if (stack.isEmpty() || !duct.mode(Line.ITEM, side).in() || !duct.isActive(Line.ITEM, side)) {
            return stack;
        }
        BlockPos origin = duct.getBlockPos().relative(side);
        int channel = duct.channel(Line.ITEM, side);
        return duct.<ItemChannelNetwork>network(Line.ITEM)
                .map(network -> network.route(stack, origin, channel, simulate))
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
}
