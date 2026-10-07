# Design

## Context

See proposal.md for motivation. `WarehouseNetwork` plans requests with `RequestPlan` in `planAndStart`, starts pick jobs with `startJobs`, retries missing stock every 40 ticks in `retryMissing`, drains station and dock buffers in `drain`, and knows its docks through `producers()`. `OrderPlanner` in Lib returns steps keyed by recipe object, reservations and missing amounts. `ProductionRecipe` carries exact stacks. The Crafting Station is a Storage producer served through a dock.

## Goals / Non-Goals

**Goals:**
- Orders as transient network state driven from `onTick`, like stations and requests.
- Every decision visible: states, reasons, commands.

**Non-Goals:**
- See proposal.

## Decisions

### Settles the PLAN.md open decision on order prioritisation
First come first served. Orders are kept in creation order and docks are handed to the oldest order that wants them. Section 5 loses the bullet.

### Order model
Package `io.github.dirnei.mc.servus.storage.order`:
- `WorkOrder`: `id`, `ItemKey key`, `int amount`, `int requestId` (0 for manual), `List<OrderStep> steps`, `Map<ItemKey, Integer> reserved`, `Map<ItemKey, Integer> missing`, `State {RUNNING, BLOCKED, DONE, CANCELLED}`, `String reason`, `long doneAt`.
- `OrderStep`: `BlockPos dock`, `ProductionRecipe recipe`, `int batches`, `Map<ItemKey, Integer> fed`, `Map<ItemKey, Integer> collected`, `State {WAITING_DOCK, FEEDING, PRODUCING, DONE}`, `String reason`. `needed(key)` is input count times batches; `inputsFed()` when every input is fed; `outputsCollected()` when the first output's collected count reaches its count times batches.
- `OrderBook`: owned by the network, holds `List<WorkOrder>` and `nextId`, and runs the executor. It talks to the network through a small `Host` interface: `producers()`, `stock()` (index counts), `Feed feed(BlockPos dock, ItemKey key, int amount)` returning the planned count and a refusal key, `boolean dockBusy(...)` is the book's own knowledge.

### Creating an order
`OrderBook.create(key, amount, requestId)`: builds `OrderPlanner.Recipe<ItemKey>` per dock recipe with `id = new StepKey(dock, recipeId)`, inputs and outputs merged by `ItemKey`, ordered idle docks first then by ticks; stock is `index.count` minus the sum of reservations of open orders; `OrderPlanner.plan(key, amount, stock, recipes, 8)`. No steps and the key itself missing means `no_producer`: nothing is created and the caller gets that reason. Otherwise the order is created with the plan's steps, reservations and missing map; missing materials make it `BLOCKED` with reason `missing <item>` but its steps still start as inputs allow, so the order unblocks as stock arrives.

### Executing
`OrderBook.tick()` runs every 20 ticks from `WarehouseNetwork.onTick`: for each order in creation order, for each step not done:
- `WAITING_DOCK`: if no other order's step is `FEEDING` or `PRODUCING` on that dock, the step becomes `FEEDING`.
- `FEEDING`: for every input with `needed > fed`, ask the host to feed `needed - fed`; the planned count is added to `fed` and subtracted from the order's reservation; a refusal `out_of_stock` keeps the step feeding and marks the order blocked with `missing <item>`, `needs_shuttle` or `no_rail` mark it blocked with that reason. When every input is fed the step is `PRODUCING`.
- `PRODUCING`: waits for `collected`. The network's `drain` calls `orders.collected(dock, key, count)` whenever it takes a stack out of a dock's collect buffer, instantly or by claiming it for a shuttle; the book credits the oldest producing step on that dock whose recipe outputs the key. When the first output is fully collected the step is `DONE`.
When all steps are done the order is `DONE` with `doneAt` set; done and cancelled orders are dropped after 200 ticks. `cancel(id)` drops the order, releasing its reservations; fed items stay where they are.

### Request integration
`planAndStart` returns `Started(String refusal, int planned, int unservable)`. `request` creates the request, then, if `unservable > 0`, calls `orders.create(key, unservable, request.id)`; a `no_producer` answer leaves the request as missing. `requestRows` shows `PRODUCING` when the book has an open order for the request. `cancelRequest` cancels its order. `retryMissing` keeps serving the request as produced stock lands in storage, which is how an order's result reaches the output slots.

### Commands
`orders <pos>` prints each order: `#id 8 Stick: running` plus a reason line and one line per step `dock x y z: Stick (2 batches) producing`. `order <pos> <item> <count>` calls `create` with request id 0 and prints the reason on refusal.

### Minecraft side verification
GameTests with the Crafting Station: `orderCraftsMissingItems` (request 8 sticks with planks in a crate, dock facing a station with the sticks recipe, a free crate for the sticks; the order runs through feeding, producing and done and the terminal's output slots receive 8 sticks), `orderWithoutProducerIsRefused` (bread request: no order, state missing; `create` answers `no_producer`), `twoLevelOrderRunsLeavesFirst` (logs in stock, station with planks and sticks recipes; 8 sticks requested; the planks step completes before the sticks step and 8 sticks arrive), `cancelReleasesReservation` (order sticks with planks in stock but the dock blocked by a busy step from another order; cancel; the second order's reservation is gone).

## Risks / Trade-offs

- [Collected counts credit the wrong step when two steps on one dock output the same item] → one step per dock at a time avoids it; mixed outputs of one recipe credit their own keys.
- [A machine with its own stock of inputs produces more than the step expects] → extra output is just stock; the step completes on the expected count.
- [Byproducts never collected keep a step producing] → steps complete on their first output only.
- [Reservations are counts, not slots] → good enough: the feed path takes whatever is there; a shortfall shows as missing and the retry fixes it.

## Open Questions

None.
