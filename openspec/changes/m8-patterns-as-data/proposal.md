# Proposal

## Why

During the M8 play test the user found pattern items pointless: patterns never leave their Pattern Storage, so Blank Patterns and encoded Pattern items only add crafting and inventory chores. This follows up PLAN.md M8 item `m8-pattern-storage`: Pattern Storage keeps patterns as data, and the Production Terminal's Patterns tab is the only place to make, edit and delete them.

## What Changes

- Remove the Blank Pattern and Pattern items, their recipes, the crafting grid wipe recipe, models, textures, lang, docs icons and tooltip code. The encoded recipe stays as data.
- Pattern Storage holds up to `patterns_per_block` patterns as data on its block entity. It has no slot GUI; an empty hand right click shows "N of M patterns" in the action bar.
- Saving a new pattern in the terminal editor needs no Blank Pattern; Delete gives nothing back.
- A broken or dismantled Pattern Storage keeps its patterns in its dropped item and restores them when placed again.
- The Pattern Storage recipe no longer uses Blank Patterns.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/patterns`: patterns as data, no pattern items, Pattern Storage without slots, patterns carried by the dropped block, encoding without a Blank Pattern.
- `storage/production-terminal`: the live view scenario and the edit scenario no longer mention pattern items.

## Non-goals

- Moving patterns between Pattern Storages by hand, copying patterns, or sharing them between worlds.
- Any change to how the CPU plans with patterns or how stations run them.

## Impact

- `storage/.../pattern/*` (EncodedPattern stays, PatternItem goes, PatternStorageBlockEntity and PatternEditing change), `menu/PatternStorageMenu`, `client/PatternStorageScreen`, `client/ProductionTerminalScreen`, the item, component, menu and client registries, datagen (recipes, lang, item models, loot), textures.
- GameTests using pattern items (`PatternGameTests`, `StationJobs`).
- Docs Storage page, docs icons, `gamedata.json`.
