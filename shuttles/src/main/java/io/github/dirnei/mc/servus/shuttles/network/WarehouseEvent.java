package io.github.dirnei.mc.servus.shuttles.network;

import net.minecraft.core.BlockPos;

/** Events posted to a warehouse network's mailbox. */
public sealed interface WarehouseEvent {

    /** A source's contents changed; poll it next tick. */
    record Dirty(BlockPos pos) implements WarehouseEvent {
    }

    /** The node at {@code pos} was stamped into another zone or cleared. */
    record ZoneChanged(BlockPos pos) implements WarehouseEvent {
    }

    /** The Inbound Station at {@code station} took less of the item than offered because of its rule's cap. */
    record CapRefused(ItemKey key, BlockPos station) implements WarehouseEvent {
    }

    /** The inbound station at {@code pos} has something in its buffer. */
    record Intake(BlockPos pos) implements WarehouseEvent {
    }

    /** The output slots of the terminal at {@code pos} changed; held deliveries may fit now. */
    record OutputChanged(BlockPos pos) implements WarehouseEvent {
    }

    /** The tanks, locks, mode or upgrades of the Fluid Gantry at {@code pos} changed; its fluid stock is read again. */
    record FluidChanged(BlockPos pos) implements WarehouseEvent {
    }

    /** The Speed upgrades of the elevator segment at {@code pos} changed; its car reads them again. */
    record LiftUpgradesChanged(BlockPos pos) implements WarehouseEvent {
    }
}
