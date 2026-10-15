package io.github.dirnei.mc.servus.shuttles.shuttle.module;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: the player finished that step of the build by hand in the open Shuttle Workshop. */
public record ShuttleBuildStepPayload(int step) implements CustomPacketPayload {

    public static final Type<ShuttleBuildStepPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "shuttle_build_step"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShuttleBuildStepPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ShuttleBuildStepPayload::step,
            ShuttleBuildStepPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
