package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Client to server: put up to a stack of the item from instant stock onto the empty cursor (Alt click). */
public record GridPullPayload(ItemStack item) implements CustomPacketPayload {

    public static final Type<GridPullPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "grid_pull"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GridPullPayload> STREAM_CODEC =
            ItemStack.STREAM_CODEC.map(GridPullPayload::new, GridPullPayload::item);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
