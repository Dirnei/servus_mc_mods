package io.github.dirnei.mc.servus.storage.terminal;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/** One line of the stock view: a one item stack as display and the total. */
public record StockEntry(ItemStack stack, long count) {

    public static final StreamCodec<RegistryFriendlyByteBuf, StockEntry> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, StockEntry::stack,
            ByteBufCodecs.VAR_LONG, StockEntry::count,
            StockEntry::new);
}
