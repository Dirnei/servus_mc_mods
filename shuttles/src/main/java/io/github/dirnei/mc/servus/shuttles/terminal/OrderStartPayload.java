package io.github.dirnei.mc.servus.shuttles.terminal;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: order this many results of the order grid at the open terminal.
 *
 * @param save whether the grid is also saved as a pattern of the line
 */
public record OrderStartPayload(int amount, boolean save) implements CustomPacketPayload {

    public static final Type<OrderStartPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "order_start"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OrderStartPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OrderStartPayload::amount,
            ByteBufCodecs.BOOL, OrderStartPayload::save,
            OrderStartPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
