package io.github.dirnei.mc.servus.shuttles.fluidstation;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: a player clicked the Fluid Outbound's keep cell. The
 * server keeps the fluid of the container on the player's cursor, or clears
 * the keep when the cursor holds no filled fluid container.
 */
public record SetFluidKeepPayload() implements CustomPacketPayload {

    public static final SetFluidKeepPayload INSTANCE = new SetFluidKeepPayload();

    public static final Type<SetFluidKeepPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "set_fluid_keep"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetFluidKeepPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
