## Context

See proposal.md for the bug and the decisions (shuttle job series, decided 2026-10-23). State after `m8-job-persistence`: every `ShuttleJob` has an `int id()`, `ShuttleFleet.knows(id)` tells whether a job is queued or carried, `WarehouseNetwork.sweepClaims` releases Inbound Station claims through the pure `OrphanSweep` while the site is online and loaded, and fleet state is saved through the node `Warehouse` tag. `ShuttleItem.label(stack)` (from `m8-shuttle-numbers`) names a shuttle "Shuttle #12" or "Bertha (#12)".

Today the inventories involved are plain Foundation `ItemSlots` (a NeoForge 26.1 `ItemStacksResourceHandler`). Every access path ends in `StacksResourceHandler.insert(int, ItemResource, int, TransactionContext)` or `extract(int, ...)`: the item capability, `ResourceHandlerSlot.mayPickup` (a simulated extract), `ResourceHandlerSlot.mayPlace` (`isValid`), and `AbstractContainerMenu.moveItemStackTo`, whose merge branch checks `Slot.getMaxStackSize(stack)` = `getCapacityAsLong`, which is 0 for a resource `isValid` refuses. Only `ItemSlots.setStackInSlot` and the menu's `IndexModifier` (`handler::set`) write without a check, and the menu only reaches them after `mayPickup`/`mayPlace` passed.

Paths are found by BFS (`RailPaths.find`); the fleet takes the free shuttle with the fewest nodes; put-away targets are the first tier entry with any room (`firstTarget` + `hasRoom`); picks walk `index.sourcesHolding` in index order (`RequestPlan`).

## Goals / Non-Goals

**Goals:**
- Locks enforced at the slot storage, so every access path obeys them.
- Planning that reserves room and stock, so arrivals always fit.
- Travel time as the one cost for paths, dispatch, targets, sources and Packing Station choice.
- No item ever popped by a leftover; stranded cargo parks in the garage, saved.

**Non-Goals:**
- Field runs (harvest, fertilize, supply) do not lock; they only see unlocked room and stock. The Field Head buffer joins in `m10-field-head-locks`.
- Fluid Gantries keep their counters.
- Following and gaps between shuttles (`m8-shuttle-following`); aisle traffic in the cost.
- Recall and a lost home rail keep dropping cargo at the garage, as the garage spec says; so does a broken lift shaft for its riders.

## Decisions

### Lockable slots
- `shuttles/lock/LockedSlots extends ItemSlots` (Foundation unchanged, so Alchemy needs no rebuild). It keeps a `SlotLock[]` and overrides `insert(int, ItemResource, int, TransactionContext)`, `extract(int, ...)` and `isValid(int, ItemResource)`: a locked slot answers 0 / false unless the slot's job is the one acting. `asJob(jobId, action)` sets the acting job around an owner's insert or extract (server thread only, restored in `finally`). Methods: `lock`, `release(jobId)` (all slots of that job, returns whether any), `lockOf(slot)`, `insertAs(jobId, stack)` into that job's IN slots, `extractAs(jobId)` from its OUT slots, `freeSlots()`, `states()` for the pure planner, `saveLocks`/`loadLocks` as a `Locks` list of `{Slot, Kind, Item, Amount, Job}`, and an `onLocksChanged()` hook (blocks call `setChanged`).
- Alternative rejected: locks in Foundation `ItemSlots` (an API change for every mod) or a wrapper handler per side (menus and `insertItem` helpers would still reach the raw slots).
- `SlotLock(Kind kind, ItemKey key, int amount, int jobId)`, `Kind { IN, OUT }`. A slot holds one IN lock or several OUT locks of different jobs whose amounts add up to no more than the slot holds, so a pick of 8 from a slot of 64 does not hide the other 56 from the next pick (found when the controller wireless tests requested twice from one slot). `SlotFill.Slot` carries the reserved OUT amount; an IN locked slot gives nothing.
- Users: `HeavyRackBlockEntity.items`, `InboundStationBlockEntity.buffer` (and so the Packing Station To storage buffer), `PackingStationBlockEntity.outgoing`, `OutboundStationBlockEntity.buffer`. `DeliveryTarget.deliveries()` returns `LockedSlots`. Blocks implement `LockHolder` (`List<LockedSlots> lockedSlots()`) for the sweep and the menus.
- Raw writes audited: Outbound `pushInto` and Packing `returnOrphans` skip locked slots; `HeavyRackBlockEntity.clear` and drops stay raw (block removal).
- Station claims (`claimedBy[]`, `Claims` tag) are replaced by OUT locks. `Claims` was never released (added by the unreleased `m8-job-persistence`), so an old tag with it is ignored; 0.3.0 tags have neither and load unlocked.

### Pure planning (no Minecraft imports, JUnit)
- `lock/SlotFill<K>`: over `Slot<K>(K key, int count, int limit, boolean locked)`: `in(slots, key, amount)` tops up unlocked partial stacks of the key, then unlocked empty slots; `inEmpty(slots, amount, max)` only empty slots (destination buffers, so a partial slot of the destination never blocks its extraction); `out(slots, key, amount)` takes from unlocked slots holding the key, smallest first; `room`, `freeSlots`, `available`.
- `zone/PutAwayRank<S>`: `Bay<S>(S id, int tier, boolean holds, boolean partial, int freeSlots, int slots, int room, double cost, int joinOrder)`; `order(bays, bucketStep)` sorts by tier, then holding bays (partial first, then cost) before the others (free slot bucket `floor(free / ceil(slots * step))` descending, then cost), join order last; bays without room are dropped.
- `shuttle/RailCosts`: Dijkstra over `(previous, node)` states so a step's cost can depend on the turn; multi start; `Search.costTo(node)`, `cheapest(goals)`, `pathTo(node)`. `shuttle/TravelWeights(railBlock, corner, liftBlock, handover, stop, liftWaitPerJob)`: `step(prev, from, to, lifts)` = lift block for a vertical step, plus handover and `lifts.waitAt(shaft, floorY)` when the vertical step starts a ride, rail block for a horizontal step plus corner when the heading turns.
- `RequestPlan.Stock` gains `cost`; `plan` sorts by cost then input order, counts only unlocked stock (given by the caller).

### Travel cost wiring
- `warehouse.json` `travel_costs`: `rail_block` 7 (1 / 0.15 base speed), `corner` 5 (corner speed 0.1 plus braking), `lift_block` 10 (1 / `lift_speed` 0.1), `lift_handover` 20 (two `lift_handover_ticks`), `stop` 14 (`stop_ticks` 10 + `stack_ticks` 4), `lift_wait_per_job` 50 (an average ride with handovers); `free_slot_bucket` 0.25. Read by `ShuttlesStatsManager.travelWeights()` / `freeSlotBucket()`.
- Lift wait per search: `ShuttleFleet` snapshots each shaft once per search (`LiftLoad`: car y, queued calls, busy shuttles whose remaining path enters the shaft), so the wait at a floor is `|carY - floorY| * liftBlock + jobs * liftWaitPerJob`.
- `ShuttleFleet.path(shuttle, from, goals)` replaces `RailPaths.find` for every leg a shuttle drives (warehouse legs, home, restore, field runs leaving or returning; `FieldRoute` on the field stays). `nearest` searches once per free shuttle per assignment pass. `ShuttleFleet.costsFrom(starts)` serves the network: put-away targets (from the station's dock rails), pick sources (from the destination's dock rails), Packing Station choice. The stop weight is added per stop when a pick source and a Packing Station are compared, so it does not change rankings between bays of one kind. `RailPaths.find` stays for reachability checks only.
- Fluid routing keeps its rail step distance.

### Placing and releasing locks
- Put-away joining (`joinOpenPutAway`): a buffer stack of an item that a put-away from the same station has not loaded yet joins that job, up to a stack per job, by growing the job's IN locks in its bay (`LockedSlots.extendIn`) and locking the new station slot OUT for the same job. Without this a buffer filling item by item (packets, slow pipes) became one job per item and one shuttle trip per two items (found by the Packing Station GameTests).
- Put-away (`WarehouseNetwork.drain`): for each unlocked buffer stack, rank tier candidates with `PutAwayRank` (cost from one search per station per drain), skipping racks no shuttle carries to; lock IN in the first bay (`SlotFill.in`, up to the stack), lock OUT on the station slot for the same amount, queue `PutAwayJob(id, station, slot, key, amount, target)`.
- Pick (`startJobs`): stock per bay is the bay's unlocked count, cost from the destination; each planned job locks OUT in its bay right away and IN in the destination when an empty unlocked slot exists. `ShuttleFleet.assignJobs` asks `Host.readyToSend(pick)`, which locks the destination slot when it is missing; a pick that is not ready is skipped this pass and waits, later jobs go ahead.
- Outbound restock asks for at most `freeSlots * maxStack` minus nothing else, so every planned pick gets a slot.
- Loading: `Host.loadPutAway(job)` and `Host.loadPick(job)` extract as the job and release its OUT locks there. Unloading: `Host.unloadInto(target, job, cargo)` inserts as the job into its IN slots and releases them; `deliverTo` does the same at the destination. Anything left over is a leftover.
- Release without load or unload: `Host.releaseLocks(job)` at every block the job names (station, target, source, destination, extra stops), called on refuse, request removal, dropped cargo and replans. A requeued job keeps its locks.
- Orphans: `sweepClaims` becomes `sweepLocks` over every loaded `LockHolder` among stations and sources, same `OrphanSweep` rule, keyed by `(pos, inventory, slot, jobId)`. It also catches locks of jobs lost by a crash between chunk saves.
- Broken bay: `onNodeRemoved` of a rack that leaves for good asks the fleet to drop queued and not yet loaded picks from it (`dropPicksFrom`), releases their destination locks and plans them again with the same request and destination; what cannot be planned counts as missing. Put-aways that were to fill it re-route at arrival (their lock vanished with the bay).

### Leftovers and the parked state
- `Load` gains `stops`, the store targets a load still visits (a put-away starts with its target; a re-route adds the bays it locked). The put-away legs are TO_SOURCE, TO_TARGET per stop, HOME; TO_STATION is used by tanker runs only. A shuttle drives to the first stop of its first load with cargo; at arrival every load whose stop it is unloads there.
- Leftover (`ShuttleFleet.reroute`): `Host.storeTarget(job id, cargo, from, reachable)` ranks with the put-away rule from where the shuttle stands, locks IN in as many bays as needed and returns them in rank order. A pick whose destination is gone or no `DeliveryTarget` becomes a store load the same way and counts the cargo as missing (`pickDropped`).
- Nothing found: leg `STRANDED`, the shuttle drives home and parks (motion arrived at home, still busy, not drawn: `views` skips stranded shuttles at home). `Shuttle.strandReason` is `NO_ROOM` or `NO_WAY`. The fleet retries stranded shuttles every 100 ticks and on `roomChanged()`, which the network calls when zones or members change, locks are released, or polling sees a source shrink. The garage of a stranded shuttle refuses to lend it out (it is busy).
- Restore: `resume` re-routes a shuttle whose leg goal is gone, and when no way is left, or the shuttle is off the rails, parks it stranded in its garage with its cargo instead of `recallShuttle`. Field runs keep their old end.
- `WAITING_ROOM` is no longer entered; a saved shuttle in it resumes as TO_TERMINAL.
- `FleetSaves` saves `Stops` per load and `Strand` per shuttle; missing keys read as none.

### Garage error state
- `ShuttleGarageBlock` gains `StorageNodeBlocks.ACTIVITY` (only BLOCKED is drawn: a red lamp on the front, new texture `shuttle_garage_front_blocked`, a recolour of `shuttle_garage_front_online`, flat face, no new geometry). The network sets it from `ShuttleFleet.stranded(garage)` after each fleet tick when it changed.
- `GarageStrandedPayload(containerId, List<Component> lines)` synced by `ShuttleGarageMenu` through `PayloadSync`; each line reads "<label> holds <n> <item>: no room|no way". `ShuttleGarageScreen` draws the first line in the alert colour above the slots (cut to the panel width), all lines as a tooltip on that line, and a red wash on the slot of a parked shuttle.

### Lock display
- `SlotLocksPayload(containerId, List<Entry(slot, in, item, amount, Optional<Component> shuttle)>)`, registered in `ServusShuttlesPayloads`. `menu/MenuLocks` maps menu slot indexes to `(LockedSlots, index)`, builds entries on the server (the shuttle label from `WarehouseNetwork.shuttleOnJob(jobId)`), and syncs every 5 broadcasts on change. Menus: `HeavyRackMenu`, `InboundStationMenu`, `OutboundStationMenu`, `PackingStationMenu` implement `LockedMenu.acceptLocks`.
- On the client `MenuLocks.accept` also copies the synced locks onto the menu's client inventories (`LockedSlots.copyLocks`), so `ResourceHandlerSlot.mayPlace`/`mayPickup` refuse there too and the client does not predict a move the server refuses; menus whose client side has no block entity use `LockedSlots` stand-ins.
- `client/LockOverlay` draws from an `extractSlots` override after the slot items (26.1 draws labels before slots): IN wash `0x6040A0FF`, OUT wash `0x60FF9A2E`, an empty IN slot gets `fakeItem` under a stronger wash so it reads faded; an `extractTooltip` override sets the item name and a line per lock with `setComponentTooltipForNextFrame`. Lang: `gui.servus_shuttles.lock.in` "Reserved: %s brings %s %s", `.out` "Reserved: %s takes %s %s", `.in_waiting` "Reserved: brings %s %s, waiting for a shuttle", `.out_waiting` "Reserved: takes %s %s, waiting for a shuttle".

### Small fixes
- `ShuttleFleet.refreshHome` compares homes with `Objects.equals`.
- `InboundStationBlockEntity.pullOneStack` moves the stack inside one transaction (`Transfer.open()`): extract from the neighbour and insert into the buffer, commit only when the buffer took all of it; with locks the simulated room already leaves out locked slots.
- `PackingStationBlockEntity.shipWireless`: what the receiver did not take goes back into To network (the slots it just left are unlocked, so it fits); should To network refuse part of it, that part goes into To storage, and only what neither takes drops at the station, so it can never vanish.

## Risks / Trade-offs

- [A pick waits for a destination slot while a big shipment fills the To network buffer] → later jobs go ahead; the Packing Station sends at its rate, freeing slots.
- [Dijkstra per assignment pass costs more than BFS] → one search per free shuttle per pass, one per station per drain, one per destination per plan; lift loads are snapshotted per search.
- [Locked slots look stuck to players] → wash, ghost and tooltip name the job and the shuttle.
- [A put-away whose bay takes only part of the stack leaves the rest locked in the station slot until the shuttle loads] → the rest is planned right after loading.
- [Fixed tick GameTests may see different paths where several ways exist] → checkpoints move later where travel got longer; assertions stay.
- [Recall still drops cargo] → it is the player's explicit way to empty a garage; the parked state covers everything automatic.

## Migration Plan

No migration: missing `Locks`, `Stops` and `Strand` read as empty. Worlds from Shuttles 0.3.0 load with everything unlocked; jobs planned after load lock as usual.

## Deviations found while applying

- Several OUT locks may share a slot (see Lockable slots); the spec says so.
- More of an item joins a put-away no shuttle has loaded yet (see Placing and releasing locks); the spec says so.
- The "straight way over corners" check is covered by JUnit (`RailCostsTest`, `TravelWeightsTest`), not by a GameTest; the GameTests cover the put-away rule and the cheapest pick source.
- `available(key)` counts a rack whose chunk is not loaded by its last index snapshot, as before; loaded racks count only unreserved items.
- Root `build.gradle` was given a temporary `gtTests` property to run single GameTests while applying; it is not committed.

## Assumptions

- Destination buffers lock whole empty slots (never top up a partial stack there), so a partial stack waiting to leave a Packing Station is never blocked by an incoming pick.
- Field runs use only unlocked room and stock and place no locks; their put-away targets follow the new ranking.
- The garage line is cut to the panel width; the full text is in its tooltip.
- `stop` is part of the weights for completeness; within one ranking every candidate has one stop, so it only matters when legs of different length are summed.

## Open Questions

- None for this change. `m8-shuttle-following` adds traffic to the cost and draws parked shuttles inside the garage; `m10-field-head-locks` makes the Field Head buffer a `LockHolder`.
