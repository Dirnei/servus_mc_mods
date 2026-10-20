# Proposal

## Why

With `m13-fluid-gantry` the warehouse has fluid storage, but nothing moves fluid into it except buckets. Tanker shuttles close the loop: producers pipe into an Import gantry, tankers carry the fluid over the rails into Storage gantries with room, and no run starts when there is no room. Explored on 2026-10-19 and 2026-10-20; mockup at `temp/fluid-gantry-mockup.html`.

## What Changes

PLAN.md milestone **M13 Fluid logistics**, item `m13-tanker-import`. Needs `m13-fluid-gantry`.

- **Tanker module**: a tool for the Shuttle Workshop. A Tanker shuttle takes only tanker runs, rides lifts (not wide) and carries one fluid up to its tier's `tanker_capacity`.
- **Fluid put-away runs**: Import gantry tanks with enough free fluid get a tanker run to a Storage gantry tank chosen by lock (locked to the fluid, Any holding it, empty Any), with room counting runs on the way.
- **No room, no run**: the fluid waits in the Import gantry, its lamp turns red and its GUI names the fluid.
- **Run rules**: partial picks, leftovers to the next tank or back to the source, aborted runs, recall.
- **Spout and stream**: the gantry spout lowers onto a stopped tanker and a fluid stream runs; lamps show working and idle.
- **Tanker look**: a steel tank with bands, copper filler cap and sight glasses, inside the rail block.

## Capabilities

### New Capabilities
- `shuttles/tanker`: the Tanker module, fluid put-away runs, target choice, no room rule, run rules, spout animation and look.

### Modified Capabilities
- none (the gantry's lamps and spout behaviour for runs are stated in `shuttles/tanker`).

## Non-goals

- Export gantries restocked by tankers with keep levels (`m13-tanker-export`).
- Water runs to Field Heads, fluids in the Storage Terminal, requests or work orders.
- Tankers taking fluid from Storage gantries for anything but leftovers.
- Moving fluid between sites.

## Impact

- Code, Shuttles: `shuttle/module/ModuleSpec` (`TANKER`), `shuttle/module/JobNeeds` (`TANKER`), registration of the Tanker item, `shuttle/module/ShuttleTiers` and `stats/ShuttleStatsReader` (`tanker_capacity`), `shuttle/ShuttleJob` (new `FluidJob`), `shuttle/Shuttle` (fluid cargo on the load), `shuttle/ShuttleFleet` (assignment, legs, stops, fluid sums, `Host` methods), `network/WarehouseNetwork` (`tickFluids`, host methods, lamps), new pure `gantry/FluidRouting` and `gantry/FluidPutAwayPlan`, `gantry/FluidGantryBlockEntity` (pumping state, activity, state line), `client/FluidGantryRenderer` (spout and stream), `datagen/ShuttleModels` (tool model), shuttle client rendering of the sight glass, `shuttle.json`, `warehouse.json`, `gantry.json`, recipe and lang.
- Tests: JUnit for the pure planners; Shuttles GameTests for runs, targets, no room, partial and aborted runs, lifts.
