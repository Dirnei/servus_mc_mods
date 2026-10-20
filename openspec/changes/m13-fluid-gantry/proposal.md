# Proposal

## Why

The warehouse knows only items. Fluids from Alchemy builds, pumps and other mods have no home in it, and the Field Head water problem showed that fluids need to move where pipes are a chore. Explored on 2026-10-19 and 2026-10-20: fluids become warehouse stock, kept in a new block with built-in tanks that Tanker shuttles serve over the rails, and other mods plug in through standard fluid capabilities at the edges. A mockup the user approved is at `temp/fluid-gantry-mockup.html`.

## What Changes

PLAN.md milestone **M13 Fluid logistics**, item `m13-fluid-gantry`. Needs `m13-capacity-upgrade`.

- **Fluid Gantry**: one item placed on a straight Warehouse Rail builds a 3 wide, 2 tall frame: a glass tank column on each side of the rail and a steel beam with a spout over it. Breaking any part removes the whole gantry and drops one item that keeps both tanks, locks, mode and upgrades.
- **Joins by its rail**, like a station: the rail between the legs is the gantry's stop for shuttles and the only way it joins a warehouse.
- **Two built-in tanks**, one per leg, one fluid each, capacity from `gantry.json` plus Capacity Upgrades (shared by both tanks; removal refused while the content would not fit).
- **Fluid lock per tank**: a fluid or Any; Any is general storage.
- **Three modes on the same block**: Storage (no pipe access, counted as warehouse fluid stock), Import (pipes fill it), Export (pipes drain it).
- **Buckets by hand** in every mode, following the locks.
- **Fluid stock**: the warehouse indexes the tanks of its Storage gantries per fluid; `/servus_warehouse fluids <pos>` lists them.
- **GUI** with two tank gauges, lock pickers, mode button, upgrade slots and a state line.
- **Look**: tank columns show their fluid, lock plates show the locked fluid, the beam sign shows the mode with the connection and activity lamps.

Decided for this change without the user (open in the explore session, chosen as recommended): the gantry is placed by one item, not formed with the carrot; the upgrade slots grow both tanks together.

## Capabilities

### New Capabilities
- `shuttles/fluid-gantry`: the gantry block, its tanks, locks, modes, GUI, look and the warehouse fluid stock.

### Modified Capabilities
- `shuttles/warehouse-rail`: the gantry's inner leg faces take the rail it stands over.

## Non-goals

- Shuttles moving fluid (Tanker module and import runs: `m13-tanker-import`; export keep levels: `m13-tanker-export`). In this change nothing fills a Storage gantry except buckets by hand.
- The spout animation and fluid stream (come with the tanker runs).
- Fluids in the Storage Terminal, requests, stock rules or work orders.
- Other mods' tanks or Servus Fluid Tanks as warehouse storage (decided against: only built-in tanks are stock).
- A redstone tank sensor.

## Impact

- Code, Shuttles: new package `gantry/` (`FluidGantryBlock`, `FluidGantryItem`, `FluidGantryBlockEntity`, pure `GantryLayout`, `GantryTanks`, `GantryCapacity`), `menu/FluidGantryMenu`, `client/FluidGantryScreen`, `client/FluidGantryRenderer`, payloads for the lock and mode, `network/WarehouseConnectionRule` (new node kind), `network/WarehouseNetwork` (fluid index, event, command data), `network/WarehouseEvent`, `command/` (`fluids`), `registry/*` (block, item, block entity, menu, component), `ServusShuttlesEvents` (block capability), `stats/ShuttlesStatsManager` with new `data/servus_shuttles/shuttles/gantry.json`, datagen (blockstate and part models, item model, lang, recipe, loot), textures by job script.
- Foundation: uses `CapacityUpgradeSlots`, `FluidSlot`, `FluidSprites`, `FluidGauge`, `LegacyFluidHandler`.
- Tests: JUnit for the pure classes; Shuttles GameTests for placement, joining, modes, buckets, locks, upgrades, drops and the fluid index.
