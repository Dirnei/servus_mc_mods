package io.github.dirnei.mc.servus.storage.station;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: set the selected zone's tags from a comma separated line. */
public record SetZoneTagsPayload(String line) implements CustomPacketPayload {

    public static final Type<SetZoneTagsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "set_zone_tags"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetZoneTagsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SetZoneTagsPayload::line,
            SetZoneTagsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
