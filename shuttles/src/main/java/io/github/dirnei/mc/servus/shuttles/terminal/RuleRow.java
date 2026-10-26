package io.github.dirnei.mc.servus.shuttles.terminal;

import io.github.dirnei.mc.servus.shuttles.order.StockRule;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * One stock rule as the GUI shows it.
 *
 * @param stack  a one item stack as display
 * @param min    the Make minimum, -1 without a Make pair
 * @param max    the Make maximum, -1 without a Make pair
 * @param cap    refuse at Inbound Stations above this, 0 for no cap
 * @param state  {@code ok}, {@code ordering}, {@code no_producer}, {@code no_cpu} or {@code refusing}
 * @param detail for {@code refusing} the label of the station that refused, else empty
 */
public record RuleRow(ItemStack stack, int min, int max, int cap, String state, Component detail) {

    public static final StreamCodec<RegistryFriendlyByteBuf, RuleRow> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, RuleRow::stack,
            ByteBufCodecs.VAR_INT, row -> row.min() + 1,
            ByteBufCodecs.VAR_INT, row -> row.max() + 1,
            ByteBufCodecs.VAR_INT, RuleRow::cap,
            ByteBufCodecs.STRING_UTF8, RuleRow::state,
            ComponentSerialization.TRUSTED_STREAM_CODEC, RuleRow::detail,
            (stack, min, max, cap, state, detail) -> new RuleRow(stack, min - 1, max - 1, cap, state, detail));

    public static RuleRow of(StockRule rule, String state, Component detail) {
        return new RuleRow(rule.key().stack(1), rule.hasMake() ? rule.make().min() : -1, rule.hasMake() ? rule.make().max() : -1,
                rule.cap(), state, detail);
    }

    public boolean hasMake() {
        return max >= 0;
    }

    public boolean hasCap() {
        return cap > 0;
    }
}
