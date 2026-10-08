# Design

## Context

Today `CounterTerminalBlockEntity` extends `StorageNodeBlockEntity` (kind STOCK), so it joins the warehouse graph by adjacency and rails. `WarehouseNetwork` keeps a `terminals` set and owns the request flow: `request(terminal, key, amount)` plans pick jobs to the terminal's position, `retryMissing` replans missing amounts every 40 ticks, `requestRows` derives states from the shuttle fleet, `deliverTo`/`pickLoaded`/`pickDropped` credit the terminal's `Request`. `CounterTerminalMenu` reaches the warehouse through `terminal.network()`.

`m8-access-point` added `WarehouseNetwork.ship(target, key, amount)`: picks land in the Access Point with the shortest shuttle trip that shares the target's data line, and the Access Point's `Shipment` sends them as packets (`DataPackets.send`). Packets enter a target through its item handler on the face touching the duct, or its stock sink; what does not fit waits at the target and is retried.

See proposal.md for why and the delta specs for behavior.

## Goals / Non-Goals

**Goals:**
- The terminal is a pure data line member; the warehouse network has no terminal code left.
- Requests ride the existing shipping path; no second delivery mechanism.

**Non-Goals:**
- Packet based pushes, persisted requests, the CPU taking over orders.

## Decisions

### Terminal block entity
`CounterTerminalBlockEntity` extends plain `BlockEntity` and implements `MenuProvider` and `DataLink`. It is no `StorageNodeBlockEntity`, no `DeliveryTarget`, and has no network membership of its own. The block keeps `FACING` and `StorageNodeBlocks.ONLINE`; the block entity sets `ONLINE` itself. A server ticker (`CounterTerminalBlock.getTicker`) runs `TerminalRequests.tick` every tick: every 20 ticks it refreshes the lamp, every 40 ticks it retries missing amounts (same cadence as before), and every tick it books ended shipments.

Capabilities registered in `ServusStorageEvents`:
- `ServusFoundationCapabilities.DATA_LINK` (the block entity itself), so the data line counts it as a link member and draws an arm.
- `Capabilities.ItemHandler.BLOCK`: an insert only wrapper over the output slots. Insertions credit open requests of that item, oldest first. Extraction returns nothing. Alternative considered: exposing a `StockSink` instead; rejected because a stock sink gives the STOCK role and the terminal would look like a warehouse to other data members.

### Finding warehouses
New `terminal/TerminalWarehouses.reachable(ServerLevel, BlockPos)`: the data network at the terminal (`DataNetworks.networkAt`), then for every member: a `WarehouseControllerBlockEntity` or an `AccessPointBlockEntity` gives its warehouse network; only `ONLINE` networks count; duplicates removed; sorted by the lowest controller position (the warehouse's `controllerPos()`, added if missing). Merged stock sums `index().entries()` per key.

Reaching a warehouse only through its controller is allowed for viewing and rules, but shipping needs an Access Point on the same line, so such a request is refused with `no_access_point` (already a `ship` refusal).

### Requests
Requests stay on the terminal; the waiting for room flag goes; new fields: the list of tracked shipments (`AccessPointBlockEntity.Shipment` plus the amount asked of it) and the controller position of the warehouse holding its work order. Request ids come from one static counter so `OrderBook.cancelForRequest` and the fleet's `removeQueued` never hit another terminal's request.

`TerminalRequests.request(terminal, key, amount)`:
1. No reachable warehouse: refusal `offline`.
2. Warehouses sorted by available stock of the key, most first. For each with stock: `ship(terminalPos, key, min(stock, remaining), requestId)`; record the shipment, subtract its planned amount; remember the first refusal.
3. Remaining amount: `orders().create(key, remaining, requestId)` on the first warehouse; if that fails and nothing shipped, drop the request and return the first refusal or `out_of_stock`.

`WarehouseNetwork.ship` gains a request id parameter (the old signature passes 0) so pick jobs carry it and cancelling can drop queued jobs.

Retry: every 40 ticks, for every request with a missing amount and no open shipment, ship again from the warehouses that have stock now. Ended shipments: the asked amount minus what that shipment delivered is added back to missing.

States (`RequestState`): while an open shipment of the request is the one its Access Point is sending (`AccessPointBlockEntity.isSending(shipment)`), `SENDING` when one more of the item fits into the output slots and `WAITING_ROOM` when it does not (packets only try empty or matching slots, so the insert handler never sees the remainder); `PICKING` when any other shipment is open; `PRODUCING` when the order warehouse has an open order for the request; else `MISSING`. `WAITING_SHUTTLE` is removed with its lang key; `sending` gets one.

Cancel: `fleet.removeQueued(id)` and `orders().cancelForRequest(id)` on every reachable warehouse, `AccessPointBlockEntity.cancel(shipment)` for open shipments (ends it, its outgoing items become orphans and go back to the intake for put away), then the request is removed. Packets already on the line still land in the output slots.

### WarehouseNetwork clean up
Removed: the `terminals` set, `request(terminal, ...)`, the terminal part of `retryMissing` (the method goes, the 40 tick timer goes), `cancelRequest`, `requestRows`, `heldFor` uses for terminals, the terminal branches in `deliverTo`, `pickLoaded`, `pickDropped`. Added: `available(ItemKey)` (sum of `stockOf`), `ship(target, key, amount, requestId)`, `controllerPos()` if missing, and public order helpers the terminal needs (`hasOpenOrderFor`, `cancelOrdersForRequest`, `createOrder`).

### Menu
`CounterTerminalMenu` takes its warehouses from `TerminalWarehouses`. `stock()` builds the payload from the merged index and the first warehouse's rule rows; reason `not_connected` becomes `no_warehouse` (new lang key) and the controller specific reasons go away. `request`, `cancel` call `TerminalRequests`; `setRule`, `pushCarried` and shift click store use the first warehouse (pushes stay `WarehouseNetwork.insert`, instant into crates and foreign inventories).

### Tests
GameTests poll with `TerminalWiring.when` instead of fixed delays (the GameTest scheduler keys tasks by instance, so every poll is a fresh lambda). They built a terminal beside a controller, on a rail or at a rail's end in about 30 tests. A helper `TerminalWiring.place(helper, pos)` puts an Access Point where the terminal stood (so it joins the same warehouse by adjacency or rail and is the shuttle destination the terminal used to be), a data duct above it and the terminal on top; `TerminalWiring.at(helper, pos)` returns the terminal two blocks up. Timings grow by the packet travel time. New tests: request delivered by packets, terminal beside a controller only is offline, two warehouses merge and split a request, cancel puts picked items back, hopper cannot extract from the output slots.

### PLAN.md
No open decision of section 5 is touched. PLAN 2.3 item 2 still says "Warehouse Controller + Counter Terminal"; its sentence about standing at the counter stays true. Nothing to change.

## Risks / Trade-offs

- [Hoppers and ducts can now insert into the output slots] -> accepted; insertion credits requests of that item, which at worst marks one delivered early. Extraction stays closed.
- [Requests are lost on reload, shipments then end as orphans] -> unchanged behaviour from before; the items go back into storage.
- [Two terminals on one line request the same item] -> each request tracks its own shipments; insertion credit goes to the oldest open request of that terminal only, since packets target one terminal.
- [Test rewiring changes geometry] -> the Access Point takes the terminal's old place, so rail and adjacency behaviour under test stays the same.
