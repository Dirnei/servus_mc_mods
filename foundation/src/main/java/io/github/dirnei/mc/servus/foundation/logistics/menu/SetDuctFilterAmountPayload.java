package io.github.dirnei.mc.servus.foundation.logistics.menu;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: sets the amount of one filter slot of the open endpoint GUI; 0 removes it.
 *
 * @param line the line ordinal, the Item Pipe or the Fluid Pipe
 */
public record SetDuctFilterAmountPayload(int line, int slot, int amount) implements CustomPacketPayload {

    public static final Type<SetDuctFilterAmountPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "set_duct_filter_amount"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetDuctFilterAmountPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetDuctFilterAmountPayload::line,
            ByteBufCodecs.VAR_INT, SetDuctFilterAmountPayload::slot,
            ByteBufCodecs.VAR_INT, SetDuctFilterAmountPayload::amount,
            SetDuctFilterAmountPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
