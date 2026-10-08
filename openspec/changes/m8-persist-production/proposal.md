# Proposal

## Why

During the M8 play test a request at the Counter Terminal was gone after a restart. Requests, Crafting CPU orders, station jobs and Access Point shipments lived only in memory, and a CPU even cancelled its orders when its chunk unloaded. The user rejected that: production that was asked for must survive a restart and unloading. This is a follow-up item of PLAN.md M8 (on-demand autocrafting), `m8-persist-production`.

## What Changes

- Counter Terminal requests are saved with the terminal: item, amount, delivered, missing, their shipments and the CPU holding their order.
- Crafting CPU orders are saved with the CPU's master block: steps, recipes, producers, batches, fed and collected counts, reservations and state. Unloading a CPU no longer cancels its orders; they pause and resume when it loads. Breaking a block still cancels.
- A Crafting Station's job (pattern, crafts left and the CPU step that owns it) is saved with the station. A job whose step no longer exists ends by itself.
- Access Point shipments keep their ids across reloads. After a reload a shipment only waits for what is in the Access Point's outgoing buffer; the rest ends as a shortfall and is shipped again by its owner.
- A level wide ledger (saved data) keeps the progress of shipments and of outputs travelling back from producers, by id, so every owner can book them after a reload or while it was unloaded. Request ids and shipment ids come from saved counters and never repeat.
- Upkeep requests in flight are still forgotten on a restart and simply sent again.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/counter-terminal`: requests survive a reload.
- `storage/crafting-cpu`: orders survive reloads, unloading pauses instead of cancelling.
- `storage/crafting-station`: jobs survive a restart; a job without its step ends.
- `storage/access-point`: shipments survive a reload and are capped to the outgoing buffer afterwards.

## Non-goals

- Saving packets in flight: they are still settled when the server stops, a crash still loses them.
- Saving shuttle pick jobs; the shipment cap after a reload covers picks that never come.
- Saving the Machine Adapter's and the upkeep keeper's open upkeep requests.
- Migrating requests or orders from before this change (unreleased).

## Impact

- `storage`: `order` (WorkOrder, OrderStep, OrderBook, new ProductionLedger and step references), `cpu/CpuBlockEntity`, `terminal/CounterTerminalBlockEntity` and `TerminalRequests`, `crafting/CraftingStationBlockEntity`, `access/AccessPointBlockEntity`, `network/WarehouseNetwork` (shipping record), commands and GameTests that read shipments.
- Saved data file `servus_storage_production` per dimension.
