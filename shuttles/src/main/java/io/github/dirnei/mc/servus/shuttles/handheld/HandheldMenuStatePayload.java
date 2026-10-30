package io.github.dirnei.mc.servus.shuttles.handheld;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Server to client, for the open handheld GUI: how many loaded shelves each
 * zone has, and the STOCK list: the stock of the bound warehouse, largest first.
 *
 * @param shelfCounts loaded shelves per zone id
 * @param bound       whether the handheld is bound to a warehouse that still has its controller
 * @param items       one of each stored item
 * @param counts      how many of each the warehouse holds
 */
public record HandheldMenuStatePayload(Map<Integer, Integer> shelfCounts, boolean bound, List<ItemStack> items,
                                       List<Long> counts) implements CustomPacketPayload {

    public static final Type<HandheldMenuStatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "handheld_menu_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HandheldMenuStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, ByteBufCodecs.VAR_INT), HandheldMenuStatePayload::shelfCounts,
            ByteBufCodecs.BOOL, HandheldMenuStatePayload::bound,
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), HandheldMenuStatePayload::items,
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), HandheldMenuStatePayload::counts,
            HandheldMenuStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
