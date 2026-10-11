# Design: m10-lift-upgrades

## Context

See proposal.md for the why. A shaft is a maximal run of stacked `ElevatorBlock` segments; `Shafts.group` builds them in `WarehouseNetwork` when `shaftsDirty` is set, and `ShuttleFleet.refreshShafts` keeps one `LiftCar` per shaft bottom. Since `m10-shuttle-tiers` the car speeds up and brakes: `LiftCar.tick(speed, accel)` with `lift_speed` and `lift_accel` from `warehouse.json`, and `LiftSyncPayload` carries a `ShuttleView.Drive(speed, top, accel)` that the client steps with `ShuttleDrive.next`. The Foundation has `UpgradeSlots` (an `ItemStackHandler` with one upgrade per slot and a change callback) and the Speed Upgrade item `ServusFoundationItems.SPEED_UPGRADE`; the Packing Station shows its slots in a menu built from `SlotItemHandler`s.

## Goals / Non-Goals

Goals: Speed upgrade slots per shaft, a GUI from any segment, items in the lowest segment that follow it, faster cars.

Non-goals: other upgrade kinds for lifts, a visual change of the car, upgrades per segment.

## Decisions

### D1. Slots and the item
`ElevatorBlockEntity` gets `LiftUpgradeSlots`, a subclass of the Foundation `UpgradeSlots` whose `isItemValid` takes only `ServusFoundationItems.SPEED_UPGRADE` (the Foundation slots take every upgrade in the tag, and only Speed makes sense for a car). Size `lift_upgrade_slots` from `warehouse.json` (4), read when the block entity is made; a save with more slots than the file allows loads the first ones only, the same as the Packing Station. Every segment has the handler, but only the lowest one ever holds items. Saved under `Upgrades` like the Packing Station. The change callback marks the block entity changed and posts `WarehouseEvent.LiftUpgradesChanged(pos)`.

### D2. Opening the GUI
`ElevatorBlock.useItemOn` returns `SKIP_DEFAULT_BLOCK_INTERACTION` for every non-empty hand, so an elevator item, the carrot, rails and everything else keep their own use (checked in `ServerPlayerGameMode.useItemOn` of `neoforge-21.1.255-sources.jar`: after `useItemOn` returns `PASS_TO_DEFAULT_BLOCK_INTERACTION` for the main hand, `useWithoutItem` runs, which would open the GUI before the item's `useOn`). `useWithoutItem` (empty hand only) finds the lowest segment below the clicked one (`LiftUpgrades.bottom`) and opens `ElevatorMenu` on its block entity with `player.openMenu(provider, buf -> ...)` (`IPlayerExtension.openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>)`), writing the bottom position and the slot count. Climbing, standing and sinking come from collision and `isScaffolding`, which this does not touch.

### D3. Menu and screen
`ElevatorMenu` (registered with `IMenuTypeExtension.create(ElevatorMenu::fromNetwork)` in `ServusStorageMenus`): one centred row of upgrade slots, then the player inventory and hotbar; shift click moves Speed upgrades in and out like `PackingStationMenu.quickMoveStack`; `stillValid` checks the bottom segment is still an elevator within reach. `ElevatorScreen` paints with `PanelPainter` like the Packing Station: title, the slot row and a hint line; the client has no stats file, so it shows no speed numbers. No new payloads: slot contents sync through the menu.

### D4. Upgrades follow the lowest segment
Pure `LiftUpgrades` (no Minecraft imports) answers which segment keeps the upgrades, by y in one column, with an `IntPredicate` for "is a segment":
- `bottom(y, segment)`: walk down from y while the block below is a segment.
- `heirOnRemove(y, segment)`: when the segment at y goes, the upgrades it holds may go to y + 1 if that is a segment, else nowhere (drop). The block adds one condition the pure rule cannot see: the heir must still survive (`canSurvive`) after the removal.
- On place at y: if y + 1 is a segment, it was the bottom of the shaft above, so its upgrades move to `bottom(y, segment)` of the new joined shaft.

`ElevatorBlock.onPlace` (new block, not a state change) and `onRemove` (block replaced by another block) do the moves on the server: take every item out of the source handler, insert into the target handler, pop what does not fit at the source position. A removed segment that holds upgrades and has no heir pops them. Segments need a segment or a solid block below (`canSurvive`), so removing the lowest segment normally brings the whole shaft above down like scaffolding through `neighborChanged`; handing the upgrades up would drop them with the top segment, far from the player. So the heir takes them only when it still stands, which happens when the lowest segment is replaced by a solid block; otherwise they drop where the lowest segment was. The middle segment rule needs nothing: the lower part's bottom keeps the items, and the segments above the break fall.

### D5. The car
`LiftCar` gets `upgrades` (set by the fleet). `ShuttleFleet.refreshShafts` reads the count of filled slots of each car's bottom block entity after it keeps or creates the cars; `WarehouseNetwork.handle(LiftUpgradesChanged)` sets `shaftsDirty` when the position is one of its elevators, so the count is read on the next tick, never per tick. `ShuttleFleet.tick` moves each car with `LiftUpgrades.speed(lift_speed, lift_speed_per_upgrade, n)` and `LiftUpgrades.accel(lift_accel, lift_accel_per_upgrade, n)`, and `sendLift` sends that car's own top speed and accel in the `Drive`. `refreshShafts` already sets `syncPending`, so clients get the new numbers.

### D6. Numbers
`warehouse.json`: `lift_upgrade_slots` 4, `lift_speed_per_upgrade` 0.05, `lift_accel_per_upgrade` 0.0025, read in `StorageStatsManager`. With four upgrades the car runs at 0.3 blocks per tick with 0.015 accel.

## Risks / Trade-offs

- A segment placed under a shaft by a piston or another mod goes through `onPlace` too, so the move still happens.
- Upgrades that do not fit when two upgraded shafts are joined drop at the joining position; players see them pop.
- Reading the count from a block entity in `refreshShafts` needs the bottom chunk loaded; an unloaded bottom counts as zero until it loads (the network rebuilds its shafts on load).
