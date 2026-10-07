# Design

## Context

See proposal.md for motivation. `InboundStationBlockEntity` (kind `STATION`) owns a buffer with per slot claims, an insert only `intake` handler, the blocked reason and the activity lamp; `WarehouseNetwork` keeps `stations`, `pendingStations` and `blockedStations`, drains buffers in `drain(pos, station)` and carries stacks by `PutAwayJob`. Pick jobs carry `terminal` as their destination and `deliverToTerminal` only knows `CounterTerminalBlockEntity.output()`. `request(terminal, key, amount)` plans with `RequestPlan` and creates pick jobs. Machines push their outputs every 8 ticks into any adjacent item handler whose side allows it, and accept inputs only for configured recipes. `ServusLibCapabilities.PRODUCTION` is a sided block capability.

## Goals / Non-Goals

**Goals:**
- Reuse the station machinery: a dock is an Inbound Station with a second buffer and a facing.
- One delivery concept: pick jobs deliver to any block with a delivery buffer.
- Producer discovery stays a capability lookup, no Manufacturing types in Storage.

**Non-Goals:**
- Deciding what to feed; see proposal.

## Decisions

### Settles no PLAN.md open decision

### Dock extends the station
`MachineDockBlockEntity extends InboundStationBlockEntity` (a protected constructor taking the block entity type is added to the station). The inherited buffer is the collect buffer and the inherited `intake` is the capability on every side, so machine pushes, hoppers and ducts land there and the network's drain, claims, blocked reason and activity lamp work unchanged. The dock adds `ItemStackHandler feed` (same slot count) and `serverTick` that every `dock_feed_interval` ticks pushes each feed slot into `level.getCapability(Capabilities.ItemHandler.BLOCK, facingPos, facing.getOpposite())` with `ItemHandlerHelper.insertItem`. `acceptsRail(side)` is false for the facing side. `producer()` returns `level.getCapability(ServusLibCapabilities.PRODUCTION, facingPos, facing.getOpposite())`, looked up on demand (cheap, only on commands and future order planning). `kind()` stays `STATION`, so the connection rule, `docksRail()` and candidate skipping need no change.

### Deliveries to any buffer
`DeliveryTarget` is a small Storage interface `{ ItemStackHandler deliveries(); }` implemented by `CounterTerminalBlockEntity` (returns `output()`) and `MachineDockBlockEntity` (returns `feed()`). `WarehouseNetwork.deliverToTerminal` becomes `deliverTo(job, stack)`: it finds the `DeliveryTarget` at `job.terminal()`, inserts, and updates request bookkeeping only when the target is a terminal with that request id. `terminals` tracking stays for requests. `PickJob.terminal` keeps its name; it is the destination position.

### Feeding a dock
`feedDock(MachineDockBlockEntity dock, ItemKey key, int amount)` reuses the planning half of `request`: it builds the same `RequestPlan.Stock` list, plans with the dock's reachability, and makes pick jobs with request id 0 to the dock's position; it returns the same refusal keys. Held instant loads use the existing `held` map keyed by destination position. The `request` method is refactored so both call one private `planAndStart(destination, key, amount, requestId)`.

### Producers
`WarehouseNetwork.producers()` returns `List<Producer>` with `record Producer(BlockPos dock, BlockPos machine, Component machineName, List<ProductionRecipe> recipes, Optional<ResourceLocation> active)`, built from the docks among `stations` whose `producer()` is present. The machine name is the block's name. Commands `producers` and `feed` sit next to `zones` and `take` in `ServusStorageCommands`.

### GUI
`MachineDockMenu` has the feed row at y 18 and the collect row at y 40, the player inventory at y 84; `quickMoveStack` from the inventory goes into the feed buffer, from either row back to the inventory. The screen reuses `StationStatePayload` through the station menu's state logic (the menu extends `InboundStationMenu`'s sync by composition: a small shared helper `StationState.of(station)` moves out of `InboundStationMenu`). Labels "To machine" and "From machine".

### Datagen and textures
Block state through the existing `station()` helper (dark, idle, working, blocked fronts) with name `machine_dock`; textures drawn with the placeholder generator showing a conveyor and the two lamps; GUI texture 176 by 166 with two slot rows; loot, pickaxe tag, recipe (hopper and chest over a rail, framed by iron ingots), lang.

### Minecraft side verification
Storage GameTests with vanilla blocks, since Storage tests load without Manufacturing: `dockFeedsTheBlockItFaces` (dock facing a chest, 8 ingots in the feed buffer end up in the chest), `dockCollectsAndPutsAway` (a hopper above the dock drops bread in, the bread ends in a crate), `dockFacingAChestIsNoProducer` (`producers()` reports the dock without recipes), `feedDockFromCrate` (feed command path: 4 ingots arrive in the feed buffer), `feedDockByShuttle` (ingots in a rack on the rail, garage with a shuttle, feed arrives by shuttle), `dockFrontTakesNoRail`. The Assembler path is checked by hand in the client with all four mods, and the `producers` command.

## Risks / Trade-offs

- [Dock accepts anything into its collect buffer] → that is what an Inbound Station does too; zone routing sorts it.
- [Feed buffer stacks the machine never accepts stay forever] → the GUI shows them and the player can take them out; work orders will only feed recipe inputs.
- [Capability lookups per feed interval] → one `getCapability` per dock every 8 ticks, cached by position in a `BlockCapabilityCache` later if profiling asks for it.

## Open Questions

None.
