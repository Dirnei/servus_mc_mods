package io.github.dirnei.mc.servus.storage.production;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: cancel the order with this id on the CPU at this position. Order ids are per CPU. */
public record CancelProductionOrderPayload(BlockPos cpu, int id) implements CustomPacketPayload {

    public static final Type<CancelProductionOrderPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "cancel_production_order"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CancelProductionOrderPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CancelProductionOrderPayload::cpu,
            ByteBufCodecs.VAR_INT, CancelProductionOrderPayload::id,
            CancelProductionOrderPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
