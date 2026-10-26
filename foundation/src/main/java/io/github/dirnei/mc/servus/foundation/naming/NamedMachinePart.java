package io.github.dirnei.mc.servus.foundation.naming;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A block without a block entity of its own that stands for a named machine,
 * such as a port of a multiblock that sits on the data line. Implemented by
 * the block; {@link MachineLabels} and numbering follow it to the machine.
 */
public interface NamedMachinePart {

    /** Where the block entity of the named machine this part belongs to stands, or null when it belongs to none. */
    @Nullable BlockPos namedMachinePos(Level level, BlockPos pos, BlockState state);
}
