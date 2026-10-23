package io.github.dirnei.mc.servus.shuttles.terminal;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Client to server: preview this many of the item at the open terminal.
 *
 * @param item   the item; ignored for {@link Mode#ORDER}, which reads the order grid
 * @param amount how many items
 * @param mode   what the preview is for
 */
public record CraftPreviewRequestPayload(ItemStack item, int amount, Mode mode) implements CustomPacketPayload {

    /** What a preview is for. */
    public enum Mode {
        /** The craft dialog: everything is crafted. */
        CRAFT,
        /** The request prompt: requestable stock is shipped, the rest crafted. */
        REQUEST,
        /** The order grid: its recipe and items are crafted. */
        ORDER
    }

    public static final Type<CraftPreviewRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "craft_preview_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftPreviewRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, CraftPreviewRequestPayload::item,
            ByteBufCodecs.VAR_INT, CraftPreviewRequestPayload::amount,
            ByteBufCodecs.idMapper(index -> Mode.values()[index], Mode::ordinal), CraftPreviewRequestPayload::mode,
            CraftPreviewRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
