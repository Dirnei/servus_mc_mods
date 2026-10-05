package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Client to server: pull one stack of this item to the cursor or the inventory. */
public record PullItemPayload(ItemStack item, boolean toInventory) implements CustomPacketPayload {

    public static final Type<PullItemPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "pull_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PullItemPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, PullItemPayload::item,
            ByteBufCodecs.BOOL, PullItemPayload::toInventory,
            PullItemPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
