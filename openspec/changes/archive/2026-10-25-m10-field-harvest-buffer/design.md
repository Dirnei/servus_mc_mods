# Design

## Context

`shuttle/FieldRuns` ends a harvester's cargo trip in `putAwayHarvest`: `unloadToStore` moves the cargo into `run.store`, `fillBufferFirst` drives to the head's rail to top up the supply buffer, and `continueStore` plans rack stops through `host.storeTargets(...)` with the harvester's lift-free ways, locks room there and unloads at each stop (`unloadAtStop`). With no reachable rack it parks with cargo. The Spreader's leftovers use the same `unloadToStore`/`fillBufferFirst`/`continueStore` path. Supply runs already drive plain shuttles to the head's rail (`goToHead`, `atHead`) and carry stale supply items away. The Field Head is a lock holder (`WarehouseNetwork.servedHeads()` in `lockHolders()`), and its supply buffer is the counted `SupplyBuffer`. Inbound Stations are put away by `WarehouseNetwork.drain(pos, InboundStationBlockEntity)` from `pendingStations`.

`m8-wtp-ingress` reworks `drain` and `sweepLocks` and is applied before this change; this design names the behaviour, not line positions.

PLAN.md leaves no open decision here; the explore session chose that field shuttles always unload at the head (option C) and that a garage on another floor stays unsupported.

## Goals / Non-Goals

**Goals:**
- Field shuttles never plan rack trips.
- Items leaving a field reach storage by the zone rules, across floors, through plain shuttles.

**Non-Goals:**
- Lifts for wide shuttles.
- A data line or pipe face on the head.

## Decisions

### The buffer
`FieldHeadBlockEntity` gets `toStorage`, a non counted `LockedSlots` of `harvest_buffer_slots` (`ShuttlesStatsManager`, `field.json`, example 9), added to `lockedSlots()` so the lock sweep sees it, saved under its own tag (missing tag loads empty) and dropped with the head's other contents. The head still registers no item capability.

### Unloading at the head
The supply parts are still filled first by the existing `fillBufferFirst`/`storeIntoBuffer` path (seeds for a Harvester, fertilizer for a Spreader). A pure `field/HeadUnload` plan then fills the To storage buffer from the stacks left: same item slots with room first, then empty slots, locked slots take nothing; what does not fit stays aboard. `FieldHeadBlockEntity.unloadToStorage` applies the plan, `toStorageTakesAny` and `toStorageHasFreeSlot` read it. JUnit covers empty slots, same item first, a full buffer, locked slots and the free slot check.

`FieldRuns.continueStore` branches: a supply run (plain shuttle) keeps the rack trips through `storeTargets`; a Harvester or Spreader goes to `storeAtHead`. That drives to the head's rail (`goToHead`, arriving through the existing `TO_HEAD` legs that top up the supply buffer first), unloads by `HeadUnload` and goes on with its run. What stays aboard parks the shuttle with `ShuttleFleet.strand(shuttle, StrandReason.HEAD_FULL)` (the garage says the head's To storage is full); the parked shuttle retries on the fleet's `roomChanged`, which the head's buffer changes trigger, and drives out only when the buffer would take some of its cargo. Losing the head mid run calls `lose`, which drops the cargo at the garage like a lost rail.

### Put-away from the head
`drain` and `joinOpenPutAway` take a small `network/PutAwaySource` interface (`putAwayBuffer()`, `setBlocked`, `clearBlocked`), implemented by the Inbound Station (and so the Packing Station) and the Field Head; `loadPutAway` and the lock sweep's re-drain use it too. `WarehouseNetwork.dockRails` returns the farm rail beside a Field Head, so `PutAwayJob.station()` stays the pickup position and the fleet's existing put-away legs work unchanged. A change of the head's To storage buffer posts `WarehouseEvent.Intake` to the head's warehouse, which marks the head pending and wakes parked shuttles (`fleet.roomChanged()`); `tickFields` also marks heads with items pending, so a buffer loaded from a save is put away. Only plain shuttles take put-aways, which the Harvester and Spreader modules already forbid.

The blocked reason (no shuttle, no room, no storage) is kept on the head and feeds the field work status instead of a lamp.

### Field jobs wait for a free slot
The field job check in `WarehouseNetwork.tickFields` adds "the head's To storage buffer has a free slot" before queueing. The pure `FieldWork.Collect` (fine, full, no shuttle, no room) and `FieldWork.whileCollecting` add three statuses after the existing ones (`COLLECT_NO_SHUTTLE`, `COLLECT_NO_ROOM`, `TO_STORAGE_FULL`); they replace any status once the field could be worked at all. "No shuttle" uses `supplyReachable`, a shuttle without a tool that reaches the head's rail.

### GUI
`menu/FieldHeadMenu` adds read-only slots for the To storage buffer after the player inventory (`PackingStationMenu.ReadOnlySlot`, now subclassable), active on the Stock tab only, at most one row of 9 at y 125 right of the "To storage" label, between the Fertilize below row (ends at y 120) and the inventory label (y 143). The open buffer carries the buffer's size (`FieldHeadMenu.writeOpening`), like the Packing Station's. `client/FieldHeadScreen` draws the label, slot frames and the tooltip on the label and empty slots. Lang via `ServusShuttlesLanguageProvider`.

### APIs
No new NeoForge API.

## Risks / Trade-offs

- [A warehouse with only field shuttles never empties the buffer] → The status says it waits for a shuttle; the player needs one plain shuttle, which every warehouse that stores has anyway.
- [Double handling on a single floor farm] → Harvests are minutes apart; the plain shuttle trip does not slow the field.
- [`drain` generalisation collides with `m8-wtp-ingress` edits] → This change is applied after it, on top of its code.

## Migration Plan

Heads saved without the To storage tag load with an empty buffer. A harvester parked with cargo from before unloads at the head on its next run.
