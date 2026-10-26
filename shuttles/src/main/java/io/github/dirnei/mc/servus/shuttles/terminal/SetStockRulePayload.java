package io.github.dirnei.mc.servus.shuttles.terminal;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Client to server: the rule of the item, or its removal.
 *
 * @param min    the Make minimum, -1 together with {@code max} for no Make pair
 * @param max    the Make maximum
 * @param cap    refuse at Inbound above this, 0 for none
 * @param export ship the surplus above this to an Outbound Station, -1 for none
 * @param remove whether the rule is removed; the thresholds are then ignored
 */
public record SetStockRulePayload(ItemStack item, int min, int max, int cap, int export, boolean remove) implements CustomPacketPayload {

    public static final Type<SetStockRulePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "set_stock_rule"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetStockRulePayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, SetStockRulePayload::item,
            ByteBufCodecs.INT, SetStockRulePayload::min,
            ByteBufCodecs.INT, SetStockRulePayload::max,
            ByteBufCodecs.INT, SetStockRulePayload::cap,
            ByteBufCodecs.INT, SetStockRulePayload::export,
            ByteBufCodecs.BOOL, SetStockRulePayload::remove,
            SetStockRulePayload::new);

    /** Removes the rule of the item. */
    public static SetStockRulePayload removal(ItemStack item) {
        return new SetStockRulePayload(item, -1, -1, 0, -1, true);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
