package io.github.dirnei.mc.servus.storage.station;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: show and edit this zone's rule in the open station menu. */
public record SelectZonePayload(String key) implements CustomPacketPayload {

    public static final Type<SelectZonePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "select_zone"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectZonePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SelectZonePayload::key,
            SelectZonePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
