package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Client to server: request this many of the item at the open terminal. */
public record RequestItemPayload(ItemStack item, int amount) implements CustomPacketPayload {

    public static final Type<RequestItemPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "request_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestItemPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, RequestItemPayload::item,
            ByteBufCodecs.VAR_INT, RequestItemPayload::amount,
            RequestItemPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
