# Design

## Context

`OrderBook.advance` runs every 20 ticks per order: steps start when their producer is free (`WAITING_STATION` to `DELIVERING`), `feed` ships missing inputs from storage, and `Host.collect` moves outputs of active steps away: the order's own item to the requesting terminal (`sendDirect`, m8-direct-delivery), everything else to the lowest Access Point. Collections are booked as `OrderStep.Collecting(key, ledger id, sent)`, shipments as `Feeding(key, ledger id, asked)`; both settle against `ProductionLedger` (m8-persist-production). The planner fixes each step's producer when the order is made and usually puts all steps of a chain on the first producer that offers them.

## Goals / Non-Goals

**Goals:** intermediates of a chain travel producer to producer; chains can spread over several producers; no loss or double count across reloads.

**Non-Goals:** see proposal.

## Decisions

### Pure split: `order/Handoff`
A pure class decides, for one output key of one step, how many go where: inputs are the movable count, the dependants (later steps of the same order still owing the key: owed amount, whether delivering, whether on the same producer), how long the outputs have been held and the wait limit. Output: a list of (dependant index, count) handoffs to delivering dependants (in step order, each up to what it owes), whether to hold the rest, and how many go to storage. Rest is held when some dependant on another producer still owes and is not delivering and the hold is younger than the limit; otherwise it goes to storage. JUnit covers it. Alternative considered: deciding inline in `CpuBlockEntity`; rejected so the rule is testable without Minecraft.

### Booking a handoff
One ledger transfer per handoff. The step books it with `collecting(key, id, n, target)` (the target position is kept for the terminal wording and saved); the dependant books it with `addFed(key, n, [Shipped(id, n)])`. Existing settle rules then do the right thing: the step counts what landed as collected, the dependant subtracts a shortfall from fed and is fed again from storage; a forgotten id counts as landed for the step and as nothing for the dependant (fed again; worst case a few extra items sit in the dependant's producer, never lost). Packets that cannot land settle into the target, the source or the ground as in m8-cable-packets; items that fall back into the source producer are collected again later.

### Hold timer
`OrderStep` keeps a transient `holdSince` game time per step, set when outputs are held for a dependant and cleared when nothing is held. Not saved: after a reload the wait starts again (non-goal). `handoff_wait_ticks` comes from `storage/cpu.json` through `StorageStatsManager` (default 100).

### Moving waiting steps
`OrderStep.producer` becomes mutable (already saved). In `advance`, a `WAITING_STATION` step whose producer is busy looks through `Host.producers()` for one that offers a recipe with the same id and the same inputs per batch, is loaded and not busy (this book or another CPU of the line); it switches to the first such producer (lowest position first, the order `CpuOrders.producers` returns) and starts there. This is what lets a two step chain on one station spread to a second station.

### No false "missing"
`feed` ignores the refusal for an input that an earlier, not done step of the same order produces; the step reason stays empty, so the job reads "getting ingredients" rather than "missing".

### Order of collection
For each output key of an active step: first the terminal (`sendDirect`, only the order's own item), then handoffs, then hold or Access Point.

### Wording
`JobStatus.Step` gains `sending` (item name) and `sendingTo` (machine name) for the first handoff still in flight of the current step; `JobStatus.of` returns `sending` with machine, item and target when present and the step has no reason. Lang `gui.servus_storage.production.job.sending`: "%s: sending %s to %s".

### NeoForge APIs
None new; `DataPackets.move`, `ProductionLedger`, the item handler capability as before.

### Testing
Pure: `HandoffTest` (all to a delivering dependant, split with surplus, hold while a dependant on another producer waits, hold expires, same producer goes to storage, several dependants in order). GameTests (`ChainHandoffGameTests`): planks to sticks on two stations never stocks planks; smelt then craft (cobblestone to stone in a furnace adapter, then stone to stone bricks on a station) never stocks stone; busy next producer falls back to storage and completes; reload mid handoff completes without loss; existing CPU, terminal and persist tests stay green.

## Risks / Trade-offs

- [A dependant grabs its producer early and waits there for the intermediate] -> it only does so when its own producer is free; other orders find it busy for that time, as with any running step.
- [Several producers switch around under load] -> only waiting steps switch, and only to free producers with the identical recipe.
- [The hold delays a chain by up to the wait when the next producer is busy] -> bounded by `handoff_wait_ticks`.
