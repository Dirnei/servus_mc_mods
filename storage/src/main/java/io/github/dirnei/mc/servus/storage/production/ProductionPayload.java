package io.github.dirnei.mc.servus.storage.production;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.CpuRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.MachineRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.PatternsView;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what the open Production Terminal shows.
 *
 * @param online   whether a CPU or a warehouse is on the line
 * @param reason   why not, when offline: no_line or nothing
 * @param cpus     every formed CPU of the line with its orders
 * @param machines every producer of the line
 * @param patterns the patterns of the line and their capacity
 */
public record ProductionPayload(boolean online, String reason, List<CpuRow> cpus, List<MachineRow> machines,
                                PatternsView patterns)
        implements CustomPacketPayload {

    public static final Type<ProductionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "production"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProductionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ProductionPayload::online,
            ByteBufCodecs.STRING_UTF8, ProductionPayload::reason,
            CpuRow.STREAM_CODEC.apply(ByteBufCodecs.list()), ProductionPayload::cpus,
            MachineRow.STREAM_CODEC.apply(ByteBufCodecs.list()), ProductionPayload::machines,
            PatternsView.STREAM_CODEC, ProductionPayload::patterns,
            ProductionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
