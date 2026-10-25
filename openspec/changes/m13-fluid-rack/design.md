# Design: m13-fluid-rack

## Context

**Heavy Duty Rack** (`shuttles/.../rack/`):
- `HeavyRackBlock` decides joins at placement (`RackRow.joinLeft`/`joinRight`, `joinFromBelow`) with `LEFT`/`RIGHT` state flags. `RackSection.bays` lists a section left to right.
- `HeavyRackBlockEntity` (`StorageNodeBlockEntity`, `NodeKind.RACK`) accepts rails on its front only. It is `standalone()` while `RackDock.closed` finds no front rail in the section.
- Wood lives in `RACK_WOOD` with a `MaterialSkin` model wrapper. The renderer draws the zone with `ZoneLabelText`.
- Shuttles stop at any rail in front of the section (`WarehouseNetwork.dockRails`).

**Fluid side, gantry-only today:**
- `WarehouseNetwork` keeps `fluidIndex` (`WarehouseIndex<TankRef, FluidKey>`), `tankViews` and `readGantries()` for Storage gantries. `TankRef(BlockPos gantry, int tank)` is saved in `FleetSaves` under `"Gantry"`.
- Put-away targets come from `routeFluid` and `FluidRouting.targets(candidates, fluid)`. The rank comes from the gantry lock, via `GantryTanks.Tank`. Restock sources come from `restockSources` and `FluidRestockPlan`.
- `ShuttleFleet.holdForPump` uses `gantryPumpTicks()` and tells the gantry to lower its spout through `host.gantryPumping`.
- `storageFill()` and `ProductionView.storage` count Storage gantries only.

**Zones:** `ZoneLabelItem.useOn` stamps one `StorageNodeBlockEntity` with `zonable()`. `ZoneFilter` holds ghost items and tags, and `WarehouseZones` ranks item put-aways.

## Goals / Non-Goals

**Goals:**
- A Fluid Rack built on the item rack's join, wood and zone code, with one fluid per section and one port.
- One notion of a "fluid store" in the network that covers Storage gantry tanks and rack sections, so routing, restock, stock, fill and the controller treat them alike.
- A tanker coupling for rack stops, in the tanker's look.

**Non-Goals:**
- Fluid stations and removing the gantry (later changes in this stack).
- A rack GUI or Capacity Upgrades.

## Decisions

### Package and block shape

New package `shuttles/.../fluidrack/`.
- `FluidRackBlock` copies the `HeavyRackBlock` placement path: `FACING`, `LEFT`, `RIGHT`, plus a `PORT` boolean set on the port bay. It reuses `RackRow` and `joinFromBelow`. Its `sameBay` check adds "fluid compatible": an empty side, or the same fluid on both.
- `FluidRackBlockEntity extends StorageNodeBlockEntity` with `kind() = NodeKind.FLUID_RACK`. It holds one `FluidSlot` of `capacity_per_bay`, which is the bay's share.
- Wood uses `RACK_WOOD`, `RackWood.pick` and a `MaterialSkin` wrapper over the board texture, as the item rack does. The ladder sides reuse the rack's climb behaviour (the same block method override).

*Alternative:* a subclass of `HeavyRackBlock`. It carries the item GUI, the sample display and the item capability, and none of them apply.

### Section tank: equal shares, pure rules

`FluidRackSection` (pure, no Minecraft imports):
- `portIndex(size)`: size 1 gives 0, 2 gives 0 (left from the front), 3 gives 1.
- `shares(total, bays)`: total / bays each, with the remainder going to the leftmost bays.
- `canJoin(sectionFluid, bayFluid)`.

`FluidRackSectionHandler` (a `LegacyFluidHandler`) wraps the bays of a section as one tank. A fill or drain moves the amount, then rewrites every bay's `FluidSlot` with `shares(...)`. It does this inside the caller's transaction (`Transfer.current()`), so a simulated fill changes nothing. Rebalancing also runs when a bay joins or leaves, in `onPlace` and `affectNeighborsAfterRemoval`. The broken bay's share leaves with its item before the rest rebalances. The handler validates fluids with the zone filter (see Zones).

The section is found with `RackSection.bays(...)` on the new block's flags. It is never scanned per tick.

### Port and network membership

- `acceptsRail(side)`: the front side, and only while the bay is the port bay (`PORT`).
- When `PORT` changes on a section change, the old and new port bays call `refreshLinks()`. The rail at the old port then loses its dock arm.
- `WarehouseConnectionRule`:
  - `FLUID_RACK` is `railOnly` and links to `RAIL` and to `FLUID_RACK` within its section, through `linksTowards`, as `RACK` does;
  - `RAIL` accepts `FLUID_RACK`;
  - add it to `StorageNodeBlockEntity.docksRail`.
- `WarehouseNetwork.onNodeAdded`/`onNodeRemoved` put `FLUID_RACK` nodes in a `fluidRacks` set, not in item `sources`.
- `dockRails(stop)` returns the rail in front of the section's port bay.
- Capability: `Capabilities.Fluid.BLOCK` is registered for the block entity. It returns the section handler while the section is standalone (no rail in front of the port bay, checked like `RackDock.closed` but for the port only), and null otherwise. Placing or breaking that rail calls `invalidateCapabilities` on every bay, as `HeavyRackBlock.refreshDock` does.

### One fluid store reference

`TankRef(BlockPos gantry, int tank)` becomes `FluidStoreRef(BlockPos pos, int tank)`:
- for a gantry tank, `pos` is the gantry master and `tank` is 0 or 1;
- for a rack section, `pos` is the port bay and `tank` is -1.

A section that changes shape posts `WarehouseEvent.FluidChanged` for the old and the new port. The old ref then drops out of the index, and runs to it re-route like a removed target. `FleetSaves` saves the ref under `"Store"`. Nothing is released, so there is no migration. Fleets saved under the old `"Gantry"` key lose their fluid job, which only affects test worlds.

`TankView` gets a `kind` (GANTRY or RACK) and a `preferred` predicate, so routing reads only views:
- a gantry tank is preferred for a fluid when it is locked to it;
- a rack is preferred when its zone's fluid filter lists the fluid.

The accepts check is "the lock or the zone allows it". `readGantries()` becomes `readFluidStores()`. It reads pending gantries and pending rack sections, and only Storage gantries and racks enter `fluidIndex`.

`FluidRouting.Candidate` replaces its `GantryTanks.Tank` lock with `(preferred, holds, accepts, room, cost)`, keeping the rank order:
1. preferred with room;
2. holding with room;
3. empty and accepting.

`FluidRestockPlan.sources` takes the same views. `GantryTanks` stays for the gantry's own lock rules until the gantry goes.

### Tanker stop and coupling

- `holdForPump` reads `tankerPumpTicks()` from `warehouse.json` (`tanker_pump_ticks`, example 40). `pump_ticks` and `gantryPumpTicks` leave `gantry.json`.
- At a gantry the spout behaviour stays (`host.gantryPumping`).
- At a rack the fleet sets `shuttle.coupling = (side toward the rack, intoRack, fluid)`. `ShuttleView` carries it, and its start and end are synced the way the sight glass already is.
  - `ShuttleGarageRenderer` eases the coupling out over `TankerLook.COUPLING_TICKS` (example 10) toward the side, holds it while the stop lasts, and eases it back when the view clears.
  - The side is the direction from the rail to the rack's port bay, so racks on both sides of a rail work.
- `TankerLook` gains the coupling boxes:
  - a stowed tube flush in each tank side and an extended tube to the block edge, inset 0.05 px from the tank faces;
  - a copper flange at its end.
- `ShuttleModels.tanker()` gains the stowed coupling recesses.
- The rack's port flange sits at `TankerLook` coupling height, so the extended tube ends exactly at the rack's front plane minus 0.005 px.

### Zones

- `ZoneLabelItem.useOn` on a `FluidRackBlockEntity` stamps or clears every bay of its section. A bay that joins a zoned section copies the zone in `onPlace`.
- `ZoneFilter.fluids(level)` returns the fluids of the ghost stacks that are filled containers. It reads `Capabilities.Fluid.ITEM` on a copy of each stack, the same way `Transfer.isFluidContainer` reaches item fluid content.
- The section handler's validator is `fluids.isEmpty() || fluids.contains(fluid)`. Item tags are ignored on fluid racks.
- The zone name is drawn by `FluidRackRenderer` on the port bay's top beam with `ZoneLabelText`.

### Look and renderer

**Datagen** (`FluidRackModels`, modelled on `HeavyRackModels`):
- iron posts at section ends;
- top and bottom beams that end exactly at the post faces;
- boards on the back and floor with the wood skin;
- a glass front pane inset 0.05 px from the beams;
- a port flange model part when `PORT` is true.

**`FluidRackRenderer`:**
- draws the section's fluid behind the glass with the fluid's still texture and tint, to the share in `level_steps` (example 16). It reuses the column drawing of `FluidGantryRenderer` (moved to a shared helper `FluidColumn` before the gantry goes);
- draws the zone name.

The level reaches clients in the update tag (`"Level"`, `"Fluid"`) only when the step changes.

### Data

- New `data/servus_shuttles/shuttles/fluid_rack.json`: `capacity_per_bay` 16000, `bays_per_section` 3 and `level_steps` 16. It is read by `ShuttlesStatsManager` like `rack.json`.
- `warehouse.json` gains `tanker_pump_ticks` 40.
- The recipe is `FluidRackRecipe extends ShapedRecipe` with the wood pick of `HeavyRackRecipe` (example: iron ingots, glass, planks, a copper ingot; yields 2).

### Storage fill and controller

- `storageFill()` adds a `StorageFill.Tank` per rack section.
- `StorageRow` carries `gantries` and `fluidRacks` counts.
- The controller and Production Terminal tooltips name both counts. `storage.no_gantry` becomes `storage.no_fluid_storage`.

## PLAN.md decisions settled

This settles the M13 fluid storage layout:
- one fluid per section;
- one port per section, on the centre or left bay;
- zone label fluid filters instead of tank locks;
- the tanker carries the moving coupling, and racks have no moving parts.

## NeoForge APIs

- `RegisterCapabilitiesEvent.registerBlockEntity(Capabilities.Fluid.BLOCK, ...)`, as `ServusShuttlesEvents` already does for the Field Head.
- `Capabilities.Fluid.ITEM` for container contents.
- `FluidUtil.interactWithFluidHandler`, as `FluidGantryBlock` uses it, for buckets.
- `Level.invalidateCapabilities(BlockPos)`.
- Client: `BlockEntityRenderer`, as `FluidGantryRenderer` uses, and `IClientFluidTypeExtensions` for still texture and tint.

## Pure and Minecraft parts

- **Pure, with JUnit:**
  - `FluidRackSection`: port, shares, join;
  - the extended `FluidRouting` ranks with preferred, holding and empty candidates mixing gantry tanks and racks;
  - `FluidRestockPlan` with rack sources.
- **Minecraft, with GameTests in `FluidRackGameTests` and updates to the tanker tests:**
  - sections, port, membership, pipe access, buckets, stock, command, zones;
  - tanker put-away to a rack, restock from a rack, re-route when the target rack breaks;
  - the coupling flag on the shuttle view.
- **Manual play check:** the glass level, the coupling animation from several angles, the zone name, the port flange, and flicker.

## Risks / Trade-offs

- [Rebalancing on every fill touches every bay of the section] At most three `FluidSlot` writes per transfer, on server events only.
- [Port moves when a section changes] Runs to the old port re-route like a removed target. The tests cover breaking the port bay during a run.
- [Renaming `TankRef` breaks saved fleets with fluid jobs] Nothing is released, so test worlds only.
