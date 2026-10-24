package io.github.dirnei.mc.servus.shuttles.terminal;

import java.util.List;
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
 * @param slots     the output slots the request owns, empty without one
 */
public record RequestRow(int id, ItemStack stack, int requested, int delivered, int missing, String state, List<Integer> slots) {

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestRow> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RequestRow::id,
            ItemStack.STREAM_CODEC, RequestRow::stack,
            ByteBufCodecs.VAR_INT, RequestRow::requested,
            ByteBufCodecs.VAR_INT, RequestRow::delivered,
            ByteBufCodecs.VAR_INT, RequestRow::missing,
            ByteBufCodecs.STRING_UTF8, RequestRow::state,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), RequestRow::slots,
            RequestRow::new);

    /** Whether the request owns that output slot. */
    public boolean owns(int slot) {
        return slots.contains(slot);
    }
}
