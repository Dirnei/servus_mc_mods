package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Client to server: craft this many of the item for the open terminal. */
public record CraftStartPayload(ItemStack item, int amount) implements CustomPacketPayload {

    public static final Type<CraftStartPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "craft_start"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftStartPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, CraftStartPayload::item,
            ByteBufCodecs.VAR_INT, CraftStartPayload::amount,
            CraftStartPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
