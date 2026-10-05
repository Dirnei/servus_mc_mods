# Design

## Context

Servus Lib has only its empty entrypoint. This change adds the first real code, a pure Java graph. See proposal.md for motivation and the `lib/network-graph` delta spec for the behavior.

## Goals / Non-Goals

**Goals:**
- Small API that the upcoming `NetworkManager` can drive from block load and unload events.
- Incremental updates: an add or remove touches only the affected component, never the whole graph.
- Deterministic results, no randomness, no threads.

**Non-Goals:**
- Persistence, serialization, routing, or any Minecraft type.

## Decisions

### D1: Package and types
`io.github.dirnei.mc.servus.lib.graph` contains:
- `Side` enum: `DOWN, UP, NORTH, SOUTH, WEST, EAST` with offsets, `opposite()`, and a static `ALL` list. Order matches Minecraft `Direction` so the manager can map by ordinal.
- `NodePos(int x, int y, int z)` record with `relative(Side)`.
- `ConnectionRule<N>`: `boolean canConnect(NodePos from, N fromNode, Side side, NodePos to, N toNode)`. The graph calls it twice per pair, once from each side, and requires both to accept.
- `GraphChange(int survivor, List<Integer> created, List<Integer> merged, List<Integer> removed)` record. `survivor` is the component the operated position belongs to afterwards, or `-1` when none. `GraphChange.NONE` for no-ops.
- `NetworkGraph<N>`: the mutable graph.

### D2: Data structures
`Map<NodePos, N> nodes`, `Map<NodePos, Set<NodePos>> edges`, `Map<NodePos, Integer> componentOf`, `Map<Integer, Set<NodePos>> members`, `int nextId`. All `HashMap`/`HashSet`; `NodePos` is a record so hashing is free. Alternative: union find. Rejected because splits need re-partitioning anyway and the explicit member sets make the `GraphChange` cheap to produce.

### D3: Add algorithm
Compute connected neighbours (both-sides rule). Collect their distinct component ids. Zero ids: new component. One id: join. Several: pick the one with the most members as survivor, move the other members' positions over, delete the absorbed sets, report them as `merged`.

### D4: Remove algorithm
Delete the node and its edges. If it was the last member: delete the component, report `removed`. Otherwise run a breadth first search from each former neighbour that is still unassigned, producing one part per search. The largest part keeps the old id; the others get fresh ids and are reported as `created`. Only the affected component is traversed.

### D5: Refresh
`refresh(pos)` re-evaluates the edges of one node after its state changed (a duct side was disabled, for example). Implemented as remove followed by add of the same node value, so it can report a split and merge in one `GraphChange`: the remove result and the add result are combined by concatenating lists and taking the add's survivor.

### D6: Error handling
Adding to an occupied position or removing an empty one throws `IllegalArgumentException`. The manager layer decides whether to tolerate it; the graph stays strict so bugs surface in tests.

## Pure Java vs Minecraft

All of it is pure Java and JUnit tested. Nothing touches Minecraft.

## PLAN.md decisions settled

None. This implements the M1 item as planned.

## Risks / Trade-offs

- [Breadth first search on remove in a large component] -> Only the component that lost a node is traversed, and only from its former neighbours. Fine for networks of a few thousand nodes. Revisit if profiling shows otherwise.
- [Ids are plain ints that restart at 1 per graph instance] -> Acceptable because graphs are rebuilt from loaded blocks and ids are never persisted.
