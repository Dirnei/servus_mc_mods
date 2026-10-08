package io.github.dirnei.mc.servus.storage.production;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.CpuRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.JobRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.MachineRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.PatternsView;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what the open Production Terminal shows.
 *
 * @param terminal the terminal the content belongs to
 * @param online   whether a CPU or a warehouse is on the line
 * @param reason   why not, when offline: no_line or nothing
 * @param cpus     every formed CPU of the line, for the header
 * @param jobs     every job of those CPUs, open first, then oldest first
 * @param machines every producer and upkeep member of the line, problems first
 * @param patterns the patterns of the line and their capacity
 */
public record ProductionPayload(BlockPos terminal, boolean online, String reason, List<CpuRow> cpus, List<JobRow> jobs, List<MachineRow> machines,
                                PatternsView patterns)
        implements CustomPacketPayload {

    public static final Type<ProductionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "production"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProductionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                BlockPos.STREAM_CODEC.encode(buf, payload.terminal());
                ByteBufCodecs.BOOL.encode(buf, payload.online());
                ByteBufCodecs.STRING_UTF8.encode(buf, payload.reason());
                CpuRow.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, payload.cpus());
                JobRow.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, payload.jobs());
                MachineRow.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, payload.machines());
                PatternsView.STREAM_CODEC.encode(buf, payload.patterns());
            },
            buf -> new ProductionPayload(BlockPos.STREAM_CODEC.decode(buf), ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf), CpuRow.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                    JobRow.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                    MachineRow.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf), PatternsView.STREAM_CODEC.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
