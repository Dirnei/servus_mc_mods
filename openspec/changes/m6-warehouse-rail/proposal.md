# Proposal

## Why

Today a warehouse grows only by touching blocks: every crate, rack, controller and terminal must be adjacent to another one. That caps the layout at one solid blob and leaves no room for aisles. PLAN.md milestone M6 item "Rails as network connections" makes rails the wiring of the mid game warehouse: any storage block beside a rail is in the network, and the same rails carry shuttles in the next change.

## What Changes

- New block **Warehouse Rail** in `servus_storage`: a flat floor rail, one block wide, placed like a vanilla rail. Rails auto connect to neighbouring rails on the same level and render straight, curve, T or cross pieces. No slopes, no ceiling variant.
- A rail is a warehouse network node. Crates, racks, the controller and the counter terminal that touch a rail on any side join that rail's network, so one controller can serve several aisles.
- A rail holds no items and is never a source: it is not indexed, and plain inventories beside a rail do not join the index. Chests still join only by touching a storage block, as today.
- The rail is craftable from iron ingots and a stick, drops itself, and sits in the Servus creative tab after the counter terminal. Sneak with the Configuration Carrot dismantles it.
- A GameTest proves a crate, two rails and a controller in a line form one online warehouse, and that a chest beside a rail is not indexed.
- The candidate neighbour computation of the warehouse network moves into a pure Java helper with JUnit tests.
- Lib fix found while testing rail removal: when removing a node splits a network, the Lib now hands every node of a split off part over to its new network, through the existing leave and join hooks. Before, the new part had no state at all and the old part kept stale state. This hit every Servus network when a cable or duct was broken in the middle of a line.
- PLAN.md section 5 open decision "Exact rail block design" is settled: floor rails only, auto connecting.

## Capabilities

### New Capabilities
- `storage/warehouse-rail`: what the rail block is, how rail pieces connect to each other, which blocks join a network through a rail, how the rail is obtained.

### Modified Capabilities
- `lib/network-manager`: the merge and split requirement gains the handover of nodes to a split off network.
- `storage/warehouse-controller`: the requirement "Sources are nodes and adjacent inventories" changes. Nodes that hold stock are sources; connector nodes such as rails are not, and their neighbours are not candidates.

## Non-goals

Deferred to later M6 changes:
- Shuttles, their simulation and client rendering. This change only makes rails connect.
- Dispatch station, put-away rules, overflow zones, pick rate stats.
- Ceiling rails, slopes, powered or unpowered rail states.
- Rails as a path for Logistics ducts or Manufacturing machines. Rails join only Servus Storage blocks.

## Impact

- `servus_storage`: new `rail` package with block and block entity, registry entries for block, item and block entity, datagen for the multipart blockstate and models, lang, recipe, loot and tags, one placeholder texture, GameTest, `WarehouseNetwork` membership changes, a new pure helper with a JUnit test.
- `servus_lib`: `NetworkSet` hands nodes over after a split, with a new unit test. The rail itself reuses `ConnectableBlock` and the existing graph with its always connect rule.
- Generated resources under `storage/src/generated/resources` gain the rail files.
- PLAN.md: section 5 loses the rail design open decision and section 2.3 states the decided rail design.
