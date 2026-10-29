package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Client to server, from the handheld GUI's FIND list: light up every bay of the bound warehouse holding this item. */
public record HandheldFindPayload(ItemStack item) implements CustomPacketPayload {

    public static final Type<HandheldFindPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "handheld_find"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HandheldFindPayload> STREAM_CODEC =
            ItemStack.STREAM_CODEC.map(HandheldFindPayload::new, HandheldFindPayload::item);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
