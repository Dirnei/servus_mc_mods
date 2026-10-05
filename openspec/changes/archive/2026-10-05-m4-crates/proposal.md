# Proposal

## Why

Storage starts with the crate: bulk storage of one item type, visible on the block face, no power. It is the first content of `servus_storage` and lays down the mod's registry, datagen and network node foundation. PLAN.md milestone M4, item "Crates with front item display".

## What Changes

- Storage mod plumbing: registries, creative tab hook, datagen providers, config free for now.
- **Crate**: holds one item type up to a capacity from `data/servus_storage/storage/crate.json` (default 4096). Right click with items inserts the held stack, a quick second right click inserts every matching stack from the inventory. Left click takes a stack, sneak left click takes one. Sneak left click on an empty crate, or any left click while empty, breaks it normally. Exposes the item handler capability so hoppers, ducts and machines use it.
- Front display: a block entity renderer draws the stored item and its count on the front face.
- Wrench rotates; sneak wrench dismantles keeping the contents on the item. Breaking a crate spills its contents.
- Warehouse network foundation: a `WarehouseNetwork` type registered with Lib and a `StorageNodeBlockEntity` base; crates are nodes, so the controller change only adds behavior.

## Capabilities

### New Capabilities
- `storage/crate`: what a crate holds, how players and automation use it, what it shows.

### Modified Capabilities
None.

## Non-goals

- Locking, upgrades, void mode.
- Controller, racks and terminal (next changes).

## Impact

- `servus_storage` gets real content; the GameTest run config namespace covers it later.
- Lib unchanged.
