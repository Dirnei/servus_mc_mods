package io.github.dirnei.mc.servus.shuttles.fluidstation;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: a player picked the Fluid Outbound's keep fluid by its
 * registry id, or nothing to clear the keep. The server ignores an id that
 * names no registered source fluid.
 */
public record SetFluidKeepPayload(Optional<Identifier> fluid) implements CustomPacketPayload {

    public static final Type<SetFluidKeepPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "set_fluid_keep"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetFluidKeepPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), SetFluidKeepPayload::fluid,
            SetFluidKeepPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
