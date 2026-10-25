# Proposal: m13-fluid-inbound

## Why

PLAN.md M13 Fluid logistics replaces the Fluid Gantry with blocks built like the item side. After the Fluid Rack (`m13-fluid-rack`), the warehouse needs a way to take fluid in. Pumps and pipes fill a Fluid Inbound, and Tankers carry the fluid to Fluid Racks, just as hoppers fill an Inbound Station and shuttles carry the items to Heavy Duty Racks. This replaces the Import gantry.

## What Changes

- **Fluid Inbound station.** A two blocks high steel station with a facing. Its front faces the rail, and it has a teal band.
  - It has a buffer of `fluid_station_tanks` tanks (in `warehouse.json`, example 3) of `fluid_station_tank_capacity` mB each (example 8,000), each tank holding one fluid.
  - Its side configuration is the Foundation Fluids layer (from `m13-fluid-side-modes`): Input, Pull or Off per side, and All or one tank per side. Input on All is the default and the front is fixed.
  - A Pull side takes fluid from the neighbour at the push interval.
- **Joining the warehouse.** The station joins the warehouse only through a rail in front of its lower block. Its copper port sits there, at tanker coupling height.
- **Tanker put-away.** Every buffer tank is a source of tanker put-away runs, with the same rules as an Import gantry tank: at least `tanker_min_load`, or full, and a fluid store with room. The tanker docks its coupling into the port.
- **Lamps and GUI.**
  - A connection lamp and an activity lamp, as on the Inbound Station: green when empty, orange while runs are queued or under way, red when a tank has no room anywhere or no Tanker reaches the station.
  - A GUI with a gauge per tank, the Fluids side panel and a state line that names the waiting fluid and why.
- **By hand and inspect.** Buckets work by hand both ways. The Data Potato prints the tanks.
- **Obtainable.** The station is craftable and keeps its tanks and sides when dismantled with the carrot.
- **Gantry.** Import gantries keep working until `m13-fluid-outbound` removes the gantry.

## Capabilities

### New Capabilities

- `shuttles/fluid-inbound`: the Fluid Inbound block, its buffer, sides, pull, network join, port, lamps, GUI, buckets, inspect and look.

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
- `ShuttlesStatsManager`: `fluid_station_tanks`, `fluid_station_tank_capacity`.
- Wiring: registries, datagen models and blockstates, lang, recipe, loot, docs.
- GameTests: a new `FluidInboundGameTests`, plus updates to `TankerRunGameTests` and `TankerLampGameTests`.

## Non-goals

- The Fluid Outbound and the removal of the Fluid Gantry (`m13-fluid-outbound`).
- Fluid filters per tank or per side.
- Rails on other faces than the front of the lower block.
- A wood choice for fluid stations: they are steel.
