package io.github.dirnei.mc.servus.manufacturing.network;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: the research state of the receiving player's team. */
public record ResearchSyncPayload(List<ResourceLocation> completed, Optional<ResourceLocation> current,
                                  Map<String, Integer> progressOfCurrent) implements CustomPacketPayload {

    public static final Type<ResearchSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "research_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), ResearchSyncPayload::completed,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), ResearchSyncPayload::current,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), ResearchSyncPayload::progressOfCurrent,
            ResearchSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
