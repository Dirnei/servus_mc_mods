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
 * Server to client: what the shelf of a scanned bay holds, for the INFO panel
 * and screen. Kept until the next INFO scan.
 *
 * @param pos    the scanned bay
 * @param code   the shelf's location code, empty without a controller
 * @param zoneId the shelf's zone id, 0 without one
 * @param used   slots in use over all bays; mB for a Fluid Rack
 * @param total  all slots of the shelf; its capacity in mB for a Fluid Rack
 * @param top    the most held items, one each
 * @param counts how many of each the shelf holds; mB for a Fluid Rack
 * @param fluid  whether the shelf is a Fluid Rack shelf
 */
public record HandheldInfoPayload(BlockPos pos, String code, int zoneId, long used, long total, List<ItemStack> top, List<Long> counts,
                                  boolean fluid) implements CustomPacketPayload {

    public static final Type<HandheldInfoPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "handheld_info"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HandheldInfoPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, HandheldInfoPayload::pos,
            ByteBufCodecs.STRING_UTF8, HandheldInfoPayload::code,
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
