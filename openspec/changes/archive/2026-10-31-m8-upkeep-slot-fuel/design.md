# Design

## Context

See proposal.md (Why). Today:

- `DataNetwork.keepUpkeep` runs every `UPKEEP_INTERVAL` (40) ticks, keeps open requests in an in-memory `UpkeepBook<BlockPos>` and asks `ItemSupplier.supply(target, like, amount)`, which returns only an int. The book is lost on restart and on network rebuild.
- `BurnerMachineBlockEntity.upkeep` names the stats item (coal) and counts any burning fuel in the slot, so a slot of Coal Blocks counts as coal but coal can never join it.
- `WarehouseSupplier.supply` calls `WarehouseNetwork.ship(target, key, asked)` with request id 0. The shipment's id is a `ProductionLedger` transfer id (saved data per dimension, ended transfers kept 24000 ticks). `PackingStationBlockEntity.cancel(long)` ends a shipment; items waiting for no open shipment are returned to storage by `returnOrphans`. Queued picks are dropped by request id only (`WarehouseNetwork.dropQueuedFor(int)`), as `TerminalRequests.cancel` does.
- After a reload a Packing Station shipment only waits for what its outgoing buffer holds and ends short for the rest (packing-station spec "Shipments survive a reload").
- Holders: `BurnerMachineBlockEntity` (Coal Generator, Burner Pump, Calciner) and `MachineAdapterBlockEntity`.

## Goals / Non-Goals

**Goals:**
- One upkeep request per member at a time, also across restarts and network rebuilds.
- A given up request leaves nothing behind at the warehouse.
- A shipment that can never be delivered does not hold its items forever, whoever asked for it.
- Ask in batches.

**Non-Goals:**
- Changing how arrivals are inserted (fuel slot insertion, adapter `expected` cap stay as they are).
- Any Production Terminal or potato display of the request.

## Decisions

### D1. The fuel slot decides the upkeep item

`BurnerMachineBlockEntity.upkeep.upkeepItem()` returns:
- the fuel slot's item (count 1 copy) when the slot holds something with a burn time > 0,
- the stats `upkeep_item` when the slot is empty,
- `ItemStack.EMPTY` when the slot holds something that does not burn (an empty bucket after lava). `DataNetwork` already skips holders with an empty item.

`upkeepCount()` counts only stacks equal to that item (`ItemStack.isSameItemSameComponents`). `upkeepLevel()` is `min(stats level, item max stack size)`, so a Lava Bucket gives level 1 and nothing more is asked while it sits there. Alternative considered: keep coal and refuse other fuels; rejected because the player chose to fill Coal Blocks and the user asked for the slot item.

`ProductionView.outOfFuel` keeps working: it reads `upkeepCount() <= 0` and treats an empty item as no upkeep.

### D2. Batch threshold as machine stat `upkeep_batch`

New `MachineStats` record component `upkeepBatch` after `upkeepLevel`, JSON `upkeep_batch`, clamped to at least 1, default `DEFAULT_UPKEEP_BATCH = 8` wherever `DEFAULT_UPKEEP_LEVEL` is used today, 0 meaning nothing for machines without upkeep (clamped to 1 when read). The default sits in code next to the other upkeep defaults, like `upkeep_level`; datapacks retune it. `UpkeepHolder` gets `default int upkeepBatch() { return 1; }`; burner machines return the stat.

The Machine Adapter has no machine stats; its item and level defaults come from the machine profile's `upkeep` object. Its batch comes from there too: `MachineProfile` gains `upkeepBatch`, read from `upkeep.batch` and defaulting to `MachineStats.DEFAULT_UPKEEP_BATCH` (8), clamped to at least 1; the shipped furnace, smoker and blast furnace profiles keep the default and need no edit. The adapter's `upkeepBatch()` returns its profile's batch, 1 without a profile (it has no upkeep then anyway). No GUI field: the level stays the player's setting, the batch is a pack setting, and the pure rule caps it at the level the player chose. Alternative considered: a machine stats file for the adapter block; rejected, the adapter acts for many machine kinds and the profile is already where their upkeep defaults live.

Rule (pure): effective batch is `min(batch, level)`; ask when `count == 0` or `level - count >= effective batch`; ask for `level - count`.

### D3. The open request lives on the holder's block entity

User decision 2. New pure record `foundation.data.UpkeepRequest(long supplier, long ticket, int remaining, int lastCount, long until)` (supplier as `BlockPos.asLong`, no Minecraft imports). `UpkeepHolder` gains abstract `UpkeepRequest upkeepRequest()` (null when none) and `void upkeepRequest(UpkeepRequest request)` (null clears; the holder calls `setChanged`). `upkeepRequested(amount, until)` stays as the adapter's hook.

Saved with the block via `ValueOutput.store(key, codec, value)` / `ValueInput.read(key, codec)` (used today in `save/ValueIo.putTag/getTag`), under key `UpkeepRequest`, only when present. The `Codec` is built with `RecordCodecBuilder` in a Minecraft-side helper (`production/UpkeepRequestCodec`), keeping the record pure. Old saves have no key and start without a request.

Game time (`ServerLevel.getGameTime()`) is saved with the world, so `until` keeps its meaning across a restart. Alternatives considered: a per-dimension `SavedData` keyed by position (survives a broken block as stale data, needs cleanup) or saving in the network (rebuilt from the graph, not saved); rejected, and the user named the block entity.

### D4. How arrivals are counted after a reload

The saved `lastCount` keeps crediting: every rise of the count since the last check counts against `remaining`, before and after a restart alike. Burning between checks hides arrivals of the same window; for that, the request also ends when the supplier reports its ticket as not open (D5), which is exact: after a reload the warehouse ends a shipment short for everything not in its buffer, the ticket closes, and the next check asks for what is missing if a batch is. The timeout stays as the last resort (example 1200 ticks, `UPKEEP_TIMEOUT`).

### D5. Supplier tickets

`ItemSupplier` changes (**BREAKING** for the Foundation API, only Shuttles implements it):
- `Supply supply(BlockPos target, ItemStack like, int amount)` where `record Supply(int amount, long ticket)` with `Supply.NONE` (amount 0).
- `boolean open(long ticket)`: false for an unknown ticket.
- `void cancel(long ticket)`: no-op for an unknown ticket.

`WarehouseSupplier`:
- `supply` takes a fresh request id from `ProductionLedger.nextRequestId()`, ships with `WarehouseNetwork.ship(target, key, asked, requestId)` and returns the shipment's ledger id as the ticket.
- `open(ticket)`: `ProductionLedger.get(level).transfer(ticket)` exists and is not ended. Works from any supplier on the network, without finding the station.
- `cancel(ticket)`: new `WarehouseNetwork.cancelShipment(long id)` finds the loaded Packing Station of the network holding that shipment, calls `PackingStationBlockEntity.cancel(id)` and `dropQueuedFor(shipment.requestId())`. Items already in the To network buffer go back via `returnOrphans`; picks on a shuttle arrive as orphans and are put away as today.
- `Shipment` gains `requestId` (int, 0 for none), set on every ship path that passes one through `shipThrough` (supplier, terminal requests), saved in `SavedShipment` under `RequestId` with `getIntOr(..., 0)`; `ProductionLedger.seenRequestId` is called for it on load like the terminal does.
- Ending a shipment early goes through one station helper `endShort(Shipment)`: end the ledger transfer, drop the shipment's grants, and when `requestId != 0` drop its queued picks through `network().ifPresent(n -> n.dropQueuedFor(requestId))`. `cancel(long)` and the safety net of D8 both use it; buffered items go back through `returnOrphans` at the end of the same `ship` pass.

Alternative considered: the ticket as the pick request id with a lookup to the shipment; rejected, the ledger id already is the persistent shipment identity and `open` needs no lookup.

### D6. Bookkeeping as pure step function

`UpkeepBook` becomes a stateless pure class: `static Step step(UpkeepRequest open, int count, int level, int batch, TicketState ticket, long now)`, where `TicketState` is `OPEN`, `CLOSED` or `UNREACHABLE` (supplier block not loaded), and `Step(UpkeepRequest keep, boolean cancel, int ask)`. Order inside a step:
1. Credit `count - lastCount` when positive, update `lastCount`.
2. End when `remaining <= 0` or `ticket == CLOSED`.
3. Else when `now > until`: end with `cancel = ticket == OPEN`; with `UNREACHABLE` keep the request (cannot cancel, must not double).
4. Else keep and ask 0.
5. When ended (or none), `ask` follows D2.

`DataNetwork.keepUpkeep` per loaded holder: read `holder.upkeepRequest()`, compute the ticket state (supplier position not loaded is `UNREACHABLE`; loaded without supplier capability is `CLOSED`; else `supplier.open(ticket)`), run `step`, call `supplier.cancel(ticket)` when told, store `keep` back (only when changed), then ask suppliers lowest position first and store the new `UpkeepRequest(supplierPos, ticket, amount, count, now + UPKEEP_TIMEOUT)` and call `upkeepRequested`. The in-memory book field and `retain` go.

The supplier is asked about the ticket only for members with an open request, once per interval, so no block scanning.

### D7. Which PLAN.md decisions this settles

PLAN.md has no open point for this; it is a play test bug in M8 upkeep. It settles: upkeep follows the slot item, persistence on the holder, batch as datapack stat, cancel on timeout, and the Packing Station's own give-up rule (D8). PLAN.md needs no change.

### D8. The Packing Station gives up undeliverable shipments

User decision: a safety net next to the upkeep cancel (D6). It runs in `PackingStationBlockEntity.ship` once per push interval, for every data line shipment that is not ended and not sending (wireless shipments are judged through the relay shipment at the receiving station), and not in a pass that returns early for a line conflict.

Target state per shipment, decided in this order:
1. **Unloaded**: `!server.isLoaded(target)`. The shipment waits: no offer, no send, and its refusal clock is cleared. The target is not gone, a farm whose chunk unloads must not lose its deliveries, and the station cannot tell a refusal from absence there. `DataPackets` would answer `NO_TARGET` for an unloaded target, so this check comes before any offer or move.
2. **Removed**: the loaded target has no item capability (`Capabilities.Item.BLOCK` null for every side and for no side), because its block was broken or replaced by one that takes no items, or a move fails with `NO_TARGET`. Ends at once through `endShort`. A removed block leaves the data graph through its removal event, so no block is scanned; the lookup is a few capability reads per open shipment per push interval.
3. **Cut**: the target still takes items but the station's data network (`DataNetworks.networkAt(level, worldPosition)`) does not reach it, or a move fails with `NO_ROUTE` or `ROUTE_BROKEN`. Today `send` and `finished` end the shipment on those reasons at once; now they only start the clock. The "not reached" check also runs for shipments with nothing ready, so a shipment waiting for picks is judged without hauling them first. While cut, no offer is made, so no grant is held and no shuttle brings picks for it.
4. **Refusing**: the last offer (`holdGrant`) or move (`send`, `finished` with `REFUSED` or `STALLED`) came back refused.
5. **Taking**: a grant or a started delivery.

The shipment keeps `refusedSince` (game time of the first cut or refusal in a row, -1 while none). Taking or unloaded clears it; cut and refusing keep it running across each other, so a duct cut while the target was refusing does not restart the clock.

**Machine and player targets.** The refusal of the target itself ends a shipment only for machine targets. A shipment is a player request when its target is a Storage Terminal: a terminal request over the data line ships to the terminal's position (`TerminalRequests` passes `terminal.getBlockPos()`), and a wireless terminal request is judged through its relay shipment, whose target is also the terminal (`addRelay`). The station checks `server.getBlockEntity(target) instanceof StorageTerminalBlockEntity` when it judges a loaded target, so no new saved key or ship path is needed and shipments saved before the update classify themselves. Every other target (upkeep holders, Machine Adapter, CPU steps to producers, the ship command) is a machine target. For a player request, refusing keeps waiting with the clock cleared, as today ("Full target does not block the station", "Nothing parks for a full slot" and "Picks stay locked" keep holding); being cut or removed is judged like any other shipment. Alternative considered: a saved `ForPlayer` flag set by the terminal ship paths; rejected, the target type already says it and also works for old saves.

Pure part: `packing/RefusalClock` (no Minecraft imports) with `static Verdict judge(TargetState state, boolean playerTarget, long refusedSince, long now, int timeout)`, `TargetState` `UNLOADED`, `REMOVED`, `CUT`, `REFUSING`, `TAKING`, and `Verdict(boolean end, long refusedSince)`:
- `UNLOADED` and `TAKING`: keep, clock -1. `REMOVED`: end.
- `CUT`: the clock starts at `now` when -1; end when `now - refusedSince > timeout`.
- `REFUSING`: a player target keeps with clock -1; a machine target is judged as `CUT`.

The block entity only maps what it saw to a state and applies the verdict.

Timeout: new Shuttles stat `refused_shipment_timeout` in `data/servus_shuttles/shuttles/warehouse.json` next to `push_interval`, read in `ShuttlesStatsManager` with `DEFAULT_REFUSED_SHIPMENT_TIMEOUT = 1200` and clamped to at least the push interval. By default it equals the upkeep timeout; whichever acts first, the shipment ends once and the other finds the ticket closed (D6 step 2). The upkeep timeout counts from the ask, not from the cut, so an upkeep shipment cut near the end of its upkeep timeout is cancelled by the upkeep side; the machine asks again once the line is back, which costs a new pick and nothing else.

`refusedSince` is saved in `SavedShipment` under `RefusedSince` with `getLongOr(..., -1)`; game time is saved with the world, so the clock keeps running across a restart. Old saves have no key and start the clock at the first refusal after the load, which is what clears the coal stuck in existing worlds (their targets are Coal Generators, machine targets).

The requester sees the end through the ledger transfer (ended, delivered short): an upkeep request through its ticket (`open` false), a terminal request, a CPU step and the Machine Adapter through the transfer they already watch. None of them retries on its own beyond what it does today for a transfer that ended short.

Alternatives considered: counting refusals instead of time (depends on the push interval, rejected); ending shipments to unloaded targets (loses deliveries to farms in unloaded chunks, rejected); ending at once on a cut line as today (a duct broken while rebuilding would drop the shipment, rejected by the user); a timeout for terminal requests too (keeping the slot full is the player's choice, rejected by the user); a one-time save cleanup (the user declined it, the timeout covers it).

### NeoForge and Minecraft APIs used

- Block capabilities `servus_foundation:upkeep` and `servus_foundation:supplier` (existing, `ServusFoundationCapabilities`, registered through `RegisterCapabilitiesEvent`), read with `ServerLevel.getCapability(cap, pos, side)` as `DataNetwork` does today.
- `ValueOutput.store` / `ValueInput.read` with a `Codec` (already used in `ValueIo`), `RecordCodecBuilder` and `Codec.LONG`/`Codec.INT` (DataFixerUpper, used by `MachineStats.Partial.CODEC`).
- `ServerLevel.isLoaded(BlockPos)` for the unloaded target check and `ServerLevel.getCapability(Capabilities.Item.BLOCK, pos, side)` for the removed check (D8).
- `ItemStack.getBurnTime(RecipeType.SMELTING, level.fuelValues())`, `ItemStack.getMaxStackSize()`, `ItemStack.isSameItemSameComponents` (used in `BurnerMachineBlockEntity` today).
No new NeoForge API.

Found during apply: a running data line delivery to a target without a grant only failed after `stall_ticks` (600) when its target was broken, so the station could not see the removal at its next push. The Foundation packet `Shipment` now fails with `NO_TARGET` at its next retry once its loaded target takes no items at all on that face (no packet inlet, item capability or stock sink); an unloaded target keeps retrying as before. Every data line `ship` without a request id also takes a fresh one from the ledger, so every shipment that ends short drops its queued picks.

### Pure vs Minecraft

- Pure, JUnit: `UpkeepBook.step`, `UpkeepRequest`, batch rule, `RefusalClock.judge`, profile `upkeep.batch` parsing.
- Minecraft, GameTest: burner slot item and level (`CoalGeneratorGameTests`), persistence round trip of the request on a block entity, supplier tickets and cancel (`PackingStationGameTests` or a new upkeep test in shuttles), shipments refused past the timeout, to removed targets, cut and rebuilt lines and terminal requests into a full slot (`PackingStationGameTests`), adapter batch (`AdapterGameTests`), the end to end Coal Block case.

## Risks / Trade-offs

- [Existing saves hold piled up refused shipments from before the fix] → the old coal shipments have no upkeep request owning them, but D8 ends them once their target has refused them for longer than the timeout after the load, and their coal goes back into storage. Checked in the play check.
- [A player keeps a terminal Output slot full on purpose] → the request waits with its picks locked, as today. Accepted by the user decision; the player cancels it in the terminal.
- [A target cut from the line for good] → its shipment holds its items until the timeout, then they go back into storage. Accepted: the price of surviving a quick duct rebuild.
- [A target in a chunk that never loads again] → its shipment waits forever with its picks locked, as today. Accepted: the station cannot tell it from a farm that loads later; breaking the target while it is loaded ends the shipment.
- [A supplier that is never loaded again keeps the request forever] → the holder asks nothing new from that line. Acceptable: the warehouse is on the same data line and normally loaded; breaking and replacing the holder clears it.
- [Changing the `ItemSupplier` signature] → only `WarehouseSupplier`, `DataNetwork` and one GameTest call it; no external mod implements it yet.
- [Fuel slot item is a modded fuel the warehouse lacks] → the supplier answers 0 and the machine burns down; once empty it falls back to coal. Accepted.

## Migration Plan

No data migration: the new NBT keys are optional, `upkeep_batch`, the profile's `upkeep.batch` and `refused_shipment_timeout` fall back to the code defaults, `RequestId` defaults to 0 and `RefusedSince` to -1. Rollback by reverting; saved `UpkeepRequest` keys are then ignored.
