package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: move the crafting grid contents into the warehouse. */
public record GridClearPayload() implements CustomPacketPayload {

    public static final GridClearPayload INSTANCE = new GridClearPayload();

    public static final Type<GridClearPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "grid_clear"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GridClearPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
