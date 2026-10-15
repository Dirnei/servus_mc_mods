package io.github.dirnei.mc.servus.shuttles.terminal;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * One open request as the GUI lists it.
 *
 * @param id        the request id, for cancelling
 * @param stack     a one item stack as display
 * @param requested how many were asked for
 * @param delivered how many landed in the output slots
 * @param missing   how many cannot come
 * @param state     the request state name
 */
public record RequestRow(int id, ItemStack stack, int requested, int delivered, int missing, String state) {

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestRow> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RequestRow::id,
            ItemStack.STREAM_CODEC, RequestRow::stack,
            ByteBufCodecs.VAR_INT, RequestRow::requested,
            ByteBufCodecs.VAR_INT, RequestRow::delivered,
            ByteBufCodecs.VAR_INT, RequestRow::missing,
            ByteBufCodecs.STRING_UTF8, RequestRow::state,
            RequestRow::new);
}
