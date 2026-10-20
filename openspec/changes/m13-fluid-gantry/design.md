# Design

## Context

Paths: `S` = `shuttles/src/main/java/io/github/dirnei/mc/servus/shuttles`, `F` = `foundation/src/main/java/io/github/dirnei/mc/servus/foundation`.

- Warehouse nodes extend `S/network/StorageNodeBlockEntity` (`kind()`, `acceptsRail(Side)`, `docksRail()`, `showsDock()`, `postDirty()`). `S/network/WarehouseConnectionRule.NodeKind` is `STOCK, RAIL, ELEVATOR, STATION, GARAGE, RACK`; `railOnly(kind)` is true for RACK, STATION and GARAGE. `S/rail/RailBlock` docks onto a neighbour with `docksRail() && acceptsRail(opposite)` and draws an arm only when `showsDock()`.
- The item index is `S/index/WarehouseIndex<S, K>` (generic, pure) used as `WarehouseIndex<BlockPos, ItemKey>` in `S/network/WarehouseNetwork`. Events are records in `S/network/WarehouseEvent` drained in `WarehouseNetwork.handle`.
- Multi block precedents: `F/machine/TwoHalves` (one item places two blocks, the lower owns the block entity, `keepTogether` in `updateShape`, `breakPartner` in `playerWillDestroy`) and `F/machine/sink/SinkBlock` (`PART` property and a layout of offsets).
- Fluid parts: `F/transfer/FluidSlot` (single tank, `setCapacity`, save and load), `F/transfer/LegacyFluidHandler` (adapter to `ResourceHandler<FluidResource>`), `F/client/FluidSprites` (still sprite and tint), `F/client/FluidGauge` (GUI gauge). The Field Head syncs its water level in steps through its update tag and draws real water in `S/client/FieldHeadRenderer`.
- Lists of choices: `S/client/CropPickerEntries` and the crop picker of the Field Head screen.

## Decisions

### D1. Parts and layout (PLAN.md open decision "forming" settled: one item)

`FluidGantryBlock` has `PART` (`LEFT_LOW, LEFT_HIGH, RIGHT_LOW, RIGHT_HIGH, BEAM`) and `AXIS` (the rail's run axis, X or Z). Left is the negative side across the run (west for a north to south run, north for an east to west run). Pure `gantry/GantryLayout` gives, for an axis and the rail position, the five part positions and, for any part, the position of the master (`LEFT_LOW`). `FluidGantryItem.useOn` on a rail checks the rail is straight along one axis (its connections lie on that axis; a lone rail takes the axis from the player's facing), the five spaces are replaceable, and both leg footings have a sturdy top; then it places all five parts in one go and copies the item's data into the master. Failures send an action bar message.

Removal: any part's `affectNeighborsAfterRemoval` removes the other parts (no loot table, so nothing drops for them); the master's block entity builds the one drop in `preRemoveSideEffects` (D7), and `playerWillDestroy` by a creative player tells it to drop nothing. `updateShape` of a part whose master is gone turns it into air, as `TwoHalves.keepTogether` does for two halves.

### D2. Master block entity and node kind

Only `LEFT_LOW` has a block entity: `FluidGantryBlockEntity extends StorageNodeBlockEntity`. A new `NodeKind.GANTRY` is rail only and docks rails; `acceptsRail` is true only for the master's inner face toward the rail, and `showsDock()` is false. A rail draws a plain arm toward a docked node whose dock it does not show (the garage), so `StorageNodeBlockEntity` gets `bendsRail()` (true by default, false for the gantry) and `RailBlock.connectsTo` leaves such a node out of its connection flags: the rail keeps its straight look, while the network still links the two through the graph. `WarehouseNetwork.onNodeAdded` keeps gantries in their own map (not in `stations`, so no item put-away code sees them). `dockRails(gantry)` is the rail between the legs. Other parts forward clicks, carrot use and bucket use to the master.

### D3. Tanks and locks are pure rules

Two `FluidSlot`s on the master. Pure `gantry/GantryTanks<K>` (no Minecraft types; `K` a fluid key) holds the rules for two tanks with lock (key or any), content key and amount, capacity: `accepts(tank, key)`, `fillOrder(key)` (locked to the key, then Any holding it, then empty Any), `warning(tank)` (content differs from a lock). Pure `gantry/GantryCapacity`: `capacity(base, perUpgrade, upgrades)` and `canRemoveUpgrade(base, perUpgrade, upgrades, largestAmount)`. The block entity applies them to the `FluidSlot`s. Locks name a fluid without components, so the lock rules use the `Fluid` as their key; one fluid per tank (components included) is kept by `FluidSlot` itself. The fluid stock (D5) is keyed by Foundation's `FluidChannelNetwork.FluidKey` (fluid plus components, compared without amount).

### D4. Modes and capabilities

`GantryMode { STORAGE, IMPORT, EXPORT }` saved on the master. The fluid capability is registered with `RegisterCapabilitiesEvent.registerBlock(Capabilities.Fluid.BLOCK, provider, FLUID_GANTRY)` (as `ServusAlchemyEvents` does for the Purifier), so every part answers: the provider finds the master through `GantryLayout` and returns `null` in Storage mode, a fill only `LegacyFluidHandler` over both tanks (fill order from D3) in Import mode and a drain only one in Export mode. Changing the mode calls `level.invalidateCapabilities(pos)` for all five parts so ducts re-check their endpoints.

### D5. Fluid stock in the warehouse

`WarehouseNetwork` gets a second index `WarehouseIndex<TankRef, FluidKey>` with `record TankRef(BlockPos gantry, int tank)` in `gantry/`. A Storage gantry posts `WarehouseEvent.FluidChanged(pos)` from `FluidSlot.onContentsChanged`, lock changes, upgrade changes and mode changes; the network drains it in `handle` and replaces that gantry's two snapshots (or removes them when the gantry is not in Storage mode or leaves). Nothing is polled per tick. The network keeps per tank view data (lock, fluid, amount, capacity) for the command and for the tanker changes. `/servus_warehouse fluids <pos>` prints totals and every gantry (mode, tanks).

### D6. Client sync and look

The master's update tag carries the mode, the two locks (fluid id or none), the two fluids and their fill steps (32 per column, `GantryLook.level(amount, capacity, steps)` pure, with the step count, since clients do not read `gantry.json`), and is sent only when one of them changes, as the Field Head does with its water level. `ONLINE` and `ACTIVITY` (`S/network/StorageNodeBlocks`) are block state properties of every part's state; the master copies them onto the beam when its own state changes, so the beam model's lamps follow without a renderer.

Static parts are datagen models per `PART` and `AXIS` (plus `MODE`, `ONLINE` and `ACTIVITY` on the beam): steel base and cap (`electric_pump_steel`), blackened iron corner posts (`heavy_rack_iron`), tank glass (`fluid_tank_glass`) inset 0.3 px from the outer faces and running through both leg blocks as one tank (no band or floor where they meet), steel I-beam between the leg caps (ends 0.05 px short of the legs), copper feed pipes and a steel spout sleeve with an iron inner pipe and copper nozzle, sign plates on both faces along the rail. Mode marks and lamps are small textures by job script. `FluidGantryRenderer` on the master draws the two fluid columns (inset inside the glass, `FluidSprites.still` and `tint` with the level's tint at the leg, one box per leg block so the texture stays within its sprite; the box drawing moves out of Foundation's `FluidTankRenderer` into a shared `client/FluidBox`), and on each lock plate the locked fluid's still sprite (or nothing over the striped Any texture). Render bounds cover the whole 3 by 2 frame. The CLAUDE.md flicker check lists every overlapping box pair before the look task is done.

### D7. Dropped item keeps everything

A data component `GANTRY_CONTENTS` (`record GantryContents(FluidStack left, FluidStack right, Optional<Identifier> leftLock, Optional<Identifier> rightLock, GantryMode mode, int upgrades)` with a codec) is set on the dropped item; Capacity Upgrades ride along as the count, not as separate drops. The block has no loot table; the master's block entity pops the item in `preRemoveSideEffects` when it goes away, unless a creative player broke the gantry. An empty gantry drops a plain item, so fresh ones stack. The tooltip lists the fluids, amounts and mode.

### D8. GUI and payloads

`FluidGantryMenu` with the capacity upgrade slots (Foundation `CapacityUpgradeSlots`, removal guarded by `GantryCapacity.canRemoveUpgrade` in `mayPickup`), the player inventory and data slots for the gauges. `FluidGantryScreen`: two `FluidGauge`s, lock buttons opening a picker list built like `CropPickerEntries` from every registered source fluid with a bucket, plus Any and "Lock to content", a mode button through `clickMenuButton`, the state line. `SetGantryLockPayload(tank, kind, fluid)` with `kind` Any, Lock to content or a fluid (with its id), acting on the sender's open gantry menu (so no position); registered in the Shuttles payload registrar (grep every payload `TYPE` is registered before launching).

### D9. Data

New `data/servus_shuttles/shuttles/gantry.json`: `tank_capacity` 16000, `capacity_per_upgrade` 16000, `upgrade_slots` 4, `level_steps` 32, read by `ShuttlesStatsManager`. Recipe (datagen, shaped): `ICI / T T / IRI` with iron ingots, a copper ingot, two `servus_foundation:fluid_tank`, a Warehouse Rail.

## NeoForge APIs

- `RegisterCapabilitiesEvent.registerBlock` with `Capabilities.Fluid.BLOCK` (used in `alchemy/ServusAlchemyEvents` on 26.1.2), `Level.invalidateCapabilities`.
- `BlockItem.useOn`, `Block.playerWillDestroy`, `BlockBehaviour.updateShape`, `affectNeighborsAfterRemoval` (as `F/machine/sink/SinkBlock`) and `BlockEntity.preRemoveSideEffects` (as `S/station/StationBlockEntity`) on 26.1.2; `onPlace`, which the server calls for every state change, copies the master's mode and lamps onto the beam.
- `BlockEntityRenderer` with `shouldRenderOffScreen` and `getRenderBoundingBox` (as `S/client/ElevatorRenderer`), `FluidSprites` from Foundation.
- `ClientboundBlockEntityDataPacket` and `getUpdateTag` (as `FieldHeadBlockEntity`).

## Pure and Minecraft

- Pure, JUnit: `GantryLayout`, `GantryTanks`, `GantryCapacity`, `GantryLook`.
- Minecraft, GameTests: placement and refusals, joining by rail, modes and pipes, buckets, locks, upgrades, drops, fluid index and command.
- Manual: look, GUI, flicker check.
