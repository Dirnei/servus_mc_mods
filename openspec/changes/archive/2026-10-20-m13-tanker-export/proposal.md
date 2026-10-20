# Proposal

## Why

Export gantries hand fluids from the warehouse to pipes and other mods' machines, but after `m13-tanker-import` nothing fills them. A keep level per Export tank, restocked by Tanker shuttles from Storage gantries, completes the fluid loop the same way Outbound Station keep slots do for items. Explored on 2026-10-19 and 2026-10-20.

## What Changes

PLAN.md milestone **M13 Fluid logistics**, item `m13-tanker-export`. Needs `m13-tanker-import`.

- **Keep level** per locked Export tank, set in the gantry GUI (steps of 1 B and 10 B), kept on the dropped item.
- **Restock runs**: when an Export tank's amount plus runs on the way is below its keep level, a tanker brings the fluid from the Storage tank with the most free fluid.
- **Nothing in stock**: no run, red lamp, the GUI names the missing fluid; runs start again when stock appears.
- **Run rules** as for put-away runs: partial picks, leftovers back to storage, aborts.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- `shuttles/fluid-gantry`: keep levels on Export tanks.
- `shuttles/tanker`: export restock runs, source choice, missing stock, restock run rules.

## Non-goals

- Fluid requests from the terminal or work orders delivering fluids.
- Export keep levels on tanks set to Any.
- Restock straight from Import gantries.

## Impact

- Code, Shuttles: `gantry/FluidGantryBlockEntity` (keep levels, saved and on `GantryContents`), `gantry/GantryContents`, `menu/FluidGantryMenu` and `client/FluidGantryScreen` (keep buttons, mark on the gauge), a payload or menu buttons for the keep level, new pure `gantry/FluidRestockPlan`, `network/WarehouseNetwork.tickFluids` (restock), `shuttle/ShuttleJob` (`FluidJob` reused with a restock flag or a sibling record), `shuttle/ShuttleFleet` (leftover back to storage), lang.
- Tests: JUnit for the plan; Shuttles GameTests for restock, runs on the way, missing stock, partial and leftover.
