package io.github.dirnei.mc.servus.foundation.production;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * A block that sends items to a data member on request, by its own means (a
 * warehouse ships them as packets). Exposed as {@code servus_foundation:supplier}.
 */
public interface ItemSupplier {

    /**
     * Asks for up to {@code amount} items like the stack to be sent to the data member at {@code target}.
     *
     * @return how many will be sent, zero when none can
     */
    int supply(BlockPos target, ItemStack like, int amount);
}
