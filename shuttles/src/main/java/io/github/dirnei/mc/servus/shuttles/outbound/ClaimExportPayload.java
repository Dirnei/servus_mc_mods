package io.github.dirnei.mc.servus.shuttles.outbound;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Client to server: claim the export of the item for a keep slot of the open Outbound Station. */
public record ClaimExportPayload(int slot, ItemStack item) implements CustomPacketPayload {

    public static final Type<ClaimExportPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "claim_export"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimExportPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ClaimExportPayload::slot,
            ItemStack.STREAM_CODEC, ClaimExportPayload::item,
            ClaimExportPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
