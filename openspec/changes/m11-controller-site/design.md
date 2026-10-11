## Context

The Warehouse Controller (`WarehouseControllerBlock`, `WarehouseControllerBlockEntity`) is a `StorageNodeBlockEntity` that makes its network online when it is the only controller. It has no GUI: right click prints `WarehouseNetwork.summary()` to chat, stock rules are edited by commands and in the Storage Terminal, and the rules travel in the item on a carrot dismantle through `saveToItem`. Block entities join their `WarehouseNetwork` in `onLoad` and leave it in `setRemoved` and `onChunkUnloaded` (`NetworkBlockEntity`), so a network only ever contains the members whose chunks are loaded. The network keeps its foreign inventories in `sources` (`node == false`), found from the neighbours of stock members, and polls them in batches.

M11 (decided 2026-10-11, see `PLAN.md`) makes every controller a site. This change gives the site its identity and its chunk bookkeeping; `m11-chunk-loader-upgrade` adds the wake and `m11-wireless-packing-station` the link between sites.

## Goals / Non-Goals

**Goals:**
- A name and an owner team per controller, a first controller GUI.
- The chunk span from events, robust against partly unloaded sites.
- Chunk Loader Upgrade item and slots, capacity, and a visible over capacity error.

**Non-Goals:**
- No wake, no chunk tickets, no site registry: that is `m11-chunk-loader-upgrade`.
- No stock rule editing in the controller GUI (later, together with SEND and KEEP rules, per `PLAN.md`).
- No GUI for a controller of an offline network beyond the online state line; the span is only updated while the network has exactly one controller.

## Decisions

### Span as a set of chunk keys, merged with what is not loaded
The controller stores a `Set<Long>` of chunk keys (`x & 0xffffffff | z << 32`, the same packing as `ChunkPos.asLong`, done in the pure `SiteChunks` so no Minecraft type is needed). After any membership or source change the network sets `spanDirty`; on its next tick, with exactly one controller, it builds the current set from `members()` plus every foreign source that has been indexed (a candidate position without an item handler never counts), then merges: `current ∪ { c ∈ previous | c is not loaded }`. This keeps the far part of a site that unloaded (its members left the network, but the site still spans that chunk), and still shrinks the set when a part is really removed while loaded.

Alternatives: keeping per chunk member counts would double count members that rejoin after a chunk load; persisting every member position costs a lot for big rail networks and still needs the same merge for removals while unloaded. The merge needs no flags on `onChunkUnloaded`. The one gap: something broken while its chunk is loaded but the controller's chunk is not stays in the set until the site is loaded together again; recorded here, accepted.

"Loaded" is `ServerLevel.hasChunk(x, z)`, checked in the tick after the event, when an unloading chunk is no longer visible. The set is recomputed at most once per tick and only after a change: iterating the network's members is in memory, not a block scan.

### Span does not travel, team is set at placement
`saveToItem` writes the whole block entity tag, so the name and rules travel like before. The span and the team would be wrong at a new place: `setPlacedBy` resets the span to the controller's own chunk and sets the team to `Teams.teamOf(player)` (Foundation `team` API). A controller placed by no player (a GameTest `setBlock`) has no team until a player places one; later changes treat that as "no team".

### Chunk Loader Upgrade item and slots
A Storage item `chunk_loader_upgrade`, like the Field Head's Range Upgrade: it is not in the `servus_foundation:upgrades` tag, because the Foundation upgrade API is about rate multipliers. The slots reuse Foundation's `UpgradeSlots` container class (one item per slot) through `ChunkLoaderUpgradeSlots`, which only accepts this item, like `LiftUpgradeSlots` does for the elevator. Slot count `chunk_loader_upgrade_slots` (4) and `chunks_per_upgrade` (9) are read by `StorageStatsManager` from `storage/warehouse.json`. No capability exposes the slots, so automation cannot reach them. Upgrades drop when the block is removed; on a carrot dismantle they are taken out first, so the item carries name and rules but no upgrades (no duplicates through `saveToItem` plus `onRemove`).

Recipe (datagen, shaped): ender pearl above, iron ingots left and right, the Foundation Chunk Loader (`MachineContent.CHUNK_LOADER_ITEM`) in the middle, redstone below. Storage already depends on Foundation.

### State and lamp
`SiteCapacity.state(span, upgrades, perUpgrade)` gives `NO_UPGRADE`, `COVERED` or `OVER`. The controller block gets `StorageNodeBlocks.ACTIVITY` next to `ONLINE`; the block entity writes `BLOCKED` for `OVER` and `IDLE` otherwise whenever span or upgrades change. `WORKING` stays unused here; `m11-chunk-loader-upgrade` uses it for an awake site. The model reuses the station datagen (`station()` with dark, idle, working and blocked fronts); the controller's lit front texture becomes the idle front and working and blocked are recoloured copies of its lamp pixels. Without upgrades there is no capacity to exceed: every plain warehouse would otherwise glow red, so it is not an error, and the GUI says the site cannot wake.

### GUI and sync
`WarehouseControllerMenu` holds the upgrade slots and the player inventory; the opening buffer carries the position, the slot count and the name. Every 10 ticks it builds a `SiteStatusPayload` (name, online, summary, span keys, controller chunk, upgrades, capacity, state) and sends it when its hash changed, like the Packing Station's status payload. The client keeps it in `ClientSiteStatus`. The name field sends `RenameSitePayload` on Enter, focus loss and close (the Zone Label pattern); the server trims it, cuts it to 32 characters, drops control characters and checks the player can still use the menu.

`ChunkMapLayout` (pure) places the span in a 60 by 60 pixel box: bounds from the keys, a cell size of 60 divided by the larger side clamped to 2..10 pixels, centred; a span too wide for 2 pixel cells shows the window around the controller's chunk. Colours: span cells green, the controller's chunk gold, red cells while over capacity.

Right click with an empty hand opens the GUI instead of printing the summary; the summary is the tooltip of the online line and still printed by `/servus_warehouse info`.

### Display name
`SiteName.display(name, x, z)` returns the trimmed name or "Warehouse at x, z"; `SiteName.clean` is the server side cleaning. The GUI title, the info command and the later site lists use it.

## Risks / Trade-offs

- [A site whose controller chunk unloads cannot see removals elsewhere] The merge keeps those chunks until the site is loaded together again. Over counting only makes the site look bigger, never lets an over capacity site wake.
- [Big sites recompute the set from all members after each change] Changes come in bursts on chunk load; the dirty flag makes it once per tick. Fine for thousands of members.
- [Controller lamp swap] Existing worlds keep working; the block gains a property with a default (`IDLE`), nothing is released yet.
