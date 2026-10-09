# Design

## Context

See proposal.md. Thread dumps (25 samples) of the hung dedicated server show only vanilla frames: `MinecraftServer.stopServer` -> `ServerChunkCache.tick` -> `ChunkMap.processUnloads` -> `scheduleUnload`. In 1.21.1 `scheduleUnload` reschedules itself while the holder's save future changes, which happens when something requests the chunk again. `ServerChunkCache.getChunk(..., FULL, true)` on a holder that is waiting to unload adds a ticket and brings it back. Bisecting the user's world with `/fill ... air replace` showed: removing only the rails makes the server stop cleanly.

## Goals / Non-Goals

**Goals:** no chunk load from `WarehouseNetwork` membership hooks and lookups; the user's world stops cleanly.

**Non-Goals:** a framework wide audit of every network.

## Decisions

- `pullsAround(pos)` reads `owner().graph().node(...)` (the member object) instead of the world. Members are the only positions it is called for in `rebuildCandidates`; for a non-member it falls back to the loaded-only lookup.
- A private `loaded(BlockPos)` helper returns `level.getBlockEntity(pos)` only when `level.isLoaded(pos)`; `Level.isLoaded` asks the chunk cache without loading (`hasChunk` uses `getChunk(..., false)`), so a holder waiting to unload counts as not loaded. Every `level.getBlockEntity` in `WarehouseNetwork` uses it; `dockRails` returns no rails for an unloaded rack position.
- `addSource(pos, false)` still adds a candidate in an unloaded chunk (it cannot tell yet whether it is a storage block); `poll` drops a non-member source whose loaded block entity is a storage node, so a rack without a front rail never gets indexed.
- Pure vs Minecraft: no pure logic changes; covered by a GameTest and the reproduction on the dedicated server.

## Risks / Trade-offs

- [A candidate in an unloaded chunk is polled later] -> polling already skips sources without a capability; the extra check drops storage nodes.
- [Other networks could have the same pattern] -> the data network already checks `isLoaded`; duct lines and tanks only build capability caches in their hooks.
