package io.github.dirnei.mc.servus.shuttles.fluidstation;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: a player set a level of the Fluid Outbound open in their
 * GUI, by drag, wheel or typed number. The server limits it to the tank's
 * capacity and moves the other level along when they would cross.
 *
 * @param restock      true for the restock level, false for the keep amount
 * @param millibuckets the new level in mB
 */
public record SetFluidLevelPayload(boolean restock, int millibuckets) implements CustomPacketPayload {

    public static final Type<SetFluidLevelPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "set_fluid_level"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetFluidLevelPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SetFluidLevelPayload::restock,
            ByteBufCodecs.VAR_INT, SetFluidLevelPayload::millibuckets,
            SetFluidLevelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
