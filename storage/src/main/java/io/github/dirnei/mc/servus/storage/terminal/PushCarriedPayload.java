package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: store the carried stack in the warehouse. */
public record PushCarriedPayload() implements CustomPacketPayload {

    public static final PushCarriedPayload INSTANCE = new PushCarriedPayload();

    public static final Type<PushCarriedPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "push_carried"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PushCarriedPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
