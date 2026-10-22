# Proposal

## Why

Moving a Warehouse Controller today throws its FE away, and mining it with a pickaxe also loses the site name (only the Configuration Carrot keeps the name). Players who move a controller to a better spot lose a full buffer and have to retype the name. This belongs to PLAN.md M11 Wireless and scale (controllers as sites, warehouse power); it was decided with the user on 2026-10-22 as a follow up of `m11-warehouse-power`.

## What Changes

- A mined or carrot dismantled controller item keeps the site name and the FE stored.
- Placing that item again restores both; the FE is capped at the controller capacity (`controller_energy_capacity` in `shuttles/power.json`).
- The controller item's tooltip shows the site name and the FE it carries.
- The block loot of the controller copies the two new components next to the wood.
- Stock rules keep travelling only through the carrot dismantle, as before.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `shuttles/warehouse-controller`: "Controller is a named site" (the name also travels in a mined item), "Controller FE buffer" (the FE travels in a dropped item, capped on placement), and a new requirement for the item tooltip.

## Impact

- `shuttles`: `ServusShuttlesComponents` (two new components), `WarehouseControllerBlockEntity` (implicit components), `WarehouseControllerBlock` (no FE clearing on dismantle or placement), a new `WarehouseControllerItem` for the tooltip, `ServusShuttlesItems`, block loot datagen and lang, GameTests in `ControllerSiteGameTests` and `ControllerWirelessGameTests`.
- Generated loot table and lang, `docs/src/data/gamedata.json`.
- No change to the site registry: a placed controller makes its own record as before.

## Non-goals

- Stock rules in a mined controller item (still carrot only).
- Carrying Anchor or Wireless Upgrades, the team or the chunk span in the item.
- Any FE output from the controller or from its item.
