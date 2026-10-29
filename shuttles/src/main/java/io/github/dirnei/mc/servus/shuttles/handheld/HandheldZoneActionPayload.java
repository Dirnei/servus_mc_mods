package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server, from the handheld GUI's zone list: choose a zone as the active one, or delete it. */
public record HandheldZoneActionPayload(boolean delete, int id) implements CustomPacketPayload {

    public static final Type<HandheldZoneActionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "handheld_zone_action"));

    public static final StreamCodec<ByteBuf, HandheldZoneActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, HandheldZoneActionPayload::delete,
            ByteBufCodecs.VAR_INT, HandheldZoneActionPayload::id,
            HandheldZoneActionPayload::new);

    public static HandheldZoneActionPayload pick(int id) {
        return new HandheldZoneActionPayload(false, id);
    }

    public static HandheldZoneActionPayload remove(int id) {
        return new HandheldZoneActionPayload(true, id);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
