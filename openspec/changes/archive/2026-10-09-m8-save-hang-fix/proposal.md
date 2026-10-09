# Proposal

## Why

During the M8 play test the client froze on "Saving world". Reproduced on a dedicated server with the user's world: the server thread loops forever in `MinecraftServer.stopServer` while `ChunkMap.scheduleUnload` keeps rescheduling. A warehouse network that spans several chunks (linked by rails) reads block entities of its members from `onNodeRemoved` (`rebuildCandidates` calls `pullsAround`, which calls `level.getBlockEntity` for every member). When chunk A unloads, that read loads chunk B, which was already waiting to unload, so B's unload is rescheduled; B's unload then loads A again, forever. A fresh world, or the user's world with only its rails removed, stops cleanly.

This is a bug fix inside PLAN.md M8 (Storage automation runs on the warehouse network); it adds no milestone item.

## What Changes

- The warehouse network never loads a chunk from its membership hooks or lookups: whether a member pulls its neighbours in is read from the member object in the graph, and every block entity read goes through a loaded-only lookup that answers "nothing" for an unloaded chunk instead of loading it.
- A neighbour candidate in an unloaded chunk is kept as a candidate and checked when it is polled: a storage block found there is dropped instead of being indexed.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/warehouse-controller`: a new requirement that the warehouse network never loads chunks, so a world with a warehouse spanning chunks saves and stops.

## Non-goals

- Auditing every other network (data, duct lines, tank) beyond what the reproduction needs; they already check loaded chunks or only build capability caches.
- Changing how racks, rails or candidates connect.

## Impact

- `storage/.../network/WarehouseNetwork.java`.
- The dedicated server stop check (servus-helpers/stop_check.sh) and the existing Storage GameTests.
