# Proposal

## Why

Without item transport every machine must touch the next one. The plan ships a deliberately simple Item Duct so Manufacturing is playable alone. PLAN.md milestone M2, item "Item Duct and Power Cable (simple), adjacent output push" (the cable landed with the power change).

## What Changes

- **Item Duct**: a connectable block like the cable, built on a new shared `ConnectableBlock` base (six connection flags, shapes, placement updates, sneak wrench dismantle). The power cable is refactored onto the same base.
- Ducts form networks through the Lib network manager. A duct holds no items. Each duct exposes an item handler per side; anything inserted (machine output push, hoppers) is routed at once to the next inventory on the network that accepts it, round robin, never back to the inserter. A per tick item budget per network comes from `data/servus_manufacturing/machine/item_duct.json`.
- Datagen: multipart blockstate and models through the shared connectable generator, loot, tag, lang, and a crafting recipe for eight ducts.

## Capabilities

### New Capabilities
- `manufacturing/item-duct`: how ducts connect, form networks and move items.

### Modified Capabilities
None. Cable behavior is unchanged by the refactor.

## Non-goals

Deferred to later changes:
- Pulling from inventories (ducts only accept what is pushed into them). Extraction comes with Splitter and Filter Ducts in M3 and the Logistics Utility Duct in M5.
- Filters, priorities, per side modes, facades.
- Item rendering inside ducts.

## Impact

- `MachineStats` gains `items_per_tick`.
- New block, block entity type and network type in Manufacturing.
