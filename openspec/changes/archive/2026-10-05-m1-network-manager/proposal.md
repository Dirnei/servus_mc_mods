# Proposal

## Why

The graph from `lib/network-graph` only tracks membership. Networks also need an object per component that owns state, receives events and does work once per server tick, and something has to feed the graph from the world. PLAN.md milestone M1, items "Mailbox and tick scheduler" and "NetworkManager hooked into block place and break events".

## What Changes

- Pure Java core in `io.github.dirnei.mc.servus.lib.network.core`:
  - `Mailbox<E>`: thread safe queue that any thread can post to and that is drained on the server thread.
  - `ServusNetwork<N, E>`: base class for one network (one graph component) with a mailbox, lifecycle hooks and a per tick hook.
  - `NetworkSet<N, E, T>`: owns one `NetworkGraph` and the networks for its components. Applies every `GraphChange` by creating, merging and disposing networks, and ticks them in id order.
- Minecraft layer in `io.github.dirnei.mc.servus.lib.network`:
  - `NetworkType<N, E, T>`: identifies a kind of network (duct, cable, warehouse) with its connection rule and network factory. Content mods register types at mod construction.
  - `NetworkNode<N>`: interface a block entity implements to take part in a network.
  - `NetworkBlockEntity`: abstract block entity base that joins on load and leaves on removal and chunk unload.
  - `NetworkManager`: one per server level. Routes node joins and leaves to the right `NetworkSet` and ticks all sets at the end of every level tick.
  - Event hooks on the NeoForge game bus for level tick and level unload.
- JUnit tests for the pure core, including merge and split of live networks and event ordering.

## Capabilities

### New Capabilities
- `lib/network-manager`: how networks come to exist from node blocks, how they merge, split and disappear, how events reach them, and when they tick.

### Modified Capabilities
None.

## Non-goals

Deferred to later changes:
- Persistence of network state in `SavedData`. Networks rebuild from loaded nodes; stateful networks come with the first feature that needs them (M4 warehouse).
- Any concrete network type, block or block entity. Ducts and cables (M2, M5) are the first users.
- Offloading heavy work to an executor (PLAN.md principle 3). Added when a network needs it.
- Client side networks. Everything here runs on the logical server.

## Impact

- New code in `lib/src/main/java` and `lib/src/test/java`.
- Lib registers two game bus listeners (level tick, level unload). No observable change in game until a mod adds node blocks.
