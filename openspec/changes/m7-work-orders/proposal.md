# Proposal

## Why

The pieces are in place: machines say what they can make, docks feed and collect, the Crafting Station produces without Manufacturing, and a pure planner explodes a bill of materials. Nothing yet turns a request the warehouse cannot fill into production. This change implements the M7 item "Work orders, BOM explosion, reservations" from `PLAN.md`: a request for an item that is not in stock creates a work order, the order is broken into steps on docks, stock on hand is reserved and the rest is produced, and the request is served as the products arrive.

## What Changes

- **Work orders on the warehouse.** A request whose stock falls short creates a work order for the shortfall when some dock's producer can make the item, directly or through intermediate recipes. Orders can also be created by hand with `/servus_warehouse order <pos> <item> <count>`.
- **Planning with reservations.** The order is planned with the Lib order planner over the recipes of all docks, idle docks first. Stock the plan uses is reserved so later plans do not count it twice; reservations shrink as the stock is actually fed to docks.
- **Execution on docks.** Each step runs on one dock: the warehouse feeds the step's inputs to the dock through the feed path, the machine produces, the dock collects, and the collected outputs count the step as done. One step at a time per dock; steps that lack inputs wait for earlier steps or for stock to arrive. Orders are served first come first served.
- **Visible state.** Each order has a state (running, blocked with a reason, done) and each step shows waiting, feeding, producing or done. The request that caused an order shows "producing". `/servus_warehouse orders <pos>` lists everything. Cancelling a request cancels its order; unfed steps are dropped, fed items stay in the machines.

## Capabilities

### New Capabilities
- `storage/work-orders`: creating orders from requests and by command, planning with reservations, execution on docks, states and reasons, cancellation.

### Modified Capabilities
- `storage/counter-terminal`: a request with missing stock creates a work order when a producer exists and shows the state producing.
- `storage/warehouse-controller`: the `orders` and `order` commands.

## Non-goals

Deferred to later changes:
- Min and max stock rules that create orders automatically (`m7-stock-rules`).
- The order board screen (`m7-order-board`); commands and the request list are the view for now.
- Persisting orders across a reload. Like requests, they are cleared; produced items stay in storage and a new request picks them up.
- Explicit order priorities. Orders run first come first served; this settles the open decision in `PLAN.md` section 5.
- Parallel steps on one dock, or splitting one step across several docks.

## Impact

- `storage`: a new package `io.github.dirnei.mc.servus.storage.order` with `WorkOrder`, `OrderStep` and `OrderBook` (the executor owned by `WarehouseNetwork`), hooks in the request flow, the station drain (collected outputs) and the delivery outcome; a new request state; two commands. The planner comes from `lib/production`.
- `PLAN.md` section 5 loses the "Order prioritization" bullet; section 2.4 is unchanged.
