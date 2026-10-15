package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Server to client: what crafting {@code amount} of {@code item} would do.
 *
 * @param fromStock stock the order would reserve
 * @param toCraft   what its steps would make
 * @param missing   raw material it cannot get
 * @param refusal   {@code no_cpu}, {@code no_producer} or {@code offline}, else empty
 */
public record CraftPreviewPayload(ItemStack item, int amount, List<StockEntry> fromStock, List<StockEntry> toCraft,
                                  List<StockEntry> missing, String refusal) implements CustomPacketPayload {

    public static final Type<CraftPreviewPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "craft_preview"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftPreviewPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, CraftPreviewPayload::item,
            ByteBufCodecs.VAR_INT, CraftPreviewPayload::amount,
            StockEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), CraftPreviewPayload::fromStock,
            StockEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), CraftPreviewPayload::toCraft,
            StockEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), CraftPreviewPayload::missing,
            ByteBufCodecs.STRING_UTF8, CraftPreviewPayload::refusal,
            CraftPreviewPayload::new);

    /** Whether Start may be pressed for this preview. */
    public boolean startable() {
        return refusal.isEmpty() && missing.isEmpty() && !toCraft.isEmpty();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
