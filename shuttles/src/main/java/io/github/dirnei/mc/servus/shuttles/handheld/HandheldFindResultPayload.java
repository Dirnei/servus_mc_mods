package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Server to client: the answer to a find. The client makes the shelves glow
 * for {@code glowTicks} and shows item, total and codes on the panel.
 *
 * @param item      one of the item searched for
 * @param total     how many the bound warehouse holds
 * @param shelves   the loaded shelves holding it, each by one of its bays
 * @param codes     their location codes, in the same order
 * @param glowTicks how long the shelves glow
 */
public record HandheldFindResultPayload(ItemStack item, long total, List<BlockPos> shelves, List<String> codes, int glowTicks)
        implements CustomPacketPayload {

    public static final Type<HandheldFindResultPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "handheld_find_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HandheldFindResultPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, HandheldFindResultPayload::item,
            ByteBufCodecs.VAR_LONG, HandheldFindResultPayload::total,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), HandheldFindResultPayload::shelves,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), HandheldFindResultPayload::codes,
            ByteBufCodecs.VAR_INT, HandheldFindResultPayload::glowTicks,
            HandheldFindResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
