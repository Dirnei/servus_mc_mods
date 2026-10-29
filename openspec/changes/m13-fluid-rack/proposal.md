# Proposal: m13-fluid-rack

## Why

Players found the Fluid Gantry hard to understand: one frame with two tanks, three modes, locks, keep levels and its own rail rules. PLAN.md M13 Fluid logistics replaces it with the item pattern players already know. Fluid Racks store, a Fluid Inbound takes fluid in and a Fluid Outbound hands it out. This change builds the storage part: a Fluid Rack that looks and joins like the Heavy Duty Rack. It is zoned by the Zone Label instead of a GUI lock, and the Tanker docks itself onto one port per section.

## What Changes

- **Fluid Rack block:**
  - Bays join into sections of up to three, side by side, like the Heavy Duty Rack. Joins are decided at placement, stacked rows copy the row below, and racks of different woods do not join.
  - Each section holds one fluid in one shared tank of `capacity_per_bay` per bay (in the new `data/<ns>/shuttles/fluid_rack.json`, example 16,000 mB). A glass front shows the fluid level.
  - It has the blackened iron frame, the boards in the crafting wood and the ladder sides of the item rack.
- **No GUI.** A bucket by hand fills or empties the section. The Data Potato prints a report.
- **One port per section:** on the centre bay of three, on the left bay of two (seen from the front), and on the only bay of one. A section joins the warehouse only through a rail in front of its port bay, and rails dock onto that bay's front only. Every bay's front holds up a hung rail, so rails run along the whole section.
- **Rail overhang:** one free rail may hang one block past a hung rail, so a rail run along a rack front reaches one past its end.
- **Pipe access:** a section with no rail in front of its port is a plain tank for pipes on every side of every bay. A docked section is reached only by Tankers.
- **Zone Label:**
  - Stamping one bay zones the whole section and shows the zone name on its top beam.
  - A filled fluid container in a ghost slot of the label means that fluid on a Fluid Rack, so a zoned section takes only the fluids its label lists.
- **Fluid stock:** Fluid Rack sections become warehouse fluid stock next to Storage gantries. They count in `/servus_warehouse fluids`, in the storage fill and in the controller's Fluids line.
- **Tanker:**
  - It puts fluid away into Fluid Racks and restocks from them, with zoned sections that list the fluid ranked like locked tanks.
  - It stops at the rail in front of a section's port and slides a telescopic coupling out of its tank's side into the port while it pumps.
- **Stop time:** the tanker stop time moves from `gantry.json` `pump_ticks` to `warehouse.json` `tanker_pump_ticks`, used at every tanker stop.

## Capabilities

### New Capabilities

- `shuttles/fluid-rack`: the Fluid Rack block, its sections, port, look, pipe access, buckets, inspect and data.

### Modified Capabilities

- `shuttles/zones`: stamping a Fluid Rack zones its section, a label's fluid containers become a fluid filter on Fluid Racks, and the zone name shows on the rack's top beam.
- `shuttles/tanker`:
  - Fluid Racks become targets of put-aways and sources of restock runs, with zoned racks ranked like locked tanks.
  - The tanker stops at a rack's port and docks its coupling there.
  - The coupling becomes part of the tanker's look.
  - The stop time moves to `warehouse.json`.
- `shuttles/storage-fill`: fluid room counts Fluid Rack sections.
- `shuttles/warehouse-controller`: the Fluids line and its tooltip count Fluid Racks.
- `shuttles/warehouse-rail`: a single free rail holds with one hung rail beside it, an overhang of one.

## Impact

- **New package `shuttles/.../fluidrack/`:** `FluidRackBlock`, `FluidRackBlockEntity`, `FluidRackItem`, `FluidRackRecipe` (wood memory), `FluidRackSection` (pure share and port rules), `FluidRackReport`, plus the renderer and datagen models.
- **`WarehouseNetwork`:**
  - `NodeKind.FLUID_RACK`, `dockRails` for the port bay, the fluid index and tank views generalised from `TankRef(gantry, tank)` to a fluid store reference covering gantry tanks and rack sections;
  - `routeFluid` and `restockSources` candidates, `storageFill`.
- **`ShuttleFleet`:** `holdForPump` stop time from `warehouse.json`, coupling start and end sent to the rack's renderer. `FleetSaves` stores the new reference.
- **Tanker look and renderer:** `TankerLook`, `ShuttleModels.tanker()`, `ShuttleGarageRenderer`.
- **Zones:** `ZoneLabelItem.useOn` (section stamp), `ZoneFilter` (fluid view), `WarehouseZones` (fluid tiers).
- **Wiring:** registries, `ServusShuttlesEvents` (fluid capability), `ShuttlesStatsManager` (`fluid_rack.json`, `tanker_pump_ticks`), the lang provider, recipe and loot providers, and `docs/src/content/docs/mods/shuttles.md`.
- **Tests:** GameTests in a new `FluidRackGameTests`, plus updates to `TankerRunGameTests`, `TankerRestockGameTests` and `StorageFillGameTests`. JUnit for `FluidRackSection` and the extended `FluidRouting`.

## Non-goals

- Fluid Inbound (`m13-fluid-inbound`) and Fluid Outbound (`m13-fluid-outbound`). Until they land, Import and Export gantries stay the tanker's source and destination docks.
- Removing the Fluid Gantry. That happens in `m13-fluid-outbound`.
- Fluids in the Storage Terminal, in requests, stock rules or work orders (still open in PLAN.md).
- Capacity Upgrades for Fluid Racks.
- Fluid tags in Zone Labels.
