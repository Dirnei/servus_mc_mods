# Proposal

## Why

Shuttles take Speed upgrades, but the lift car of a shaft always runs at `lift_speed`, so a tall warehouse with fast shuttles waits on its lifts. The user asked in the play check for a small elevator GUI to fit Speed upgrades, with the items kept in the lowest segment and the GUI reachable from any segment.

## What Changes

- Every shaft gets upgrade slots (count from `warehouse.json`, example `lift_upgrade_slots` 4) that take only the Foundation Speed Upgrade, one per slot.
- A right click with an empty hand on any segment opens a small GUI with the upgrade slots of that shaft and the player inventory. Clicks with an item in hand keep doing what they did (an elevator item still builds the shaft up, the carrot still dismantles), and climbing is unchanged.
- The items are stored in the shaft's lowest segment. When the lowest segment changes, the upgrades follow: a segment placed below a shaft takes them over, a segment placed between two shafts joins them and the upper shaft's upgrades move down (those that do not fit drop), a bottom replaced by a block the shaft stands on hands them to the segment above, and a broken bottom, which brings the shaft above down like scaffolding, drops them where it was. A shaft broken in the middle keeps its upgrades in its lower part.
- Each Speed upgrade raises the car's top speed and its acceleration, additive like the shuttles (examples `lift_speed_per_upgrade` 0.05 and `lift_accel_per_upgrade` 0.0025). The car keeps its upgrade count, read when the shaft or its upgrades change, never per tick. Clients draw the upgraded car through the speed data the lift sync already carries.

## Capabilities

### New Capabilities
- `storage/lift-upgrades`: the upgrade slots of a shaft, the elevator GUI, where the items live and how they follow the lowest segment, and what the upgrades do to the car.

### Modified Capabilities

## Impact

- `storage/.../rail/ElevatorBlock`, `ElevatorBlockEntity`: interaction, upgrade handler, item moves on place and remove.
- New `storage/.../menu/ElevatorMenu`, `storage/.../client/ElevatorScreen`, menu registration in `ServusStorageMenus` and `ServusStorageClient`.
- `storage/.../shuttle/LiftCar` (upgrade count), `ShuttleFleet` (per car speed and accel), `WarehouseNetwork` (new mailbox event), `StorageStatsManager` and `warehouse.json` (three numbers), pure `LiftUpgrades`.
- Lang through datagen; docs page and `PLAN.md` M10.
