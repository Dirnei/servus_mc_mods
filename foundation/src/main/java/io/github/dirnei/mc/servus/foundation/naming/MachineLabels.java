package io.github.dirnei.mc.servus.foundation.naming;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** The one place messages ask how a machine at a position reads. */
public final class MachineLabels {

    private MachineLabels() {
    }

    /**
     * The loaded named machine at the position: its block entity, or the machine a
     * {@link NamedMachinePart} there belongs to. Never loads a chunk.
     */
    public static Optional<NamedMachine> machineAt(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return Optional.empty();
        }
        if (level.getBlockEntity(pos) instanceof NamedMachine named) {
            return Optional.of(named);
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof NamedMachinePart part) {
            BlockPos owner = part.namedMachinePos(level, pos, state);
            if (owner != null && !owner.equals(pos) && level.isLoaded(owner) && level.getBlockEntity(owner) instanceof NamedMachine named) {
                return Optional.of(named);
            }
        }
        return Optional.empty();
    }

    /** The label of the loaded named machine there, else the name of the block there, else {@code unloaded}. */
    public static Component at(Level level, BlockPos pos, Component unloaded) {
        if (!level.isLoaded(pos)) {
            return unloaded;
        }
        return machineAt(level, pos).map(NamedMachine::label).orElseGet(() -> level.getBlockState(pos).getBlock().getName());
    }

    /** The label of the machine there, or "Machine" while its chunk is not loaded. */
    public static Component at(Level level, BlockPos pos) {
        return at(level, pos, unloaded());
    }

    /** As {@link #at(Level, BlockPos)}, for chat and action bar messages: a named machine in its kind's chat colour. */
    public static Component chatAt(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return unloaded();
        }
        return machineAt(level, pos).map(NamedMachine::chatLabel).orElseGet(() -> level.getBlockState(pos).getBlock().getName());
    }

    private static Component unloaded() {
        return Component.translatable("machine.servus_foundation.label.unloaded");
    }
}
