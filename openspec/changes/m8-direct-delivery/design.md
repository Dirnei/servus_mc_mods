# Design

## Context

Today `CpuBlockEntity.Host.collect(step)` moves every output it can extract from the producer with `DataPackets.move` to `collectionPoint` (the lowest Access Point reaching the producer), books a ledger transfer and `step.collecting(...)`; the step counts what lands. The Counter Terminal's `TerminalRequests.retry` ships a request's missing part from stock every 40 ticks when it has no open shipment, which is how produced items finally reached the terminal. Items landing in the terminal's output slots are credited to its open requests, oldest first (`CounterTerminalBlockEntity.credit`).

## Goals / Non-Goals

**Goals:** one trip for crafted results of a request, no double shipping, nothing lost, saved across reloads.

**Non-Goals:** direct delivery for stock rules or intermediate products.

## Decisions

### The order knows its terminal
`WorkOrder` gets `deliverTo` (a terminal position or null) and `directSent` (items already sent straight to it), saved as `DeliverTo` and `Direct`. `TerminalRequests.request` and `craft` set `deliverTo` on the created order. Stock rule and manual orders keep it null.

### Collection splits the outputs
In `collect(step)`, for an output key equal to the order's item and an order with `deliverTo`, the CPU sends `min(toMove, order.amount - directSent, terminal.need(requestId))` to the terminal with `DataPackets.move(producer, terminal, ...)`, books a ledger transfer and `step.collecting` exactly like a collection to the Access Point, and adds to `directSent`. The rest goes to the collection point. If the move is refused (no route), everything goes to the collection point. `need` is `requested - delivered - pending shipments` of that request, so a request already served from elsewhere takes nothing more.

### The request stops double shipping
`retry` skips a request whose order is open (`hasOpenOrderFor` on its CPU). `book` lowers `missing` to `requested - delivered - pending` when that is smaller, so items that land directly reduce what is missing; after the order ends the usual retry ships any rest from stock.

### Testing
GameTests in `DirectDeliveryGameTests`: sticks request delivered with the warehouse never holding sticks and the Access Point intake empty; 2 stick request leaves 2 sticks in storage; chain (logs to planks to sticks) puts planks into storage and sticks into the terminal; the order's `deliverTo` survives a save and load.

### PLAN.md open decisions
None.

## Risks / Trade-offs

- [Terminal output full] -> packets wait at the terminal like any delivery; the request shows waiting for room.
- [Terminal broken while the order runs] -> the move is refused or ends short; outputs go to the Access Point and the step still completes.
