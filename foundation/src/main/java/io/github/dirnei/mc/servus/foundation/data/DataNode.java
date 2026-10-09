package io.github.dirnei.mc.servus.foundation.data;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * What a data network needs from a block that carries data: its level,
 * whether it is gone, and through which sides data passes. The Utility Duct's
 * data line implements it as a cable; blocks that carry the network to
 * touching blocks implement it as a carrier, see {@link DataCarrier}.
 */
public interface DataNode {

    Level dataLevel();

    boolean dataRemoved();

    /** Whether data passes this side: to another data node, or to a member. */
    boolean dataSideOpen(Direction side);

    /**
     * True for a cable such as the duct's data line: it finds members on its
     * sides and counts as a cable in the summary. False for a carrier such as
     * a terminal: it links touching data nodes and is a member itself, but
     * never makes its other neighbours members.
     */
    default boolean dataCable() {
        return true;
    }
}
