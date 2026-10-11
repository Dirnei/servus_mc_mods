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

    /** The inbound station at {@code pos} has something in its buffer. */
    record Intake(BlockPos pos) implements WarehouseEvent {
    }

    /** The output slots of the terminal at {@code pos} changed; held deliveries may fit now. */
    record OutputChanged(BlockPos pos) implements WarehouseEvent {
    }

    /** The Speed upgrades of the elevator segment at {@code pos} changed; its car reads them again. */
    record LiftUpgradesChanged(BlockPos pos) implements WarehouseEvent {
    }
}
