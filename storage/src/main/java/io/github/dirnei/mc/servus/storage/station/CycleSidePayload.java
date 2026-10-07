package io.github.dirnei.mc.servus.storage.station;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: cycle the mode of the side with this {@code Direction} ordinal on the open station. */
public record CycleSidePayload(int direction) implements CustomPacketPayload {

    public static final Type<CycleSidePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "cycle_side"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CycleSidePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CycleSidePayload::direction,
            CycleSidePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
