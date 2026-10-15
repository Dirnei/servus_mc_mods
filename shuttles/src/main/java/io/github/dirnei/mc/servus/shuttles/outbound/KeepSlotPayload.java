package io.github.dirnei.mc.servus.shuttles.outbound;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Client to server: keep this many of the item in a keep slot; an empty item or amount below one clears the slot. */
public record KeepSlotPayload(int slot, ItemStack item, int amount) implements CustomPacketPayload {

    public static final Type<KeepSlotPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "keep_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KeepSlotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, KeepSlotPayload::slot,
            ItemStack.OPTIONAL_STREAM_CODEC, KeepSlotPayload::item,
            ByteBufCodecs.VAR_INT, KeepSlotPayload::amount,
            KeepSlotPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
