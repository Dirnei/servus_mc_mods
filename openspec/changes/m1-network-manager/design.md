# Design

## Context

`lib/network-graph` exists and is tested. Nothing in Lib touches the world yet. See proposal.md for motivation and the `lib/network-manager` delta spec for behavior.

APIs verified in the NeoForge 21.1.255 sources:
- `net.neoforged.neoforge.event.tick.LevelTickEvent.Post` with `getLevel()`.
- `net.neoforged.neoforge.event.level.LevelEvent.Unload` with `getLevel()`.
- `IBlockEntityExtension.onLoad()` and `onChunkUnloaded()` (NeoForge patches on `BlockEntity`), plus vanilla `setRemoved()`.
- `net.neoforged.fml.common.EventBusSubscriber(modid = ...)` for game bus listeners.

## Goals / Non-Goals

**Goals:**
- Pure core that is fully unit tested; the Minecraft layer is thin glue.
- Content mods only implement a `NetworkType` and a block entity; they never touch the graph.
- Deterministic tick order and single threaded network code.

**Non-Goals:**
- Persistence, executors, client side mirrors.

## Decisions

### D1: Package split
`io.github.dirnei.mc.servus.lib.network.core` is pure Java: `Mailbox`, `ServusNetwork`, `NetworkSet`, `NetworkFactory`. `io.github.dirnei.mc.servus.lib.network` has the Minecraft glue: `NetworkType`, `NetworkNode`, `NetworkBlockEntity`, `NetworkManager`, `NetworkEvents`, `Conversions`.

### D2: Mailbox
`ConcurrentLinkedQueue<E>`. `post(E)` from any thread. `drain(Consumer<E>)` takes a snapshot bound of the current size and handles exactly that many, so events posted during handling wait for the next tick (spec scenario "Event posted during handling").

### D3: ServusNetwork<N, E>
Fields: `int id`, `NetworkSet<N, E, ?> owner`, `Mailbox<E> mailbox`. Hooks with empty defaults: `onNodeAdded(NodePos, N)`, `onNodeRemoved(NodePos, N)`, `onAbsorb(ServusNetwork other)` (called on the survivor with the network being merged away, before it is disposed), `onDisposed()`, `onTick()`. Abstract `handle(E event)`. `members()` delegates to the owner's graph. `post(E)` is public.

### D4: NetworkSet<N, E, T extends ServusNetwork<N, E>>
Owns `NetworkGraph<N>` and `TreeMap<Integer, T>`. `nodeAdded(pos, node)` calls `graph.addNode`, then applies the `GraphChange`: for each `merged` id, `survivor.onAbsorb(old)`, `old.onDisposed()`, remove; for each `created` id, `factory.create(id, this)`; then `survivor.onNodeAdded`. `nodeRemoved(pos)` calls `onNodeRemoved` on the old network first (while it still knows the node), then `graph.removeNode`, then applies `created` (new networks for split parts) and `removed` (dispose). `nodeRefreshed(pos)` uses `graph.refresh`. `tick()` iterates the `TreeMap` in id order: drain mailbox into `handle`, then `onTick`. `disposeAll()` for level unload.
Alternative: one graph per network instead of one per type. Rejected because merging would require re-keying nodes across graphs; one graph per type with components as networks is what the graph was built for.

### D5: NetworkType<N, E, T>
Record-like final class: `ResourceLocation id`, `ConnectionRule<N> rule`, `NetworkFactory<N, E, T> factory`. `NetworkTypes.register(type)` stores it in a `LinkedHashMap` keyed by id, so iteration order is registration order (spec: tick in registration order). Registration happens in mod constructors, before any level loads.

### D6: NetworkNode and NetworkBlockEntity
`NetworkNode<N>` interface: `NetworkType<N, ?, ?> networkType()` and `N networkNode()` (the value stored in the graph; a block entity may return itself). `NetworkBlockEntity` is an abstract `BlockEntity` implementing it: `onLoad()` joins when `level` is a `ServerLevel`; `setRemoved()` and `onChunkUnloaded()` leave; a `joined` flag prevents double leave. `refreshNetworkConnections()` for subclasses to call after a side mode changes. Mods that cannot extend the base can call `NetworkManager.of(level).join(...)`/`leave(...)` themselves.

### D7: NetworkManager
`Map<NetworkType, NetworkSet>` filled lazily from `NetworkTypes`. Static `Map<ResourceKey<Level>, NetworkManager> MANAGERS`; `of(ServerLevel)` creates on demand. `NetworkEvents` (game bus, `@EventBusSubscriber(modid = "servus_lib")`): `LevelTickEvent.Post` ticks the manager when `getLevel()` is a `ServerLevel` and a manager exists; `LevelEvent.Unload` disposes and drops it. Server stop is covered because every level unloads.
Alternative: NeoForge data attachments on the level. Rejected for now: a static map is simpler and the unload event already gives the lifecycle.

### D8: Conversions
`Conversions.toNodePos(BlockPos)`, `toBlockPos(NodePos)`, `toSide(Direction)` and `toDirection(Side)` by ordinal, guarded by a unit test in the Minecraft layer? No: Minecraft classes cannot load in plain JUnit. The ordinal match is asserted in `SideTest` against a hard coded list and documented on `Side`.

## Pure Java vs Minecraft

- Pure Java, JUnit tested: `Mailbox`, `ServusNetwork`, `NetworkSet`, `NetworkFactory`.
- Minecraft, verified by launching the game and by the first real network type in M2: `NetworkType`, `NetworkTypes`, `NetworkNode`, `NetworkBlockEntity`, `NetworkManager`, `NetworkEvents`, `Conversions`.

## PLAN.md decisions settled

None. Implements the M1 items as planned.

## Risks / Trade-offs

- [No node blocks exist yet, so the Minecraft glue is only exercised by launching] -> The glue is small and M2's power cable is the first end to end check. If it fails there, it is fixed in that change.
- [Static manager map keyed by level] -> Cleared on `LevelEvent.Unload`. Integrated servers reload levels per world, so stale entries cannot survive a world switch.
- [onChunkUnloaded and setRemoved may both fire] -> `joined` flag makes leave idempotent.
