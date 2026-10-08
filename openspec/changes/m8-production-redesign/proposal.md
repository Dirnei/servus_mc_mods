# Proposal

## Why

In the play test the user said the Production Terminal tabs are unclear, except Patterns, and that the Machines tab "is a weird list of things nobody really cares about": it lists every recipe of every producer (hundreds for a furnace adapter), and the Orders tab shows CPU, order and step internals instead of what a player wants to know (what is being made, how far along, what is stuck). This is a follow up to the PLAN.md M8 item `m8-production-terminal`.

## What Changes

- Tabs become **Crafting**, **Patterns**, **Machines**, Crafting selected on open.
- **Crafting** replaces Orders with a crafting monitor like Refined Storage: one row per job with the item icon and "64 Stick", a progress bar of crafted against wanted, the current step in plain words ("Crafting Station: crafting 2 of 4", "Furnace: smelting 1 of 8", "Waiting: 3 Oak Planks missing", "Waiting for a CPU", "Waiting: Furnace out of fuel"), and a cancel button X. Finished and cancelled jobs fade out a few seconds after they end. CPU slots only show in the tooltip of a one line header ("2 CPUs, 1 of 4 slots").
- **Machines** becomes a status overview: one compact row per producer or upkeep member with icon, name, state (idle, crafting an item, out of fuel, blocked with its reason, no machine, no recipes, upkeep only) and an upkeep bar where present. Problems sort to the top. No recipe lists; a tooltip says how many items it can make, or that it crafts any pattern.
- The payload carries compact rows: no recipe lists, only counts. Instant content on open and the client cache stay.
- The row geometry joins the checked `TerminalLayout`, with the no overlap test extended to the job and machine rows.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/production-terminal`: tab bar, Machines tab, Orders tab (now Crafting tab), cancel from the terminal, live view wording, screen layout.

## Non-goals

- Changing how orders are planned or run.
- Patterns tab behaviour (unchanged).
- A per item wood or recipe preference.
- Hand drawn art for the panel (still painted in code).

## Impact

- `storage/.../production/{ProductionRows,ProductionView,ProductionPayload}` and new pure helpers for job wording and machine ordering.
- `storage/.../menu/ProductionTerminalMenu` (signature), `client/{ProductionTerminalScreen,ClientProduction}`, `terminal/TerminalLayout` and its test.
- Lang keys in the Storage language provider; docs Storage page.
- GameTests in `ProductionTerminalGameTests` and `PatternGameTests` that read the old rows.
