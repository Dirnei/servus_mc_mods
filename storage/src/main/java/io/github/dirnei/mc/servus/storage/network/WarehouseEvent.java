package io.github.dirnei.mc.servus.storage.network;

import net.minecraft.core.BlockPos;

/** Events posted to a warehouse network's mailbox. */
public sealed interface WarehouseEvent {

    /** A source's contents changed; poll it next tick. */
    record Dirty(BlockPos pos) implements WarehouseEvent {
    }

    /** A block next to the node at {@code pos} changed; rediscover its neighbours. */
    record NeighborsChanged(BlockPos pos) implements WarehouseEvent {
    }

    /** The node at {@code pos} was stamped into another zone or cleared. */
    record ZoneChanged(BlockPos pos) implements WarehouseEvent {
    }
}
