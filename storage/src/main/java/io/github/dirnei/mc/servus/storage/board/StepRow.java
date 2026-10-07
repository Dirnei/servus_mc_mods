package io.github.dirnei.mc.servus.storage.board;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * One step of an order as the board shows it.
 *
 * @param dock    the dock the step runs on
 * @param machine the name of the block the dock faces
 * @param output  the step's first output, may be empty
 * @param batches how often the recipe runs
 * @param state   waiting_dock, feeding, producing or done
 * @param reason  why the step waits, empty when it does not
 */
public record StepRow(BlockPos dock, String machine, ItemStack output, int batches, String state, String reason) {

    public static final StreamCodec<RegistryFriendlyByteBuf, StepRow> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, StepRow::dock,
            ByteBufCodecs.STRING_UTF8, StepRow::machine,
            ItemStack.OPTIONAL_STREAM_CODEC, StepRow::output,
            ByteBufCodecs.VAR_INT, StepRow::batches,
            ByteBufCodecs.STRING_UTF8, StepRow::state,
            ByteBufCodecs.STRING_UTF8, StepRow::reason,
            StepRow::new);
}
