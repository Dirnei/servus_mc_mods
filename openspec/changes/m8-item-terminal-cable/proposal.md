# Proposal

## Why

PLAN.md M8 item 4, `m8-item-terminal-cable`. The decided rule is "only storage touches rails, everything else sits on the data line". The Counter Terminal is not storage, yet today it joins the warehouse by adjacency or rails and shuttles drive to it. With the data line, packets and the Access Point in place, the terminal can move to the data line and get its deliveries as packets.

## What Changes

- The Counter Terminal leaves the warehouse network. It no longer links to rails, crates or controllers and is never a shuttle destination.
- It becomes a data member with the link role and an insert only item handler for its output slots, so packets can land there.
- It finds warehouses over its data line: a warehouse is reachable when its controller or one of its Access Points is a member of the terminal's data network and the warehouse is online. Its lamp shows that a warehouse is reachable.
- The stock view merges the stock of every reachable warehouse.
- Requests are filled through the shipping path of `m8-access-point`: picks to the Access Point with the shortest shuttle trip (crates instantly, racks by shuttle), then packets into the output slots. With several warehouses, the one holding the most of the item ships first, then the next. A shortfall becomes a work order on the first reachable warehouse, as before.
- Request states: picking (on the way to the Access Point), sending (packets on the line), waiting for room, producing, missing. "Waiting for a shuttle" folds into picking.
- Requests keep living on the terminal (still not saved). Request ids become unique across terminals so work orders link to the right request.
- Stock rule mode and pushes act on the first reachable warehouse. Pushes stay an instant store into crates and foreign inventories (no packets yet).
- Rail, lamp and shuttle specs drop the terminal as a rail member and shuttle destination.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/counter-terminal`: joins the data line, reaches warehouses over it, merged stock, requests served by packets, new request states, insert only output handler.
- `storage/connection-indicator`: the terminal's lamp means "a warehouse is reachable over the data line".
- `storage/warehouse-rail`: counter terminals no longer take rails or join through them.
- `storage/shuttles`: pick jobs for requests end at an Access Point, not at the terminal.

## Non-goals

- Pushing through packets to an Access Point (pushes stay instant into crates and foreign inventories).
- Saving requests across a restart.
- Moving work orders off the controller (`m8-crafting-cpu`) and the Order Board (`m8-crafting-cpu`, `m8-production-terminal`).
- A wireless terminal (M9).

## Impact

- `storage/terminal` (block, block entity, a warehouse lookup over the data line), `storage/menu/CounterTerminalMenu`, `storage/network/WarehouseNetwork` (terminal paths removed, small public helpers), `storage/access/AccessPointBlockEntity` (sending state per shipment), `ServusStorageEvents` (capabilities), lang.
- Every Storage GameTest that used a terminal is rewired through an Access Point and a data duct.
- Docs Storage page.
