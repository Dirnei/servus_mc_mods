# Proposal: m13-fluid-outbound

## Why

This change finishes the M13 Fluid logistics rewrite in PLAN.md. After the Fluid Rack and the Fluid Inbound, the warehouse still hands fluid out only through Export gantries. A Fluid Outbound keeps a set of fluids stocked for machines, as the Outbound Station does for items. With it in place, the Fluid Gantry has no job left and goes away. The tanker spec is rewritten around racks and stations. This also closes the PLAN.md open point that an Export gantry no tanker reaches shows no red lamp.

## What Changes

- **Fluid Outbound station:** two blocks high like the Fluid Inbound, with an amber band.
  - The same buffer of `fluid_station_tanks` tanks of `fluid_station_tank_capacity` mB.
  - A keep row: per tank, a fluid and an amount, set in the GUI with a filled container and -1 B, +1 B, -10 B and +10 B buttons. A tank with a keep fluid takes only that fluid.
  - Fluid sides Push, Output or Off with All or one tank. The back defaults to Push on All, the other sides to Off, and the front is fixed.
  - A Push side pushes into the neighbour at the push interval.
  - It joins only through the rail in front of its lower half, where its port is.
- **Restock by tanker:** every `fluid_check_interval` the warehouse restocks each kept tank from Fluid Racks, with the rules of today's Export restock runs. The tanker docks its coupling into the port.
- **Lamps and GUI:**
  - Activity lamp: green when every keep is met, orange while runs are on their way, red when a fluid is missing from storage or no Tanker reaches the station.
  - GUI: keep row, gauges, the Fluids side panel and a state line.
  - Buckets work by hand, and the Data Potato prints the tanks and keeps.
- **BREAKING: the Fluid Gantry is removed.** That covers the block, the item, the recipe, `gantry.json`, its GUI, renderer, textures, lang, GameTests and the spout animation. A gantry placed in an existing world disappears with its fluid. The changelog tells players to empty their gantries before they update.
- **Tanker wording:** its sources are Fluid Inbounds, its stores are Fluid Racks and its restock targets are Fluid Outbounds. "Spout and stream" goes. The tanker's tooltip names the new blocks.
- **Other specs:**
  - Storage fill and the controller's Fluids line count Fluid Racks only.
  - The rail, elevator and traffic specs name the new blocks instead of the gantry.
- **PLAN.md:** M13 is groomed. The gantry text goes, the red lamp open point is closed, and the rest of M13 (Storage Terminal fluids, Field Head water runs, requests and stock rules, tank sensor) stays open.

## Capabilities

### New Capabilities

- `shuttles/fluid-outbound`: the Fluid Outbound block, its keep row, tanks, sides, push, network join, restock lamp, GUI, buckets, inspect and look.

### Modified Capabilities

- `shuttles/tanker`: rewritten without gantries. Sources are Fluid Inbounds, stores are Fluid Racks and restock targets are Fluid Outbounds; "Spout and stream" is removed.
- `shuttles/fluid-gantry`: every requirement is removed.
- `shuttles/storage-fill`: fluid room counts Fluid Racks only. The requirement is renamed.
- `shuttles/warehouse-controller`: the Fluids line counts Fluid Racks only.
- `shuttles/warehouse-rail`: the faces that take rails name the Fluid Rack's port bay and the fluid stations' lower front instead of the gantry legs.
- `shuttles/rail-elevator`: fluid blocks instead of the gantry in the shaft rule.
- `shuttles/shuttle-traffic`: stops at fluid ports instead of gantries.

## Impact

- **New code:** `shuttles/.../fluidstation/`: `FluidOutboundBlock`, `FluidOutboundBlockEntity`, `FluidOutboundMenu`, `FluidOutboundScreen`, `SetFluidKeepPayload`.
- **Deleted:**
  - the whole `shuttles/.../gantry/` package except the pure tanker planners, which move to `shuttles/.../fluid/` with their tests: `FluidRouting`, `FluidPutAwayPlan`, `FluidRestockPlan`, `FluidStoreRef`;
  - `FluidGantryMenu`, `FluidGantryScreen`, `FluidGantryRenderer`, `GantryModels`, `SetGantryLockPayload`, `GANTRY_CONTENTS`, `NodeKind.GANTRY`;
  - `gantry.json`, the gantry textures and generated assets, `tools/gantry-textures/`;
  - `FluidGantryGameTests`, `GantryTankGameTests`, `GantryNetworkGameTests`, `GantryMenuGameTests`, `GantryFixtures` (its rail shape helper moves for `ElevatorGameTests`).
- **Network and fleet:** `WarehouseNetwork` (gantry sets, `readFluidStores`, `tickFluids`, `restockFluids`, host methods `gantryMode`/`gantryPumping`) and `ShuttleFleet` (spout state, `pumpingIntoGantry`).
- **Tanker tests:** `TankerFixtures` and the tanker GameTests are rebuilt on Fluid Inbound, Fluid Rack and Fluid Outbound.
- **Other:** the lang provider, the controller and terminal screens, `ServusShuttlesCommands.fluids`, the docs (`shuttles.md`, `gamedata.json`), `PLAN.md` and `shuttles/CHANGELOG.md`.

## Non-goals

- Converting placed gantries into the new blocks.
- Fluids in the Storage Terminal, requests, stock rules or work orders.
- Water runs to Field Heads.
- A redstone tank sensor.
- A feed command for fluids like `/servus_warehouse feed`.
