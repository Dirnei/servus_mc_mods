package io.github.dirnei.mc.servus.shuttles.gantry;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: the lock picked for a tank of the open Fluid Gantry menu.
 * {@code kind} is {@link #ANY}, {@link #CONTENT} (lock to what the tank holds)
 * or {@link #FLUID} with the fluid's id.
 */
public record SetGantryLockPayload(int tank, int kind, Optional<Identifier> fluid) implements CustomPacketPayload {

    public static final int ANY = 0;
    public static final int CONTENT = 1;
    public static final int FLUID = 2;

    public static final Type<SetGantryLockPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "set_gantry_lock"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetGantryLockPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetGantryLockPayload::tank,
            ByteBufCodecs.VAR_INT, SetGantryLockPayload::kind,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), SetGantryLockPayload::fluid,
            SetGantryLockPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
