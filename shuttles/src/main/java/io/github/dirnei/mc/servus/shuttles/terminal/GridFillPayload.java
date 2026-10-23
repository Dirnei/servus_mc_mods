package io.github.dirnei.mc.servus.shuttles.terminal;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Client to server (JEI transfer): set the order grid's ghost cells. Entry i
 * lists the stacks cell i accepts, empty for a cell that stays empty.
 */
public record GridFillPayload(List<List<ItemStack>> slots) implements CustomPacketPayload {

    public static final Type<GridFillPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "grid_fill"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GridFillPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_LIST_STREAM_CODEC.apply(ByteBufCodecs.list(9)), GridFillPayload::slots,
            GridFillPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
