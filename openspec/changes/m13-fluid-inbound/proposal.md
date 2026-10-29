# Proposal: m13-fluid-inbound

## Why

PLAN.md M13 Fluid logistics replaces the Fluid Gantry with blocks built like the item side. After the Fluid Rack (`m13-fluid-rack`), the warehouse needs a way to take fluid in. Pumps and pipes fill a Fluid Inbound, and Tankers carry the fluid to Fluid Racks, just as hoppers fill an Inbound Station and shuttles carry the items to Heavy Duty Racks. This replaces the Import gantry.

## What Changes

- **Fluid Inbound station.** A two blocks high station with a facing: an Inbound Station, in its wood, on a glass tank. Its front faces the rail.
  - It has one buffer tank of `fluid_station_tank_capacity` mB (in `warehouse.json`, example 16,000), holding one fluid at a time. The lower half is that tank, glass on all four sides in an iron frame; the upper half is the Inbound Station block with its lamps. There is no port: tankers stop in front of the station, and a hose arm comes later.
  - Its side configuration is the Foundation Fluids layer (from `m13-fluid-side-modes`): Input, Pull or Off per side. With one tank there is no tank choice. Input is the default and the front is fixed.
  - A Pull side takes fluid from the neighbour at the push interval.
- **Joining the warehouse.** The station joins the warehouse only through a rail in front of its lower block, where tankers stop.
- **Tanker put-away.** The buffer tank is a source of tanker put-away runs, with the same rules as an Import gantry tank: at least `tanker_min_load`, or full, and a fluid store with room. The tanker stops in front of the station.
- **Lamps and GUI.**
  - A connection lamp and an activity lamp, as on the Inbound Station: green when empty, orange while runs are queued or under way, red when a tank has no room anywhere or no Tanker reaches the station.
  - A GUI with one gauge, the Fluids side panel and a state line that names the waiting fluid and why.
- **By hand and inspect.** Buckets work by hand both ways. The Data Potato prints the tank.
- **Obtainable.** An Inbound Station on top of a Fluid Tank gives a Fluid Inbound in the station's wood. It keeps its tank, sides and wood when broken or dismantled with the carrot.
- **Gantry.** Import gantries keep working until `m13-fluid-outbound` removes the gantry.

## Capabilities

### New Capabilities

- `shuttles/fluid-inbound`: the Fluid Inbound block, its buffer, sides, pull, network join, lamps, GUI, buckets, inspect, look and recipe.

### Modified Capabilities

- `shuttles/tanker`: Fluid Inbound tanks become sources of fluid put-away runs next to Import gantry tanks, with the same no room and no tanker rules and lamp. Ending a run when the source is removed covers the station. The coupling docks at station ports as at rack ports.

## Impact

- New package `shuttles/.../fluidstation/`:
  - `FluidStationBlock`, a two high block with `HALF`, shared with the Outbound later;
  - `FluidStationBlockEntity` (abstract) and `FluidInboundBlockEntity`;
  - `FluidStationItem`;
  - `FluidInboundMenu`, `FluidInboundScreen`;
  - `FluidStationReport`.
- `NodeKind.FLUID_STATION`, `WarehouseConnectionRule`, `StorageNodeBlockEntity.docksRail`.
- `WarehouseNetwork`:
  - `tickFluids` sources from Inbound tanks;
  - `dockRails` returns the rail in front of the lower block;
  - lamp states;
  - `FluidStoreRef` for station tanks.
- `ShuttleFleet`: coupling at station stops.
- `ServusShuttlesEvents`: the fluid capability per half and face, through `FluidSideHandler`.
- `ShuttlesStatsManager`: `fluid_station_tank_capacity`.
- Wood: `Material.register` for the stations, a wood material recipe, and a small block entity on the upper half that carries the wood for its model.
- Wiring: registries, datagen models and blockstates, lang, recipe, loot, docs.
- GameTests: a new `FluidInboundGameTests`, plus updates to `TankerRunGameTests` and `TankerLampGameTests`.

## Non-goals

- The Fluid Outbound and the removal of the Fluid Gantry (`m13-fluid-outbound`).
- Fluid filters per tank or per side.
- Rails on other faces than the front of the lower block.
