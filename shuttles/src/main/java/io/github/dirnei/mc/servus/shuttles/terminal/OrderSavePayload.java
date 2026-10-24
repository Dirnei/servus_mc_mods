package io.github.dirnei.mc.servus.shuttles.terminal;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: save the order grid of the open terminal as a pattern of its line, without ordering. */
public record OrderSavePayload() implements CustomPacketPayload {

    public static final OrderSavePayload INSTANCE = new OrderSavePayload();

    public static final Type<OrderSavePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "order_save"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OrderSavePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
