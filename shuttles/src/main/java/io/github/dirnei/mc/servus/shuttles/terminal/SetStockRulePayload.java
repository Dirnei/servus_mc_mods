package io.github.dirnei.mc.servus.shuttles.terminal;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Client to server: keep min to max of the item; a negative min removes the rule. */
public record SetStockRulePayload(ItemStack item, int min, int max) implements CustomPacketPayload {

    public static final Type<SetStockRulePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "set_stock_rule"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetStockRulePayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, SetStockRulePayload::item,
            ByteBufCodecs.INT, SetStockRulePayload::min,
            ByteBufCodecs.INT, SetStockRulePayload::max,
            SetStockRulePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
