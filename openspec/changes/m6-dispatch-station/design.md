# Design

## Context

See proposal.md for motivation. The warehouse is `WarehouseNetwork`, a `ServusNetwork` over `StorageNodeBlockEntity` nodes linked by the pure `WarehouseConnectionRule` with kinds `STOCK`, `RAIL`, `ELEVATOR`. The network keeps `sources` (stock nodes plus their foreign neighbours, indexed by `WarehouseIndex`), `connectors` (rails and segments, the travel graph), `extract` and `insert`, and ticks through `onTick` after draining its mailbox of `WarehouseEvent`s. `insert` has a fixed preference: sources holding the item, empty crates, other nodes, foreign inventories in use. `ShuttleFleet` owns the shuttles and a `PickJob` queue; each shuttle has a `ShuttleMotion` on a path from `RailPaths.find`, goes out, loads beside the goal rail, comes home and delivers. `WarehouseControllerBlockEntity` persists only the shuttle count. `RailBlock.docksTo` docks onto nodes with `holdsStock()` and `acceptsRail(side)`. `StorageStatsManager` loads `warehouse.json`. The crate and rack block entities sync to clients through `getUpdateTag` and have renderers that draw text with `Font.drawInBatch` (crate) and items (rack). The Logistics duct GUI edits filters with a `GhostSlot` reading through a supplier; Storage cannot import it but can copy the pattern. Blocks already return `SKIP_DEFAULT_BLOCK_INTERACTION` for the Configuration Carrot so the item's `useOn` runs.

## Goals / Non-Goals

**Goals:**
- Routing is one pure decision (`PutAwayPlan`) used by every insert path, unit tested without Minecraft.
- No new scanning: the station drains on buffer change events; blocked stations retry on a slow timer; zone and rule changes are events.
- Put-away reuses the fleet, paths and rendering; a shuttle is still a motion plus cargo plus a job.
- Zones are in world and persistent on the blocks; rules are persistent on the controller.

**Non-Goals:**
- See proposal non-goals. Also no reservation of target room across jobs: two shuttles may be sent to one target and the second brings its leftover back.

## Decisions

### Settles no PLAN.md open decision
Section 5 has no bullet about put-away. Section 2.3 already states put-away rules with overflow zones; this change gives them the concrete shape below and `PLAN.md` needs no edit.

### Zones as a stamped name on the block, not a region or a marker block
Alternatives: a Zone Marker block with a radius (needs scanning or region math, awkward on multi floor layouts), a region picked with the carrot (fiddly), a zone field in each block's GUI (crates have none). Chosen: `ZoneLabelItem`, named in a vanilla anvil (`DataComponents.CUSTOM_NAME`, read with `ItemStack.getHoverName()`), stamps `StorageNodeBlockEntity.zone`. The base class gets a `zone` string, `saveAdditional`/`loadAdditional` for `"Zone"`, and includes it in `getUpdateTag`; crate and rack already send update tags, so the client gets it for free. `setZone` calls `setChanged`, `level.sendBlockUpdated` and posts `WarehouseEvent.ZoneChanged(pos)`. Crate, Double Crate, Quad Crate and Heavy Rack blocks return `SKIP_DEFAULT_BLOCK_INTERACTION` from `useItemOn` when the held item is a `ZoneLabelItem`, exactly as they do for the carrot, so `ZoneLabelItem.useOn` runs; the item only acts on a `StorageNodeBlockEntity` whose `kind()` is `STOCK` and whose block is not the controller or terminal (a `zonable()` hook on the block entity, true for crates and racks). `saveToItem` on crate and rack already copies the block entity tag, so the zone survives dismantling. Zone key for comparison is `ZoneNames.key(name)`: trimmed, lower cased with `Locale.ROOT`; the display name is the first spelling seen.

### Rules per zone, held by the controller, edited at any station
Alternative: an ordered rule list per station. Rejected because terminal pushes and hopper feeds must obey the same rules, and a warehouse has one layout. `WarehouseControllerBlockEntity` gets `ZoneRules rules` persisted as NBT list `"Zones"`: `{Name, Items: [ItemStack], Tags: [String], Overflow: boolean}`. `ZoneRules` is a Minecraft side holder (ItemStack filters) that produces a pure snapshot `Map<String, PutAwayPlan.Rule<ItemKey>>` keyed by zone key, rebuilt lazily when edited. The network reaches the rules through the controller position it already tracks (`fleet.controller()`); a network with no controller has no rules and is offline anyway. Edits go `DispatchStationMenu` to `WarehouseNetwork.rules()` to the controller block entity, which calls `setChanged`. The controller block's carrot dismantle uses `saveToItem` like crates do, so the rules ride along in the item; a hand break drops a plain controller.

### Pure routing: `PutAwayPlan`
Package `io.github.dirnei.mc.servus.storage.zone`, no Minecraft imports:
- `record Rule<K>(Set<K> items, Set<String> tags, boolean overflow)` with `filtered()` = items or tags non empty.
- `PutAwayPlan.route(K key, Map<String, Rule<K>> rules, Function<K, Set<String>> tagsOf)` returns `Route(List<Pool> pools, Reason blockedReason)`. `Pool` is `Zone(name)` or `General`. The primary pool is the best matching filtered zone (exact item beats tag, then alphabetical by key) or `General`; the overflow zone is appended when one exists and is not already the primary. `General` means unzoned blocks plus zones whose rule is not filtered and not overflow.
- `Reason` is `ZONE_FULL_NO_OVERFLOW`, `OVERFLOW_FULL`, `NO_STORAGE`, computed from the pools that were tried.
- `ZoneNames.key(String)` for normalisation.
`PutAwayPlanTest` covers: exact over tag, alphabetical tie, no match goes general then overflow, overflow zone never primary unless its own filter matches, unfiltered non overflow zone is part of general, reasons for each blocked case. `ZoneNamesTest` covers trim and case.

The network resolves pools to blocks: `zoneOf: Map<BlockPos, String>` maintained from `node.zone()` at `onNodeAdded`, on `ZoneChanged`, and on absorb; `General` is sources whose position has no zone key or whose zone's rule is unfiltered and not overflow, plus foreign inventories (foreign inventories always count as general). Within a pool the existing four level order stays: holding the key, empty crates, other nodes, foreign in use. `insert(stack)` becomes: route, then `insertInto` per pool in order, with the leftover returned. Tag lookup is `stack.getTags().map(t -> t.location().toString())` on the server; the pure plan sees tag ids as strings.

### Station node kind
`WarehouseConnectionRule.NodeKind` gains `STATION`. Rule: `STOCK` and `STATION` link to `STOCK`, `STATION` and `RAIL` on any side and never to `ELEVATOR`; `RAIL` links to `STATION` like to `STOCK`; `ELEVATOR` never links to `STATION`. `WarehouseConnectionRuleTest` gets the station cases. `StorageNodeBlockEntity.holdsStock()` stays `kind() == STOCK`; a new `docksRail()` is true for `STOCK` and `STATION` and `RailBlock.docksTo` and `WarehouseNetwork.railSideAccepted` use it. In `WarehouseNetwork`, `onNodeAdded` puts a station into a new `stations` set (neither source nor connector), `rebuildCandidates` passes stations together with connectors so no neighbour of a station becomes a candidate, and `isTravelNode` is unchanged.

### Intake and drain
`DispatchStationBlockEntity` (kind `STATION`, `MenuProvider`) holds an `ItemStackHandler buffer` sized from `StorageStatsManager.dispatchBufferSlots()` and a `boolean[] claimed`. The capability registered in `ServusStorageEvents.ModBus.onRegisterCapabilities` with `RegisterCapabilitiesEvent.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ...)` (the pattern Logistics and Manufacturing use) returns an insert only view: `extractItem` returns `ItemStack.EMPTY`. `onContentsChanged` calls `setChanged` and posts `WarehouseEvent.Intake(pos)`; the network adds the station to `pendingStations`. `onTick` drains each pending station: for up to `put_away_per_tick` unclaimed stacks, `route` the key, walk the pools and pick the first block whose handler accepts at least one item (`ItemHandlerHelper.insertItem(handler, stack, true)`). If none accepts, the stack stays, the station records the `Reason` and key for its GUI, sets its `BACKED_UP` block state, and the network keeps it in `blockedStations`, retried every 20 ticks and at once on `ZoneChanged`, rule edits and index changes of a zone block. If a target is found and `fleet.canCarry(stationPos, targetPos)` (shuttles exist, home rail exists, both positions touch travel nodes the home rail reaches), the slot is claimed and `PutAwayJob(station, slot, key, amount, target)` is queued; otherwise the stack moves at once and the remainder goes back into the buffer. `BACKED_UP` clears when the buffer has no blocked stack. The buffer drops with `Containers.dropContents` style popping in `onRemove`.

### Fleet: one queue, two job kinds
`ShuttleJob` is a sealed interface over `PickJob` and `PutAwayJob`. The shuttle gets a `Leg` enum: `TO_SOURCE`, `TO_TARGET`, `TO_STATION` (bringing leftovers back), `HOME`. A pick is `TO_SOURCE` then `HOME` as today. A put-away is `TO_SOURCE` (station rail; load with `host.takeFromStation(station, slot, key, amount)` which clears the claim), `TO_TARGET` (`host.insertAt(target, cargo)`), then `HOME`, or `TO_STATION` first when cargo is left (`host.returnToStation(station, cargo)`, popping at the station if it does not fit). Goal rails for any position come from one `railsBeside(BlockPos)` helper replacing the key based `goals`. Paths come from `RailPaths.find` as today. `assign` picks the nearest free shuttle only in the sense already implemented (first free shuttle, path from its current node); jobs are polled in order from the shared `ArrayDeque`. When the station is gone at loading time the job ends and the shuttle goes home. `dropCargo` on release pops cargo at the controller as today. `ShuttleView` is unchanged; cargo is empty on the legs where nothing is carried. `summary()` adds the put-away count.

### Station GUI
`DispatchStationMenu` (registered in `ServusStorageMenus` via `IMenuTypeExtension.create`) has `SlotItemHandler`s for the buffer, the player inventory, and nine `GhostSlot`s (a Storage copy of the Logistics class) reading through a supplier that returns the selected zone's filter handler from the controller's `ZoneRules`. Zone list and the selected zone's tags and overflow flag are sent with a `ZoneRulesSyncPayload` from `broadcastChanges` on a signature change, like `StockSyncPayload`. Client to server: `SelectZonePayload(key)`, `SetZoneTagsPayload(text)`, `SetOverflowPayload(key, flag)`; ghost slots use the vanilla click path with `GhostSlot` semantics (set from cursor, clear on empty cursor); `quickMoveStack` moves between the inventory and the buffer, so shift click feeds the station as the spec asks. The screen is an `AbstractContainerScreen` with the buffer row, a scrollable zone list, the ghost row, an `EditBox` for tags and a toggle button; offline shows the reason like the terminal. Zone list entries carry a block count so empty zones render grey. The backed up reason is a translatable key plus the item name, sent in the same payload.

### Datapack and datagen
`warehouse.json` gains `dispatch_buffer_slots` (9) and `put_away_per_tick` (1), read in `StorageStatsManager`. Datagen: station blockstate with `FACING`, `ONLINE` and `BACKED_UP` (orientable models with dark, lit and red front like the existing lamp helper), item models for the station and label, loot tables, recipes (station: chest over hopper framed by iron ingots; label: paper over a dye, shapeless), block tags (`mineable/pickaxe`), lang for block, item, GUI strings and reasons. Textures are placeholders drawn with the existing `png.py` helper.

### Minecraft side verification
GameTests in `ServusStorageGameTests`: `stationPutsAwayInstantly` (station touching the controller, hopper fed stack lands in a crate), `stationRoutesToZone` (two crates, one stamped and a rule set through `ZoneRules` directly, item lands in the zoned crate), `stationBacksUpWhenZoneFull` (zoned crate full, no overflow, stack waits and `BACKED_UP` is true, then stamping another crate releases it), `shuttleCarriesPutAway` (rail from controller past a station to a crate, one shuttle, stack leaves the buffer only after the shuttle arrives and lands in the crate later), `chestOnStationIsNotStock`. Manual in the client: anvil naming, stamping and the drawn zone name, the GUI editing flow, hopper and Utility Duct feeding, the red lamp.

## Risks / Trade-offs

- [Claimed stacks and players editing the buffer] → claims are per slot and re-validated at loading time; a missing stack ends the job and the shuttle goes home.
- [Two jobs to one target fill it] → the second shuttle brings its leftover back to the station; no reservations in this change.
- [Blocked station retry is a timer] → it is one check per blocked station every 20 ticks and only while something is blocked; zone and rule events also wake it.
- [Zone names typed differently] → normalised key for matching, first spelling kept for display; the `zones` command shows both.
- [Rules lost on a hand broken controller] → the station GUI makes rules quick to re-enter and the carrot dismantle keeps them; a future change can move rules into `SavedData` if players ask.
- [Tags typed wrong] → unknown tags simply match nothing; the GUI shows a tag in red when no item carries it.

## Open Questions

None that change the specs or tasks. Texture polish and the exact GUI layout are decided during apply.
