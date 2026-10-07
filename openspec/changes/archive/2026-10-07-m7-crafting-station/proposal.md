# Proposal

## Why

Autocrafting should work for a player who only installed Servus: Storage, and the work order changes need a producer that Storage's own GameTests can drive. This change implements the M7 item "Storage only Crafting Station fallback" from `PLAN.md`, ahead of the work orders so that every later change can be verified against it.

## What Changes

- **Crafting Station block.** A Storage block that crafts vanilla crafting recipes without power. The player configures up to `crafting_station_recipes` recipes (example 4) by laying out a pattern in a ghost grid and saving it; configured recipes show as cells and can be removed. It has nine input slots and four output slots, crafts any configured recipe whose items are all present, taking `work_ticks` from `data/servus_storage/storage/crafting_station.json` (example 40) per craft, and pushes finished items into adjacent inventories like a Manufacturing machine does.
- **Behaves like a machine towards the warehouse.** Its item handler accepts into the inputs only items used by a configured recipe and lets automation extract outputs only, so a Machine Dock facing it feeds it and collects from it. It exposes the Lib production capability with its configured recipes, so work orders can use it.
- **GUI.** Pattern grid with a Save button, recipe cells with click to remove, input and output slots, player inventory.

## Capabilities

### New Capabilities
- `storage/crafting-station`: the block, recipe configuration, crafting, item handler sides, production capability, GUI, data and crafting.

### Modified Capabilities
- None.

## Non-goals

Deferred to later changes:
- Power, speed upgrades, smelting or other recipe types.
- Research gating. The station is a vanilla fallback.
- A recipe viewer transfer button; recipes are laid out by hand in the grid.
- The Crafting Station being a warehouse node itself; it is served through a Machine Dock like any machine.

## Impact

- `storage` only: new block, block entity, menu, screen, payload for the editor buttons, datagen and textures; `ServusStorageEvents` registers the item handler and production capabilities; `StorageStatsManager` reads `crafting_station.json`.
- The pure Lib helpers `IngredientGatherer` and `RoundRobin` are reused.
- `PLAN.md` needs no change.
