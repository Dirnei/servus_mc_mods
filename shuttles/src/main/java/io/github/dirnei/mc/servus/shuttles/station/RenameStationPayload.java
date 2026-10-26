package io.github.dirnei.mc.servus.shuttles.station;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: the name typed into the open Inbound, Outbound or Packing Station GUI. */
public record RenameStationPayload(String name) implements CustomPacketPayload {

    public static final Type<RenameStationPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "rename_station"));

    public static final StreamCodec<ByteBuf, RenameStationPayload> STREAM_CODEC =
            ByteBufCodecs.stringUtf8(64).map(RenameStationPayload::new, RenameStationPayload::name);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
