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
 * Server to client: what one bay holds, for the INFO panel.
 *
 * @param pos    the bay
 * @param zoneId its zone id, 0 without one
 * @param used   slots in use; mB for a Fluid Rack
 * @param total  all slots; the section's capacity in mB for a Fluid Rack
 * @param top    the most held items, one each
 * @param counts how many of each it holds; mB for a Fluid Rack
 * @param fluid  whether the bay is a Fluid Rack bay
 */
public record HandheldInfoPayload(BlockPos pos, int zoneId, long used, long total, List<ItemStack> top, List<Long> counts, boolean fluid)
        implements CustomPacketPayload {

    public static final Type<HandheldInfoPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "handheld_info"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HandheldInfoPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, HandheldInfoPayload::pos,
            ByteBufCodecs.VAR_INT, HandheldInfoPayload::zoneId,
            ByteBufCodecs.VAR_LONG, HandheldInfoPayload::used,
            ByteBufCodecs.VAR_LONG, HandheldInfoPayload::total,
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), HandheldInfoPayload::top,
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), HandheldInfoPayload::counts,
            ByteBufCodecs.BOOL, HandheldInfoPayload::fluid,
            HandheldInfoPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
