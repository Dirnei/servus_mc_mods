package io.github.dirnei.mc.servus.delicacies.aquarium;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: the auto catch switch and keep level set in the aquarium GUI of the master at {@code pos}. */
public record AquariumSettingsPayload(BlockPos pos, boolean autoCatch, int keep) implements CustomPacketPayload {

    public static final Type<AquariumSettingsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusDelicaciesIds.MOD_ID, "aquarium_settings"));

    public static final StreamCodec<ByteBuf, AquariumSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AquariumSettingsPayload::pos,
            ByteBufCodecs.BOOL, AquariumSettingsPayload::autoCatch,
            ByteBufCodecs.VAR_INT, AquariumSettingsPayload::keep,
            AquariumSettingsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
