# Proposal

## Why

Play test: when an item is produced for a Counter Terminal request, the CPU first moves the result to an Access Point where it is put away, and only then does the terminal ship it back out of storage. That is a detour through the warehouse (shuttles for racks included) and makes crafting requests feel slow. Part of PLAN.md M8 as follow up to `m8-crafting-cpu` and `m8-terminal-ui`.

## What Changes

- A work order made for a Counter Terminal request remembers that terminal.
- When the CPU collects the outputs of a step that makes the order's item, it sends up to what the request still needs straight from the producer to the terminal as packets. Batch surplus and the outputs of intermediate steps still go to an Access Point and are put away.
- The step counts collected outputs as before, wherever they land. The request counts what lands in the output slots; while its order is open it no longer ships the same item from stock, and its missing amount shrinks as direct items land, so nothing is shipped twice.
- When the terminal cannot be reached from the producer's line, the outputs go to the Access Point as before.
- The terminal link and the amount already sent directly are saved with the order.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/crafting-cpu`: final step outputs of a request's order go straight to its terminal.
- `storage/counter-terminal`: crafted items arrive without a trip through storage; no stock shipping for the item while its order runs.

## Non-goals

- Direct delivery for stock rules (they fill storage on purpose).
- Delivering intermediate products anywhere but storage.

## Impact

- `storage/.../order/WorkOrder.java` (terminal link, direct count, saved), `storage/.../cpu/CpuBlockEntity.java` (collection), `storage/.../terminal/{TerminalRequests,CounterTerminalBlockEntity}.java`.
- GameTests in `storage/.../gametest/CraftingCpuGameTests.java`.
