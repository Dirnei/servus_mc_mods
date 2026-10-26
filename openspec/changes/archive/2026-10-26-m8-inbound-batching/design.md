# Design: m8-inbound-batching

## Context

See proposal.md, Why. The relevant code as of main `8cecf03d`:

- `foundation/.../logistics/network/ItemChannelNetwork.java`
  - `onTick` (l. 100-118): every `pull_interval` ticks each Pull endpoint calls `pull(source, items_per_pull)`. Values from `foundation/src/main/resources/data/servus_foundation/duct/utility_duct.json`: `items_per_tick` 16, `items_per_pull` 8, `pull_interval` 8, read through `DuctStatsManager`.
  - `pull` (l. 124-154): simulates `route` for up to 8 items of the first slot the filter and keep amount allow, extracts what was accepted, routes it for real.
  - `route` (l. 163-199): caps the stack at the tick `budget`, asks `RoundRobinPlan.plan` for offers (capacity is called lazily, group by group, so only reached targets are asked), inserts each offer with `insertAnySlot`.
  - `insertAnySlot` (l. 231-237): slot 0 upward, the first slot that takes the item wins. A partial stack in slot 3 loses against an empty slot 0.
- `shuttles/.../station/InboundStationBlockEntity.java`
  - `buffer` is a `LockedSlots` of `inbound_buffer_slots` (9, `shuttles/warehouse.json`); every content or lock change posts `WarehouseEvent.Intake` (l. 46-58, 263-265).
  - `intake` (l. 65-108) passes inserts to `buffer.insertItem(slot, ...)`; since `m8-rule-cap` it clamps by `capRoom` with `pendingInserts`.
  - `pullOneStack` (l. 173-196) inserts with `buffer.insertItem(stack, ...)`, slot 0 upward.
- `shuttles/.../lock/LockedSlots.java`
  - `insert` (l. 430-438): a non counted slot with any lock of another job refuses every insert (`blocked`).
  - `lock` (l. 91-102): an OUT lock joins other OUT locks on the slot.
- `shuttles/.../network/WarehouseNetwork.java`
  - `handle` (l. 1631-1638) queues the station on `Intake`; the tick drains it with `drain` (l. 1925-1979), `put_away_per_tick` (1) stacks per tick.
  - `drain` skips every locked slot (l. 1935), joins an open put-away (`joinOpenPutAway`, l. 1986-2003) or plans a new `PutAwayJob` and OUT locks the slot for the stack's count (l. 1964-1965).
  - `joinOpenPutAway` locks the new slot for the same job, up to a stack per job, and grows the rack IN lock with `extendIn`.
  - `loadPutAway` (l. 1382-1384) takes `extractAs(jobId)`, every OUT lock of the job, and releases them.
  - `capRoom` (l. 1258-1272) counts index, put-away cargo and `InboundStationBlockEntity.held(key)`.
  - `grantLock` (l. 1891-1893) keeps Packing Station grant locks from the orphan sweep.
- `ShuttleFleet.openPutAway` (l. 594-612): a put-away is open while queued or while its shuttle drives to the station with empty cargo.

### Root cause

Each pipe batch gets its own slot because a planned put-away closes its slot:

1. A pull moves 8 cobblestone; `insertAnySlot` puts them into the first empty slot, say slot 0. The buffer posts `Intake`.
2. Within the same or the next tick `drain` plans a put-away and OUT locks slot 0 for 8 (`WarehouseNetwork.java` l. 1964).
3. Eight ticks later the next 8 arrive. `LockedSlots.insert` refuses slot 0 because it holds another job's lock (`LockedSlots.java` l. 437, `blocked`), so `insertAnySlot` moves on to slot 1.
4. `drain` finds slot 1 unlocked, `joinOpenPutAway` joins it to the open job while the shuttle has not loaded (l. 1943), and OUT locks slot 1 as well. Repeat.

So the trip count is mostly fine already: the batches join one job of up to a stack, as long as the shuttle has not loaded. The slots are not: a stack of 64 takes 8 of the 9 slots, and the main spec even says so (`shuttles/slot-locks`, scenario "Pull side cannot top up"). Two things make it worse: when the shuttle arrives before the stack is complete it leaves with a part (the rest is a second trip), and `insertAnySlot` and `pullOneStack` prefer an empty low slot over a partial stack of the same item.

The cap clamp from `m8-rule-cap` is not involved: it limits amounts, not slots.

## Goals / Non-Goals

**Goals:**
- One slot per stack for every inserter (our pipe, hoppers, other mods' pipes, the Pull side).
- One trip per stack for our Item Pipe, by announcing what is coming.
- Foundation stays independent of Shuttles; no new capability.
- Pipe throughput and balance numbers unchanged.

**Non-Goals:**
- Announcing pushed items or items from other mods' pipes.
- A settle delay for non announced inserts (see Decision 1, alternative B).

## Decisions

### 1. Top up a slot whose put-away is not loaded (root cause fix)

`LockedSlots` gets a top up mode, switched on only for the Inbound buffer: a slot whose locks are all OUT locks takes inserts of the item it holds, up to its limit, from every inserter, and still refuses extracts by anyone but the job. An OUT lock only lives until its shuttle loads (`extractAs` releases it), so "all OUT locks" is exactly "put-away planned, not loaded". IN locks (pipe grants, Decision 2) stay closed.

`drain` no longer skips a locked slot outright. It looks at each slot's unreserved surplus (`count` minus the OUT locks on it, pure `SlotFill.unreserved`), and for a surplus:
- grows the OUT lock of the open put-away that already holds this slot, up to a stack per job, and extends that job's rack IN lock with `extendIn`;
- else joins another open put-away of the item (`joinOpenPutAway`, unchanged rule);
- else plans a new put-away for the surplus, a second OUT lock on the same slot, which `lock` already allows.

When a shuttle loads, it takes its job's amount; what is left in the slot is surplus and goes the same way. A slot whose shuttle is on its way therefore keeps filling, and the shuttle takes whatever its job grew to by the time it loads (never more than a stack). A new pure helper `lock/TopUp.join(jobTotal, surplus, stackSize)` returns how much of the surplus joins; JUnit covers it.

Alternatives:
- **B, settle delay**: plan a put-away only after a slot got nothing for N ticks or is full. Simple, but it delays every hopper and hand insert, and adds a balance number. Rejected as the main fix; the grant (Decision 2) gives the waiting where we know an amount is coming.
- **C, counted mode for the buffer**: reuse the Field Head's counted slots. They also open extracts and IN reservations to sharing, which the Inbound Station does not want. Rejected.

### 2. The announce: reuse the WTP packet endpoint

Options weighed:

- **(a) The pipe offers its whole available amount in one insert.** Raising `items_per_pull` to a stack, only toward warehouse machines. Fast and simple, but it changes the pipe's rate in one case only, the duct would have to know what a warehouse machine is, and the station still could not tell a full stack from a part. Rejected.
- **(b) A new Foundation interface** (for example `IncomingItems.announce(item, amount)`) that the duct calls and the station implements. It tells the station, but the station would then need its own reservation, expiry, reload and sweep rules, which are exactly what the WTP grant already has. Rejected as a duplicate.
- **(c) Reuse the WTP handshake** (`PacketEndpoint`, `Grant`, `GrantBook` in `foundation/.../data/packet`, from `m8-wtp-ingress`). The station registers the existing `servus_foundation:packet_endpoint` capability; the pipe offers, holds the grant and fills it with `Grant.deliver`; the station reserves one slot with an IN lock under the grant's ticket; `drain` skips IN locked slots, so the slot is put away only after the grant is filled or ends. **Recommended.** It is the same promise the Packing Station already makes to the terminal, Foundation stays ignorant of Shuttles, and `Grant` already supports a sender that fills and closes a grant itself.

How the pipe uses it (`ItemChannelNetwork`):
- Only on the pull path. `pull` knows the source and can count it (`heldCount` already exists for keep amounts). `route` gets a `PullContext` (source endpoint, item) or null for pushes.
- In the lazily called `capacity` for a target endpoint during a pull, the network looks up the `PACKET_ENDPOINT` capability through a second `BlockCapabilityCache` on the endpoint (same position and face as the item cache). With an endpoint and no open grant for (target endpoint, item), it offers `PipeOffer.amount(movable, stackSize, receiveAllowance)`: what the source can give of the item after the keep amount, at most a stack, at most the side's fill up to allowance. A granted grant makes the capacity `grant.left()`; a refusal falls back to today's capacity, so the cap (which refuses) still lets the duct fall through to a lower priority target as `m8-rule-cap`'s "Duct falls back" scenario needs.
- The real pass inserts into a granted target with `grant.deliver(stack, gameTime)` instead of `insertAnySlot`.
- Grants are kept per (target endpoint, item) with the set of source endpoints that fed them. After each pull cycle the network closes a grant that is filled, whose sources no longer give the item (count after keep is 0), or whose endpoints are gone (`endpointsChanged`). `onDisposed` closes all. The station's own `GrantBook.sweep` with `grant_hold_ticks` is the fallback for a lost network.
- Because `RoundRobinPlan.plan` asks capacity group by group, only targets the plan reaches are offered, so a low priority station is not reserved for nothing.

How the station answers (`InboundStationBlockEntity implements PacketEndpoint`):
- `offer`: refuse offline, when `putAwayTarget` finds no storage, when no empty unlocked slot exists, or when `capRoom` leaves 0. Otherwise reserve one empty slot with an IN lock for `min(amount, stack, capRoom)` under `nextJobId()` as ticket.
- `receive`: `buffer.fillAs(ticket, stack)` for a held grant.
- `close`: release the IN lock; the lock change posts `Intake` and `drain` plans one put-away.
- The capability is registered only for sides that are not Off and not the front, like `handlerFor`.

The grant code of `PackingStationBlockEntity` (`offer`, `receive`, `close`, `holdsGrant`, `dropGrantLocks`, the sweep in its tick) moves into a shared `station/BufferGrants` helper over a `LockedSlots` and a slot planner, used by both stations (code style: extract instead of copy). The Packing Station keeps its whole-slots-per-grant rule; the Inbound Station plans one slot. `WarehouseNetwork.grantLock` asks any `BufferGrants` holder instead of only the Packing Station.

One slot per grant, not several like the Packing Station: a pipe with a full chest behind it would otherwise reserve the whole buffer for minutes at pipe rate and shut out hoppers, and a filled slot would wait for the last one before any shuttle starts. With one slot, the first stack starts its trip while the second fills.

### 3. Stacked inserts

`ItemChannelNetwork.insertAnySlot` and `capacity` use `ItemView.insertItemStacked` (NeoForge `ResourceHandlerUtil.insertStacking`, already used by `foundation/.../transfer/Transfer.insertItemStacked`). `pullOneStack` uses `buffer.insertItemStacked`. This changes delivery order into every inventory, which is the intended new duct requirement "Item pipe fills partial stacks first".

### 4. The cap counts grants

`InboundStationBlockEntity.held(key)` adds what the station's open grants of the key still expect (`grant.left()`). `WarehouseNetwork.capRoom` already sums `held` over all Inbound Stations, so hoppers, Pull sides, other offers and `m8-rule-cap`'s `pendingInserts` clamp all see it. Items filled into a grant go through `receive`, not `intake`, so they are not clamped twice; they were clamped when the grant was made. When a grant ends short, `left()` drops to 0 and the room is free again.

### 5. Showing the announced amount

The GUI reuses the lock overlay: the grant's slot is IN locked, so it already gets the wash and the faded item. `SlotLocksPayload.Entry` gets a `pipe` flag set server side when the lock id belongs to an Inbound pipe grant, and `LockOverlay.line` then reads "Reserved: pipe brings {left} more {item}" (lang key `gui.servus_shuttles.lock.in_pipe`). The amount sent is the lock amount minus what the slot holds. A Data Potato report on the station is left to the Data Potato open point.

### NeoForge APIs used

- `net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(BlockCapability, ServerLevel, BlockPos, Direction)` for the second, endpoint cache per duct endpoint (already used for `Capabilities.Item.BLOCK` in `ItemChannelNetwork.endpoint`).
- `ServusFoundationCapabilities.PACKET_ENDPOINT` (`BlockCapability.createSided`), registered with `RegisterCapabilitiesEvent.registerBlockEntity` in `ServusShuttlesEvents` as for the Packing Station (l. 77).
- `net.neoforged.neoforge.transfer.ResourceHandler<ItemResource>`, `transaction.Transaction`, `ResourceHandlerUtil.insertStacking` through Foundation's `Transfer` and `ItemView`.
No new NeoForge API; every call above is already used in the repo.

### Pure logic and tests

- Pure, JUnit: `logistics/duct/PipeOffer` (Foundation), `lock/TopUp` and `SlotFill.unreserved` (Shuttles), the one-slot plan for the Inbound grant (`SlotGrant.plan` given at most one free slot).
- Minecraft, GameTest: top up in `SlotLockGameTests`, the user's case and cap interplay in a new `shuttles/.../gametest/InboundBatchingGameTests`, stacked insert in a new Foundation `logistics/gametest/DuctStackingGameTests`.
- Manual: play check of the GUI tooltip and the trip count in the client.

### Apply order

After `m8-rule-cap` is archived (it owns the intake clamp a fix agent is changing now, and `capRoom`), and after `m5-duct-endpoint-gui` and `m5-duct-keep-amounts` are archived (they own `ItemChannelNetwork.pull`, `DuctMode` and the keep amounts this change reads). The utility duct delta uses ADDED requirements so it does not collide with `m5-duct-endpoint-gui`'s MODIFIED "Item channel behaviour". `m8-rule-export` touches Outbound only and does not conflict.

## Risks / Trade-offs

- [A pipe grant holds a slot while the pipe is slow or shared with other targets] → One slot per grant, closed when its sources run dry, and `grant_hold_ticks` as fallback.
- [The orphan lock sweep releases grant IN locks] → `grantLock` asks the station's `BufferGrants`, as it does for the Packing Station today.
- [Top up lets an OUT locked slot change its count while a shuttle drives] → `loadPutAway` takes only the job's lock amount; the surplus is planned separately, so nothing is lost or taken twice. GameTest "Arriving after loading".
- [Stacked insert changes where items land in foreign inventories] → Only order changes, never amounts; machines with dedicated slots keep their own `isValid` rules.
- [The Inbound Station now answers `PACKET_ENDPOINT`; a data line delivery could find it] → The station is no data member, so no delivery targets it; a GameTest checks the data line ignores it.
- [Offer churn when a station keeps refusing (full buffer)] → Offers happen once per pull cycle per reached target, and refusal costs one slot scan of 9 slots.

## Migration Plan

No save format change. Grants are not saved; on load the Inbound Station drops IN locks that no job owns, the same way the Packing Station does (`dropGrantLocks`), so their items are put away. Rollback is reverting the change; saved OUT locks stay valid either way.

## Decisions taken during apply

Answers of the user to the open questions:
- **One slot per pipe grant**, as recommended in Decision 2.
- **A slow source does not wait for a full grant.** The grant closes when the source runs dry, so a quarry feeding a chest slowly still makes part trips. A minimum wait before a short grant ends is an open point in `PLAN.md` (Open: Shuttles).

Recommendations taken and details settled while implementing:
- **The Packing Station shares the buffer class.** `PackingStationBlockEntity` extends `InboundStationBlockEntity`, so its To storage buffer is the same top up `LockedSlots` and gets the top up too, and its grants live in the same `BufferGrants`. Its whole-slots-per-grant rule stays (`planGrant` override), its grants are marked as not coming from pipes (`grantsFromPipes`), and what its open grants still expect counts as held for the cap like an Inbound grant. Refusal reasons and their order (offline, no storage, full) are unchanged; the constants moved to `InboundStationBlockEntity`.
- **Cap refusal reason.** An Inbound offer the cap leaves no room for is refused with the existing `to_storage_full` reason; the pipe ignores reasons and no data line delivery targets an Inbound Station.
- **Top up only where switched on.** `drain` plans the surplus of locked slots only for buffers in `LockedSlots.Mode.TOP_UP`; the Field Head buffer (counted mode) keeps skipping locked slots.
- **Joining tries every open put-away.** `ShuttleFleet.openPutAways` lists all open put-aways of the item from the station; the job already on the slot is tried first, then the others, before a new put-away is planned.
- **`TopUp.join(jobTotal, surplus, stackSize)`** takes three arguments; the lock amount on the slot is not needed, since a slot never holds more than a stack.
- **Grants end with the block entity.** The station closes its grants in `setRemoved` and `onChunkUnloaded` and again on load, so a pipe never fills a block entity that left the level and offers again to the loaded one.
- **Where the tests live.** Foundation has no block that answers offers, so the duct grant GameTests (`DuctGrantGameTests`) live in Shuttles and use the Inbound Station as the endpoint. The stacked insert tests (`DuctStackingGameTests`) stay in Foundation.
- **GUI amount.** `SlotLocksPayload.Entry.amount` of a pipe grant lock is what the pipe still brings (lock amount minus the slot's count); `MenuLocks.pipeGrants` marks those locks, set by the Inbound Station menu only.
