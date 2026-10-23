## Context

Fifth and last change of the shuttle job series (decided 2026-10-23). State after `m8-slot-locks` and `m8-shuttle-following` (applied, not archived): racks and station buffers are `lock/LockedSlots` and implement `LockHolder`; warehouse put-aways and picks lock at planning, `WarehouseNetwork.storeTargets(jobId, cargo, from, ways)` ranks racks by the put-away rule with travel cost and locks IN, leftovers re-route and otherwise park the shuttle `STRANDED` in its garage (`ShuttleFleet.strand`, lamp and GUI line from `strandedOf`), and `sweepLocks` releases orphans of loaded `LockHolder`s among stations and sources.

Field runs (`shuttle/FieldRuns`) were left out:
- The Field Head buffer is `field/SupplyBuffer`: three parts, each an item and an int count, no slots. Players, pipes and hoppers never reach it (no capability). Only supply runs fill it, Harvesters and Spreaders take from it, and Harvesters and Spreaders also put spare seeds or leftover fertilizer back.
- Supply runs plan amounts from unlocked stock (`host.available`) but lock nothing, then walk `sourcesHolding` in index order and take with `host.extractFrom` (normal access path).
- Field put-away uses `host.putAwayTarget` (ranking with cost 0, no locks), inserts with `host.insertAt`, and waits at home in `Leg.WAIT_STORE`, retrying every 20 ticks, without the garage error state.
- `stow` puts cargo overflow back into the source with `host.insertAt` and ignores what the source refuses: those items vanish.
- `addOrDrop` pops harvest drops that do not fit an empty cargo, and a kept seed that cannot be planted, at the plot.

## Goals / Non-Goals

**Goals:**
- The head buffer takes part in slot locks, with the same save, sweep and GUI as racks and stations.
- Supply runs promise both ends at planning; Harvesters and Spreaders promise what they take from the head.
- Field storing uses the warehouse put-away rule with real travel cost and IN locks, re-routes leftovers and parks stranded cargo in the garage with the error state.
- No item vanishes and none drops on the ground on a field run, except where the specs keep it (Recall, lost rail, lost way home).

**Non-Goals:**
- Locking the head buffer for what Harvesters and Spreaders put back (spare seeds, leftover fertilizer): they fill at the head as far as unreserved room allows, and the rest is put away. Nothing is lost when room shrinks.
- Restored field runs still re-decide from where they stand, and a lost field run still drops its cargo at the garage (`m8-job-persistence`, `m8-slot-locks`).
- Plot work, routes on the field and job queuing are unchanged.

## Decisions

### Counted slots for the head buffer
- The head's parts stay counts (a level from data may exceed a stack, and the GUI shows a count table), so a part cannot be closed the way a rack slot is: an IN lock of a supply run would stop the Harvester from taking seeds for the whole trip. `LockedSlots` gains a counted mode (`new LockedSlots(size, true)`): a slot may hold IN and OUT locks of several jobs at once (`lock` keeps the others), an insert by anyone but a job holding an IN lock there is limited to the capacity less the count and less the IN amounts of other jobs, and an extract is limited to the count less the OUT amounts of other jobs. The acting job's own reservation is left out of both sums. The arithmetic is pure: `SlotFill.sharedRoom(limit, count, reservedIn)` and `SlotFill.sharedAvailable(count, reservedOut)`, JUnit tested. Normal (rack and station) slots behave exactly as before. Alternative rejected: a separate lock type for the head, which would need its own save, sweep, payload and tooltip.
- New `LockedSlots` methods: `reserved(slot, kind, exceptJob)`, `insertAs(jobId, slot, stack)` and `extractAs(jobId, slot, amount)` (one slot, release the job's lock of that direction on that slot), `insertAllAs(jobId, stacks)` (several items of one job into one inventory: each stack only into IN slots locked for its item, released once at the end; the single `insertAs(jobId, stack)` released every IN lock of the job after the first stack and would let a stack fill a slot locked for another item) and `extractAllAs(jobId)` (one stack per item; `extractAs(jobId)` merged items of different kinds into one stack).
- `SupplyBuffer` keeps its API and stores its parts in a counted `LockedSlots(3)` whose capacity is unbounded (the head's level caps it), so counts above a stack still work. It saves in the old format (`seeds`, `fertilizer`, `anti_corrosion` with `Item` and `Count`), plus `Locks` (the `saveLocks` list) when any lock exists. A 0.3.0 tag has no `Locks` and loads unlocked. Loading writes the slots without calling the change hook. New methods: `insertAs`, `takeAs`, `lockIn`, `lockOut`, `available`, `reservedIn`, `release`, `slots()`.
- `FieldHeadBlockEntity implements LockHolder` (`lockedSlots()` is the buffer's slots). `supplyRoom` leaves out reserved room; new `supplyAvailable(part)` (items no job reserved), `fillSupplyAs`, `takeSupplyAs`, `lockSupplyIn`, `lockSupplyOut`, `releaseSupply(jobId)`, and a public `warehouse()` for the menu.

### Job ids and where a field run holds locks
- A field run's job id is the id of the shuttle's lead job (`FieldJob`, `FertilizeJob`, `SupplyJob`), so the fleet's `knows(id)` keeps its locks alive for the orphan sweep and `shuttleOnJob` labels them.
- `FieldRun.locked` (ordered set of block positions) records every block where the run placed a lock: the head, supply sources, store stops. `FieldRuns.release(shuttle)` calls `host.releaseLocks(jobId, locked)` when a run ends normally, and `ShuttleFleet.dropFieldRun` (park, recall, lost way) calls it before dropping the cargo.
- `sweepLocks` also visits the serving Field Heads on the warehouse's rails (`servedHeads()`, shared with `tickFields`).

### Supply runs
- Planning (`startSupply` → `planSupply`): amounts per part as before (`SupplyPlan.toBring` over unlocked stock above the reserve and the planned cargo room, low parts first), with the part's held amount being its level less its unreserved room. Then `host.lockStock(jobId, key, amount, headRails, ways)` locks OUT in racks by `RequestPlan.plan` order (cheapest by travel cost from the head's rail, reachable only, one entry per rack) and returns the racks and what it locked; the head part is locked IN for that amount. The run keeps `sources` (racks to visit, in that order); `keys`/`remaining`/`reserves`/`sourcesTried` go.
- Fetch: drive to the first source; a source without a way loses its locks and is skipped. At the rack `host.takeLocked(source, jobId)` (`extractAllAs`) gives one stack per item; `stow` adds it to the cargo, what the cargo does not hold goes back into the rack (`insertAt`) and what the rack refuses joins the stacks to put away. The split is the pure `FieldCargo.stow(cargo, entry, putBack)` (JUnit).
- At the head: stale items come aboard (unchanged), then each cargo item fills its part as the job (`fillSupplyAs`, releasing the IN lock of that part), then the run releases its remaining head locks. The rest is put away.
- `SupplyRun` saves `Sources`; every run saves `Stops` and `Locked` (long arrays). Missing keys read as empty. A run saved before this change has none and goes on without locks.

### Harvester and Spreader fetches
- The Harvester decides its seed fetch in `turn` from `supplyAvailable(SEEDS)`; `fetchSeeds` locks that many OUT (`lockSupplyOut` returns what it got) and `loadSeeds` takes them as the job (`takeSupplyAs`). Without a way to the head the lock is released at once.
- The Spreader plans `toFetch` from `supplyAvailable(FERTILIZER)` and locks it OUT the same way; `loadFertilizer` takes as the job.
- Spare seeds and leftover fertilizer still fill the head as plain inserts, which now see the room less the IN reservations of a supply run.

### Field storing
- `FieldRun.stops` (racks locked for the run's store stacks, in the order to visit) replaces `storeTarget`. `continueStore`: when no stop is left and stacks remain, plan every stack with `host.storeTargets(jobId, stack, {here}, fleet.ways(shuttle))`, which ranks by the put-away rule with the travel cost from where the shuttle stands, only reachable racks (a liftless shuttle's ways leave out lift steps), and locks IN; then drive to the first stop (`TO_STORE`). A stop without a way loses its locks and is dropped. At the stop `host.unloadAllInto(stop, jobId, store)` (`insertAllAs`) and the stop is removed; leftovers stay in `store` and are planned again.
- Nothing planned and stacks left: `ShuttleFleet.strand(shuttle)` parks the shuttle in its garage as for warehouse leftovers (reason from `anyRoomFor` over the field run's stacks as well). `retryStranded` hands field shuttles to `FieldRuns.continueStore` instead of `continueStores`. `Leg.WAIT_STORE`, `FieldRun.waitTimer` and `STORE_RETRY_TICKS` go; a stranded field shuttle is counted by `strandedOf` (it is busy with its field job) and shown by the garage like any other.
- `Host.putAwayTarget`, `sourcesHolding` and `extractFrom` leave the fleet's host interface (field runs were their last users); the network keeps the public methods for commands and GameTests. Removed: `storable`, `anyStorable`, `insertAll`, `handsOverAtOnce` (put-away targets and supply sources are racks with a way only).

### No drops on the field
- `addOrDrop` becomes `addOrStore`: what the cargo cannot hold joins the stacks to put away, and `work` treats any such stack like a full cargo (put away, then come back). A seed kept for replanting that cannot be planted goes the same way.

### Field Head GUI
- `MenuLocks.ofInventory(LockedSlots)` maps virtual menu slots 0..n-1 to the inventory's slots; `FieldHeadMenu` implements `LockedMenu` and ticks it in `broadcastChanges` with `head::warehouse`. The client copy is the client head's buffer.
- `FieldHeadScreen` washes the buffer cell (`HEAD_Y` row) of a part with locks, IN or OUT colour of the first lock, and appends `LockOverlay.line` per lock to the buffer tooltip. `LockOverlay` exposes `washIn()`, `washOut()` and `line`.

## Risks / Trade-offs

- [A supply run's IN lock is larger than what arrives when a rack became unreachable] → it fills what it carries and releases the rest at the head.
- [Field shuttles stranded now wait in the garage and retry every 100 ticks or when room may have appeared instead of every 20 ticks] → the same rule as warehouse leftovers; `roomChanged` fires on every lock release and source shrink.
- [A Harvester stranded with cargo keeps its field job, so the field gets no other harvester until the cargo is stored] → as the spec says, it takes no other job; the field is worked again once the cargo is stored.
- [Fixed tick field GameTests may see other racks chosen now that cost counts] → checkpoints and rack placement in tests are adjusted, assertions stay.

## Migration Plan

No migration. Head tags without `Locks`, field run tags without `Stops`, `Sources` and `Locked` load as unlocked and empty. A field shuttle saved in the retired `WAIT_STORE` leg (only by unreleased builds of this series) reads as `STRANDED` and resumes through its run, which plans its put-away again. 0.3.0 saves have no fleet state at all.

## Archive order

Last of the series: after `m8-shuttle-numbers`, `m8-job-persistence`, `m8-slot-locks` and `m8-shuttle-following`. The `shuttles/slot-locks` delta modifies requirements that `m8-slot-locks` adds; `openspec validate` notes that the archive would refuse it until that change is archived.

## Deviations found while applying

- The cargo overflow of a supply run's take cannot happen in a normal run any more (the run locks only what its planned cargo room holds), so no GameTest provokes it; the put back and keep rule is covered by JUnit (`FieldCargoTest`, `FieldCargo.stow`).
- The Field Head has no hand, pipe or hopper access at all, so "a hand cannot fill a locked slot" has no head counterpart; `FieldLockGameTests.headPartsKeepTheirReservations` checks that reserved room takes no other insert and reserved items no other take, and that the locks survive saving.
- `fillSupplyAs` fills up to the part's level, not only up to the reserved amount; the run only ever carries what it reserved.
- Root `build.gradle` was given a temporary `gtTests` property to run single GameTests while applying; it is not committed.

## Assumptions

- The buffer's IN wash has no ghost item: the buffer is a count table, not a slot, and the column header already shows the item; the tooltip names the incoming item and amount.
- A supply run counts the source cost from the farm rail beside the head (the destination), like picks count from their destination.
- Harvesters and Spreaders keep putting spare seeds and leftover fertilizer back into the head without a lock (see Non-Goals).
