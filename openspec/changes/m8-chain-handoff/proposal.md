# Proposal

## Why

Play test: a request for Cracked Stone Bricks (cobblestone to stone in a furnace, stone to stone bricks on a Crafting Station, stone bricks to cracked stone bricks in a furnace) sends every intermediate from its machine to an Access Point, into storage, back out of an Access Point and on to the next machine. `m8-direct-delivery` already sends the final item straight to the terminal; the intermediates still take the long way. This is a follow-up to PLAN.md M8 item `m8-crafting-cpu` (steps run over the data line).

## What Changes

- When a step's outputs are ingredients of a later step of the same order, the CPU sends them as packets straight from that step's producer to the later step's producer, counted as that step's fed inputs. Storage only ships what the later step still needs on top.
- A step waiting because its planned producer is busy moves to another free producer of the line that offers the same recipe, so a chain can spread over two stations or two furnaces and hand off between them.
- When the later step is not ready yet (its producer busy with another order), the outputs wait at their producer for a short while (`handoff_wait_ticks`, example 100) and then go to storage as before, so nothing gets stuck. When the later step runs on the same producer, the outputs go to storage right away.
- A later step does not report a fed input as missing while an earlier step of the same order still makes it.
- The Production Terminal's Crafting tab says when a step hands off, for example "Furnace (adapter): sending Stone to Crafting Station".
- Handoffs are booked in the production ledger like every other transfer, so a reload mid handoff neither loses nor duplicates items.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/crafting-cpu`: intermediates go straight to the next step; waiting steps move to a free producer with the same recipe.
- `storage/production-terminal`: the Crafting tab names a handoff.

## Non-goals

- Changing what a cancel does: items already handed off stay in the next producer, outputs still in a producer stay there, as today.
- Handing off between different orders or different CPUs.
- Saving the short wait across a reload (it starts again).
- Planning a chain over several producers up front; producers are only switched when a step would otherwise wait.

## Impact

- `storage/order/{OrderBook,OrderStep,Handoff}`, `storage/cpu/CpuBlockEntity`, `storage/production/{JobStatus,ProductionView}`, `storage/crate/StorageStatsManager` and `data/servus_storage/storage/cpu.json` for the wait, lang.
- GameTests in Storage; JUnit for the pure handoff split.
