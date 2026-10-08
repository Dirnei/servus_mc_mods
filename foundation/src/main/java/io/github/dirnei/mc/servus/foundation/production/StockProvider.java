package io.github.dirnei.mc.servus.foundation.production;

import net.minecraft.world.item.ItemStack;

/** A block that can supply items: how many of something it has and taking some of it. */
public interface StockProvider {

    /** How many items like the stack (same item and components) are available. */
    long count(ItemStack like);

    /** Takes up to {@code amount} items like the stack; the returned stack may be smaller or empty. */
    ItemStack extract(ItemStack like, int amount);
}
