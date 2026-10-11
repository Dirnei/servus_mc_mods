## Why

A site is only useful from far away when it can work while nobody is near it. `m11-controller-site` gave every controller a name, a chunk span and Chunk Loader Upgrade slots with a capacity; this change uses them: a site with enough upgrades can **wake** for a transfer, loading the whole site for as long as the work runs, and every site leaves a record behind (name, team, chunks, last known stock) that other sites can read while it sleeps. `m11-wireless-packing-station` then wakes sites for real transfers.

## What Changes

- New site registry kept with the world: one record per controller with its dimension, position, name, team, chunk span, upgrade count and its **last known stock**, refreshed while the site is online (stock at most every `site_snapshot_interval` ticks, example 100) and kept while it sleeps. A broken or dismantled controller removes its record; a chunk unload keeps it.
- **Wake**: a request to wake a site loads every chunk of its recorded span with a Storage chunk ticket (NeoForge `TicketController`, ticking), keeps them loaded while anything holds the wake and while the site's own work runs (shuttle jobs and queued jobs, inbound put-aways, held deliveries), then for `wake_grace_seconds` more (example 30, `storage/warehouse.json`), then releases every chunk.
- **Hard limit**: a site without Chunk Loader Upgrades, or whose span exceeds what they cover, never wakes. A wake needs an owner team, counts as one active loader against the team cap of the Foundation Chunk Loader (`maxChunkLoadersPerTeam`) and respects FTB Chunks like the Chunk Loader does (`chunkLoaderRespectsFtbChunks`). A wake that may not happen waits and says why; it is retried every tick while it is still wanted. No FE cost for the wake in this slice.
- A wake does not survive a restart: tickets are dropped on load and team registrations of wakes are cleared on stop and on start. The warehouse network itself still never loads a chunk; only a wake does.
- Controller: the activity lamp turns orange while the site is awake by a wake; the GUI shows a wake line (awake for a transfer, waiting with the reason, or the grace time left).
- Commands: `/servus_warehouse wake <pos>` requests a wake for the controller at that position (in the command's dimension) and reports what happened, so the wake is testable before `m11-wireless-packing-station` adds the real trigger; `/servus_warehouse sites` lists the site records with name, team, chunks, capacity, awake or asleep and the number of item types last known.
- Pure logic without Minecraft imports and with JUnit tests: the wake state machine (asleep, waiting, awake, grace) and the wake allowance (reasons in a fixed order).

## Capabilities

### New Capabilities

- `storage/site-wake`: the site registry with last known stock, waking a site, the hard limit, the team cap and FTB Chunks, what keeps a site awake, the grace time and release, wakes across restarts, the wake command and the sites command.

### Modified Capabilities

- `storage/warehouse-controller`: the controller's activity lamp and GUI show the wake.

## Impact

- New: `storage/.../site/WakeMachine.java`, `WakeAllowance.java` (pure), `storage/.../site/SiteRecord.java`, `SiteRegistry.java` (SavedData), `SiteWakes.java` (runtime wakes, tickets, team registration), `SiteTickets.java` (ticket controller registration), `storage/.../site/SiteEvents.java` (server tick, stop and start); JUnit tests under `storage/src/test/.../site/`; `gametest/SiteWakeGameTests.java`.
- Changed: `WarehouseControllerBlockEntity` (registry updates, removal versus unload, wake lamp and line), `WarehouseNetwork` (`siteBusy`, stock snapshot), `StorageStatsManager` and `storage/warehouse.json` (`wake_grace_seconds`, `site_snapshot_interval`), `ServusStorageCommands` (`wake`, `sites`), lang.
- Uses Foundation's `ChunkLoaderData` (team registrations), `ServusFoundationConfig` (cap and FTB Chunks switch) and `Teams`.
- Docs page `docs/src/content/docs/mods/storage.md` and `PLAN.md` M11.
