# Proposal

## Why

PLAN.md M8 item 7 (`m8-pattern-storage`): the Crafting CPU should know crafting recipes from patterns the player encodes once, not from each Crafting Station's own list. Patterns hold concrete items, so the planner never chooses between recipes or tag variants, and Pattern Storage on the data line makes the number of known recipes grow by placing blocks, like crates and racks do for items.

## What Changes

- New items **Blank Pattern** (cheap, craftable) and **Pattern** (encoded: a vanilla crafting recipe id, a 3x3 template of concrete items and the result, shown in its tooltip). A Pattern crafts back into a Blank Pattern.
- New block **Pattern Storage** (Storage): a link member of the data line that holds `patterns_per_block` encoded Patterns (datapack JSON, example 9) in a slot GUI; more blocks hold more patterns. No tiers.
- The **Production Terminal** gets a third tab, **Patterns**: every pattern on its data line with result, inputs and its Pattern Storage, a capacity bar (used of total slots), a plus button that opens the recipe editor (ghost grid, preview, Save, Cancel) with JEI transfer, and clicking a pattern opens the editor to change or delete it. Save encodes a Blank Pattern from the player's inventory and stores it in the first Pattern Storage of the line with room; Delete gives the Blank Pattern back.
- The **Crafting CPU** plans with the patterns of its data line in addition to the recipes producers report. A pattern step runs on a producer that can execute that pattern; until `m8-station-hands` that is a Crafting Station whose own recipe list holds the same recipe with the same items.
- PLAN.md section 5: the Pattern Storage tier question is settled (more blocks, no tiers).

## Capabilities

### New Capabilities
- `storage/patterns`: Blank Pattern and Pattern items, encoding, Pattern Storage, capacity, how the CPU uses patterns.

### Modified Capabilities
- `storage/production-terminal`: the tab bar gains the Patterns tab; live view includes patterns and capacity; the editor in the terminal.
- `storage/crafting-cpu`: steps can come from patterns, run on a producer that can execute the pattern.

## Non-goals

- Crafting Stations executing any pattern without their own recipe list: `m8-station-hands`.
- Patterns for machine recipes (smelting, assembling): machines report their own recipes; adapters come in `m8-machine-adapter`.
- Pattern Storage tiers, item handler access to Pattern Storage for hoppers or ducts, wireless pattern access.

## Impact

- `storage`: new `pattern` package (items, component, block, block entity, menu, screen), Production Terminal menu, screen, payload and view, CPU host recipe list, JEI plugin, payload registration, registries, datagen (models, loot, recipes, lang, tags), `storage/pattern_storage.json`.
- Docs: Storage page and `gamedata.json`.
- PLAN.md section 5.
