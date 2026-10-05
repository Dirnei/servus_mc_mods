# Proposal

## Why

Every Servus network (ducts, warehouses, factory sites) is a graph of blocks that must be kept up to date when blocks are placed or removed, without walking the world. PLAN.md milestone M1, item "Network graph library (pure Java) with split and merge, plus JUnit tests" is the first building block of Servus Lib and everything in M2 onward depends on it.

## What Changes

- New pure Java package `io.github.dirnei.mc.servus.lib.graph` in `servus_lib` with no Minecraft imports.
- `NetworkGraph<N>`: holds nodes at integer positions, derives edges from a pluggable connection rule, and tracks connected components with stable ids.
- Adding a node creates a component, joins one, or merges several. Removing a node shrinks, splits or deletes a component. Each operation returns a change record so the caller can create, merge or dispose network objects.
- Supporting types: `NodePos`, `Side`, `ConnectionRule`, `GraphChange`.
- JUnit tests for all split and merge cases.

## Capabilities

### New Capabilities
- `lib/network-graph`: component tracking for block networks: how nodes join, merge, split and leave, and what the graph reports about it.

### Modified Capabilities
None.

## Non-goals

Deferred to later changes:
- Tick mailbox, scheduler and `NetworkManager` wiring into block events (next M1 change).
- Persistence of graphs. Graphs are rebuilt from loaded blocks.
- Weighted edges, path finding or routing. Later milestones add them on top when a feature needs them.
- Any Minecraft types. Conversion between `BlockPos`/`Direction` and `NodePos`/`Side` lives in the manager change.

## Impact

- New code in `lib/src/main/java` and `lib/src/test/java` only.
- No changes to the build, other mods or the game.
