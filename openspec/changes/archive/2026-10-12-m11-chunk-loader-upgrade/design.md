## Context

`m11-controller-site` made every Warehouse Controller a site with a name, a team (set at placement), a chunk span kept by its `WarehouseNetwork` and Chunk Loader Upgrade slots with a capacity (`SiteCapacity`). A site exists only while its controller is loaded: the network forgets every member whose chunk unloads (`NetworkBlockEntity.onChunkUnloaded`), and spec rule "The network never loads chunks" forbids the network from loading any.

Foundation's Chunk Loader (`ChunkLoaderBlockEntity`) forces its own chunk through a `TicketController` registered in `RegisterTicketControllersEvent` (`ChunkLoaderTickets`), counts active loaders per team in the `ChunkLoaderData` SavedData against `ServusFoundationConfig.maxChunkLoadersPerTeam()`, and stays inactive when FTB Chunks is loaded and `chunkLoaderRespectsFtbChunks()` is on (`ModList.get().isLoaded("ftbchunks")`).

## Goals / Non-Goals

**Goals:**
- A record per site that outlives its chunks, with last known stock, for `m11-wireless-packing-station` to list and order from.
- Waking a whole site for as long as its work runs plus a grace time, inside the team cap and FTB Chunks, with a hard capacity limit.
- A debug command to wake a site before the wireless link exists.

**Non-Goals:**
- No FE cost for a wake (the cost is the upgrades and the team slot).
- No wake trigger besides the command; wireless transfers come next change.
- No cross dimension anything.

## Decisions

### Site registry as SavedData
`SiteRegistry` is a `SavedData` on the overworld's data storage (`servus_storage_sites`), like `ChunkLoaderData`. Key: dimension id plus controller position. A `SiteRecord` holds dimension, position, display name, team (nullable), span (chunk keys), upgrade count, last known stock (`ItemKey` to count, saved as item stacks of one with a long count) and the game time last seen online. The capacity is computed from the upgrade count and the current `chunks_per_upgrade`, so a datapack change applies to sleeping sites too.

The controller writes its record when its name, team, span or upgrades change and on load. The network snapshots the index for its controller every `site_snapshot_interval` ticks (100) while online and writes it only when the counts changed, so the SavedData is not marked dirty for nothing. A controller removes its record in `setRemoved` only when it was not unloading (a flag set in `onChunkUnloaded`, which `LevelChunk.clearAllBlockEntities` calls before `setRemoved`, checked in the 1.21.1 source) and its position, still loaded, holds no controller any more; a server shutdown keeps the record.

### Wake state machine (pure)
`WakeMachine` has the states `ASLEEP`, `WAITING`, `AWAKE` and `GRACE` and answers each tick with an action (`NONE`, `LOAD`, `RELEASE`) from three inputs: wanted (a pending request or any hold), busy (the site's own work) and the allowance. Asleep or waiting plus wanted wakes when allowed, else waits with the reason; waiting without being wanted falls asleep. Awake without wanted and busy starts the grace countdown; wanted or busy during grace goes back to awake; the countdown at zero releases. Awake or in grace, an allowance of no upgrade or over capacity releases at once (hard limit). The team limit and FTB Chunks are checked only when waking: once a site holds its team slot it keeps it until it sleeps.

`WakeAllowance.check(hasTeam, ftbBlocks, capacityState, registered, teamActive, teamCap)` returns the first reason in this order: no Chunk Loader Upgrade, over capacity, no owner, use FTB Chunks, team limit reached, else allowed. A site already registered for its team is never refused for the cap.

### Runtime wakes and tickets
`SiteWakes` keeps one `Wake` per site being woken (machine, holds, a one tick request flag, the chunks it forced). A request sets the flag; `hold(site, token)` and `release(token)` are the API for transfers in the next change. It ticks on `ServerTickEvent.Post`: wanted is the flag or a hold, busy is `WarehouseNetwork.siteBusy()` when the controller is loaded and online (shuttles out, jobs queued, inbound buffers waiting, held or deferred deliveries), the allowance comes from the record and Foundation's cap data. `LOAD` registers the site in `ChunkLoaderData` for its team (key: the controller position, like a Chunk Loader's) and forces every chunk of the recorded span; while awake the forced set follows the record's span, so chunks the woken site reveals load too. `RELEASE` unforces exactly the chunks it forced and unregisters the team slot. A wake that is asleep with no hold and no request is dropped.

Tickets come from a Storage `TicketController` (`servus_storage:site_wake`, registered in `RegisterTicketControllersEvent`) with `forceChunk(level, controllerPos, x, z, add, ticking = true)`. Verified in the NeoForge 21.1 source: `TicketController(ResourceLocation, LoadingValidationCallback)`, `forceChunk(ServerLevel, BlockPos, int, int, boolean, boolean)`, `LoadingValidationCallback.validateTickets(ServerLevel, TicketHelper)` and `TicketHelper.getBlockTickets()` / `removeAllTickets(BlockPos)`. The validation callback removes every ticket of this controller when a world loads, so a wake never survives a restart. On `ServerStoppingEvent` every wake is released while the levels still exist, and on `ServerStartedEvent` every site's team registration is removed from `ChunkLoaderData` as a backstop.

A wake is requested whenever asked, even while a player keeps the site loaded: it then still holds the site for the transfer if the player walks away, at the price of the team slot while it runs.

### Busy
`siteBusy()` is true while the fleet has shuttles out or jobs queued, an inbound station waits to be drained, or deliveries are held or deferred for a destination. Field runs count as shuttles out, so a woken site finishes a harvest it started.

### Lamp and GUI
`WarehouseControllerBlockEntity.lampActivity()` returns blocked over capacity, working while `SiteWakes` reports the site awake or in grace, idle otherwise; `SiteWakes` refreshes the lamp of a loaded controller on every state change. `wakeLine()` and `wakeAlert()` (the hooks the GUI payload already carries) return "Awake for a transfer", "Awake, sleeping in N s" or "Waiting to wake: <reason>" (red).

### Numbers
`wake_grace_seconds` (30) and `site_snapshot_interval` (100 ticks) in `storage/warehouse.json`, read by `StorageStatsManager`, with test setters.

## Risks / Trade-offs

- [The recorded span can be stale when the site changed while partly loaded] The span only grows while partly loaded (`m11-controller-site`), so a stale span can only refuse a wake, never let an over capacity site through; the woken site recomputes it and a growth beyond capacity releases at once.
- [A wake takes a team slot even while a player is near] Simple and robust for transfers; the slot is freed after the grace time.
- [Stock snapshots of big warehouses] Every 100 ticks a copy of the index entries; written only when changed.
