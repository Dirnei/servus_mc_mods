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
 * @param stack       a one item stack as display
 * @param min         the Make minimum, -1 without a Make pair
 * @param max         the Make maximum, -1 without a Make pair
 * @param cap         refuse at Inbound Stations above this, 0 for no cap
 * @param state       {@code ok}, {@code ordering}, {@code no_producer}, {@code no_cpu} or {@code refusing}
 * @param detail      for {@code refusing} the label of the station that refused, else empty
 * @param export      ship the surplus above this, -1 without an export
 * @param holder      the label of the Outbound Station holding the export, empty while none does
 * @param exportState {@code no_station}, {@code exporting}, {@code station_full} or {@code ok}; empty without an export
 */
public record RuleRow(ItemStack stack, int min, int max, int cap, String state, Component detail, int export, Component holder,
                      String exportState) {

    /** Nine fields, written out by hand. */
    public static final StreamCodec<RegistryFriendlyByteBuf, RuleRow> STREAM_CODEC = StreamCodec.of(
            (buf, row) -> {
                ItemStack.STREAM_CODEC.encode(buf, row.stack());
                ByteBufCodecs.VAR_INT.encode(buf, row.min() + 1);
                ByteBufCodecs.VAR_INT.encode(buf, row.max() + 1);
                ByteBufCodecs.VAR_INT.encode(buf, row.cap());
                ByteBufCodecs.STRING_UTF8.encode(buf, row.state());
                ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, row.detail());
                ByteBufCodecs.VAR_INT.encode(buf, row.export() + 1);
                ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, row.holder());
                ByteBufCodecs.STRING_UTF8.encode(buf, row.exportState());
            },
            buf -> new RuleRow(ItemStack.STREAM_CODEC.decode(buf), ByteBufCodecs.VAR_INT.decode(buf) - 1, ByteBufCodecs.VAR_INT.decode(buf) - 1,
                    ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf) - 1, ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf)));

    /** The row of a rule with its states, the label of the station that refused and the label of the export's station. */
    public static RuleRow of(StockRule rule, String state, Component detail, Component holder, String exportState) {
        return new RuleRow(rule.key().stack(1), rule.hasMake() ? rule.make().min() : -1, rule.hasMake() ? rule.make().max() : -1,
                rule.cap(), state, detail, rule.export(), holder, rule.hasExport() ? exportState : "");
    }

    public boolean hasMake() {
        return max >= 0;
    }

    public boolean hasCap() {
        return cap > 0;
    }

    public boolean hasExport() {
        return export >= 0;
    }
}
