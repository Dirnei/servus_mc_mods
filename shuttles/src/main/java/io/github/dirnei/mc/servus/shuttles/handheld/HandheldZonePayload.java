package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server, from the handheld GUI's zone form: create a zone
 * ({@code id} 0) or edit the zone with that id.
 */
public record HandheldZonePayload(int id, String name, int colour, ZoneFilter filter) implements CustomPacketPayload {

    public static final Type<HandheldZonePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "handheld_zone"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HandheldZonePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HandheldZonePayload::id,
            ByteBufCodecs.stringUtf8(64), HandheldZonePayload::name,
            ByteBufCodecs.VAR_INT, HandheldZonePayload::colour,
            ZoneFilter.STREAM_CODEC, HandheldZonePayload::filter,
            HandheldZonePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
