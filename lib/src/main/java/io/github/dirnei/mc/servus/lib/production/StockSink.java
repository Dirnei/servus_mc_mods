package io.github.dirnei.mc.servus.lib.production;

import net.minecraft.world.item.ItemStack;

/** A block that can accept items. */
public interface StockSink {

    /** Hands the stack in; returns what did not fit. */
    ItemStack insert(ItemStack stack);
}
