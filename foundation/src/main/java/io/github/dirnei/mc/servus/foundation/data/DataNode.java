package io.github.dirnei.mc.servus.foundation.data;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * What a data network needs from a block that carries data: its level,
 * whether it is gone, and through which sides data passes. The Utility Duct's
 * data line implements it; other mods may too.
 */
public interface DataNode {

    Level dataLevel();

    boolean dataRemoved();

    /** Whether data passes this side: to another data node, or to a member. */
    boolean dataSideOpen(Direction side);
}
