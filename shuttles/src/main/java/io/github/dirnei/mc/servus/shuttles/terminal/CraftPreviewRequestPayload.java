package io.github.dirnei.mc.servus.shuttles.terminal;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Client to server: preview crafting this many of the item at the open terminal. */
public record CraftPreviewRequestPayload(ItemStack item, int amount) implements CustomPacketPayload {

    public static final Type<CraftPreviewRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "craft_preview_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftPreviewRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, CraftPreviewRequestPayload::item,
            ByteBufCodecs.VAR_INT, CraftPreviewRequestPayload::amount,
            CraftPreviewRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
