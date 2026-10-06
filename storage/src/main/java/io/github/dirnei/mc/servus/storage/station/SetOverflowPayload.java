package io.github.dirnei.mc.servus.storage.station;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: make the selected zone the overflow zone, or stop it being one. */
public record SetOverflowPayload(boolean overflow) implements CustomPacketPayload {

    public static final Type<SetOverflowPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "set_overflow"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetOverflowPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SetOverflowPayload::overflow,
            SetOverflowPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
