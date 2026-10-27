package io.github.dirnei.mc.servus.shuttles.fluidstation;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: a player clicked a Fluid Outbound keep cell. The server
 * keeps the fluid of the container on the player's cursor in that tank, or
 * clears the keep when the cursor holds no filled fluid container.
 */
public record SetFluidKeepPayload(int tank) implements CustomPacketPayload {

    public static final Type<SetFluidKeepPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "set_fluid_keep"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetFluidKeepPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetFluidKeepPayload::tank,
            SetFluidKeepPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
