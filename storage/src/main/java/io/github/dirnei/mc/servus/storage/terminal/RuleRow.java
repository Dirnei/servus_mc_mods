package io.github.dirnei.mc.servus.storage.terminal;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * One stock rule as the GUI shows it.
 *
 * @param stack a one item stack as display
 * @param min   the minimum
 * @param max   the maximum
 * @param state {@code ok}, {@code ordering}, {@code no_producer} or {@code no_cpu}
 */
public record RuleRow(ItemStack stack, int min, int max, String state) {

    public static final StreamCodec<RegistryFriendlyByteBuf, RuleRow> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, RuleRow::stack,
            ByteBufCodecs.VAR_INT, RuleRow::min,
            ByteBufCodecs.VAR_INT, RuleRow::max,
            ByteBufCodecs.STRING_UTF8, RuleRow::state,
            RuleRow::new);
}
