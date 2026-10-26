package io.github.dirnei.mc.servus.foundation.logistics.menu;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: sets one fluid filter slot of the open endpoint GUI to a fluid by id; empty clears the slot. */
public record SetDuctFluidFilterPayload(int slot, Optional<Identifier> fluid) implements CustomPacketPayload {

    public static final Type<SetDuctFluidFilterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "set_duct_fluid_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetDuctFluidFilterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetDuctFluidFilterPayload::slot,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), SetDuctFluidFilterPayload::fluid,
            SetDuctFluidFilterPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
