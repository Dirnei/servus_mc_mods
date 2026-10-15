package io.github.dirnei.mc.servus.foundation.logistics.menu;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: sets one filter slot of the open endpoint GUI to an item by id; empty clears the slot. */
public record SetDuctFilterPayload(int slot, Optional<Identifier> item) implements CustomPacketPayload {

    public static final Type<SetDuctFilterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "set_duct_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetDuctFilterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetDuctFilterPayload::slot,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), SetDuctFilterPayload::item,
            SetDuctFilterPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
