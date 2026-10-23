package io.github.dirnei.mc.servus.foundation.item;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Implemented by blocks that answer the Data Potato with a chat report. The
 * block must let the potato through in {@code useItemOn} (return
 * {@code PASS}) so the potato gets the click.
 */
public interface PotatoInspectable {

    /**
     * Called on the server only. The report must not change anything.
     *
     * @param hit where the click landed, in world coordinates
     * @return the chat lines, or an empty list when there is nothing to report
     */
    List<Component> potatoReport(Level level, BlockPos pos, BlockState state, Vec3 hit);
}
