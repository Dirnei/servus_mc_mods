# Design

## Context

`WarehouseNetwork` exists as an empty membership network. Crates and racks are nodes with item handlers. Lib networks have a mailbox (`post` and `handle`), `members()`, `onNodeAdded`, `onNodeRemoved`, `onAbsorb`, `onTick`. See proposal.md.

APIs verified: `BlockCapabilityCache.create(cap, level, pos, context)`, `BlockBehaviour.neighborChanged`, `ItemStack.getComponentsPatch`, `RegisterCommandsEvent.getDispatcher`, `BlockPosArgument.getLoadedBlockPos`.

## Decisions

### D1: Pure index
`WarehouseIndex<S, K>` in `storage/index`: `update(source, snapshot)` replaces that source's contribution incrementally, `remove(source)`, `count(key)`, `entries()` sorted by count descending, `sourcesHolding(key)`, `snapshot(source)`. No Minecraft imports; JUnit tested. Keys in Minecraft are `ItemKey(Item, DataComponentPatch)` records built from a stack.

### D2: Sources and polling
`WarehouseNetwork` keeps `Map<BlockPos, Source>`; a `Source` holds a `BlockCapabilityCache<IItemHandler, Direction>` (side null) and whether it is a node. On `onNodeAdded` the node position becomes a node source and its six neighbours become candidate sources unless already known. `handle(NeighborsChanged pos)` adds missing candidates around that node and marks the existing ones dirty; `handle(Dirty pos)` queues that source for the next poll. `onTick` polls the dirty queue first, then round robin, up to `sources_per_tick` reads; a poll reads the handler and calls `index.update`; a candidate without handler is dropped. `onNodeRemoved` removes the node source and rebuilds candidates from the remaining members. `onAbsorb` copies the other network's sources and snapshots.

### D3: Online rule
Controller count is tracked in `onNodeAdded`, `onNodeRemoved` and `onAbsorb`. `status()` returns ONLINE, NO_CONTROLLER or MULTIPLE_CONTROLLERS. Operations work regardless; terminals and the command report the status.

### D4: Operations
`extract(key, amount)`: iterate `sourcesHolding(key)`, extract matching stacks slot by slot until satisfied or a full stack is reached, mark those sources dirty. `insert(stack)`: holders of the key, then empty crates, then other node sources, then foreign sources whose snapshot is not empty (so item ducts and unused machines are not fed by accident); mark dirty.

### D5: Controller block and command
`WarehouseControllerBlock` (horizontal facing, right click sends the summary with `displayClientMessage`). `ServusStorageCommands` registers `/servus_warehouse info <pos>` on `RegisterCommandsEvent`, permission level 2.

### D6: Nodes report
`StorageNodeBlockEntity` gains `network()`, `postDirty()` and a static `neighborsChanged(level, pos)`. Crate and rack call `postDirty()` from their change hooks; the three storage blocks override `neighborChanged` and call the static helper.

## Pure Java vs Minecraft

`WarehouseIndex` pure with JUnit tests. Verified over RCON with the info command: crate plus chest plus rack indexed; chest removal drops its items; two controllers report offline.

## Risks / Trade-offs

- [Polling foreign inventories costs per tick] -> Capped by the batch size; snapshots only rebuild for polled sources.
- [Capability caches for six neighbours per node] -> Small objects; dropped when nodes leave.
