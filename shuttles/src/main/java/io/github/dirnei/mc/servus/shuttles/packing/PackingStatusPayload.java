package io.github.dirnei.mc.servus.shuttles.packing;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: the status line of the open Packing Station.
 *
 * @param line  the current delivery or why the station waits
 * @param alert whether the line explains a problem (drawn red)
 * @param rate  items per second the station converts in each direction, upgrades included
 */
public record PackingStatusPayload(Component line, boolean alert, float rate) implements CustomPacketPayload {

    public static final Type<PackingStatusPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "packing_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PackingStatusPayload> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, PackingStatusPayload::line,
            ByteBufCodecs.BOOL, PackingStatusPayload::alert,
            ByteBufCodecs.FLOAT, PackingStatusPayload::rate,
            PackingStatusPayload::new);

    public static final PackingStatusPayload EMPTY = new PackingStatusPayload(Component.empty(), false, 0);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
