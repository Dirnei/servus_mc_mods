# Proposal

## Why

Crates and racks are shelves; the controller makes them a warehouse. It knows what is where across every touching storage block and every chest or machine next to them, so a terminal can search and pull. PLAN.md milestone M4, items "Warehouse Controller with adjacency networking" and "Indexing of foreign inventories with batched polling".

## What Changes

- **Warehouse Controller** block: a storage network node. A network with exactly one controller is online. Right click shows a summary in chat (sources, item types, total items, or why the network is offline).
- **Index** on the warehouse network: every storage node and every foreign inventory adjacent to a node is a source. Sources are polled in batches of `sources_per_tick` (stats JSON, default 4) round robin; storage nodes also flag themselves dirty on change so their snapshot refreshes next tick. Adjacent inventories are discovered on node placement and on neighbour changes, never by scanning.
- Network operations for terminals: `count`, `entries`, `extract(key, amount)` across sources, `insert(stack)` with a preference order.
- Server command `/servus_warehouse info <pos>` printing the index of the network at a position, for ops and testing.
- Crafting recipe for the controller. Texture, model, lang.

## Capabilities

### New Capabilities
- `storage/warehouse-controller`: online rule, index, polling, operations, command.

### Modified Capabilities
None.

## Non-goals

- Terminal GUI (next change). Rails, wireless, autocrafting.

## Impact

- `WarehouseNetwork` gets real behavior; crates and racks post dirty and neighbour events.
- `CrateStatsManager` becomes `StorageStatsManager` and also reads `warehouse.json`.
