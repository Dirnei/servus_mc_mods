package io.github.dirnei.mc.servus.shuttles.garage;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The shuttles of an open garage menu that park with cargo that has nowhere
 * to go, server to client.
 *
 * @param containerId the garage menu
 * @param lines       one line per such shuttle: its label, the cargo and why it waits
 * @param slots       the garage slots of those shuttles, in the order of the lines
 */
public record GarageStrandedPayload(int containerId, List<Component> lines, List<Integer> slots) implements CustomPacketPayload {

    public static final Type<GarageStrandedPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "garage_stranded"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GarageStrandedPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GarageStrandedPayload::containerId,
            ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list()), GarageStrandedPayload::lines,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), GarageStrandedPayload::slots,
            GarageStrandedPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
