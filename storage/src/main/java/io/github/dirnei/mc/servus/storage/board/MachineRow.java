package io.github.dirnei.mc.servus.storage.board;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * One station of the warehouse as the board shows it.
 *
 * @param station    the station
 * @param machine the name of the block the station faces
 * @param working the first output of the active recipe, empty when none runs
 * @param state   working, idle, no_recipes or no_producer
 */
public record MachineRow(BlockPos station, String machine, ItemStack working, String state) {

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineRow> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MachineRow::station,
            ByteBufCodecs.STRING_UTF8, MachineRow::machine,
            ItemStack.OPTIONAL_STREAM_CODEC, MachineRow::working,
            ByteBufCodecs.STRING_UTF8, MachineRow::state,
            MachineRow::new);
}
