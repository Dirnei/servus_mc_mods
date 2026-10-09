# Design

## Context

See proposal.md for the why and the specs for behaviour. Today:
- `foundation.data.packet`: `DataPackets.send` hands a caller's stack to the network, `DataPackets.move` takes items from a source as each packet leaves. `Shipment` ticks one packet per delivery at a time; `PacketFlight` and `PacketRoutes` give travel time from `packet_size` and `packet_speed` (`PacketLimits`, `PacketLimitsManager`, `data/servus_foundation/data/packets.json`); `PacketStash` keeps items that could not be placed; packets in flight are settled at server stop.
- `storage.access`: `AccessPointBlockEntity` extends the Inbound Station block entity: the inherited intake buffer is "to storage", `outgoing` holds picks waiting to be sent, both sized by `inbound_buffer_slots`. Its GUI is the Inbound Station menu (intake only, extractable). `WarehouseNetwork.ship` plans picks into `outgoing`; `ShipmentCap`, `AccessPointChoice`, `WarehouseSupplier`, `ServusStorageShipping` build on it. Shipments are tracked in `ProductionLedger` by id.
- `StorageTerminalBlockEntity.storeInput` empties every input slot into `DataPackets.send` at once, which is why the Input row empties immediately.

## Goals / Non-Goals

**Goals:**
- The Packing Station is the only throughput limit between the warehouse and the data line, per direction, visible in its GUI.
- Items stay visible at their source until a station takes them; nothing is in flight.
- Rename with no behaviour hidden behind the old name.

**Non-Goals:**
- Cable bandwidth, upgrades, load overlay (`m8-data-bandwidth`).
- A delivery queue in the GUI.

## Decisions

### Rename
Same approach as `m8-storage-terminal`: a script replaces `AccessPoint` with `PackingStation`, `ACCESS_POINT` with `PACKING_STATION`, `access_point` with `packing_station`, `access-point` with `packing-station` and "Access Point" with "Packing Station" in code, data, docs and specs; `git mv` renames the files, the `access` package to `packing`, textures, the docs icon and `openspec/specs/storage/access-point` to `storage/packing-station`. Main specs of other capabilities that only name the block are edited directly (no behaviour change). The deltas of this change target the renamed path, so the move must happen before archive.

### Instant deliveries and transfer gates (Foundation)
- `Shipment` loses travel: each network tick a running delivery moves `min(source can give, target accepts, gate allowances)` items in one step. `PacketFlight` and the travel part of `PacketRoutes` are removed; reachability is "source and target are members of the same data network" (already known by `DataNetworks`). `PacketStash` stays for handed over stacks that cannot be placed after a route breaks.
- New sided capability `servus_foundation:transfer_gate` (`TransferGate`: `int allowance(Flow flow)`, `void consume(Flow flow, int items)`, `Flow` = IN or OUT). A delivery asks the gate of its source for OUT and of its target for IN. Deliveries are ticked in creation order, so earlier ones use a gate's budget first.
- Pure `RateBudget` (no Minecraft imports, JUnit): items per second turned into a per tick budget with fractional carry (8 per second gives 8 items over 20 ticks, not 0 per tick), capped at one second of budget so an idle station cannot burst.
- `packets.json` keeps only `retry_interval`; `PacketLimits` and the manager shrink accordingly.
- APIs: `BlockCapability.createSided` and `RegisterCapabilitiesEvent.registerBlockEntity` (NeoForge 21.1 capabilities, as used for the existing Servus capabilities); the existing `AddReloadListenerEvent` listener for packets.json.

### Packing Station (Storage)
- Implements `TransferGate` with two `RateBudget`s from `items_per_second` in a new `data/servus_storage/storage/packing_station.json` (loaded with the other storage stats by `StorageStatsManager`).
- IN (to storage): its insert path accepts at most the IN allowance and only what fits into the To storage buffer; put-away from that buffer is unchanged (Inbound Station logic).
- OUT (to network): shipments send from the To network buffer at most the OUT allowance per tick. `WarehouseNetwork` releases picks into the To network buffer only while it has room (picks wait in the plan instead of over-filling), so shuttles bring items as fast as the station drains.
- GUI: new `PackingStationMenu` and `PackingStationScreen` with two rows of read-only slots (a `Slot` subclass whose `mayPickup` and `mayPlace` return false; shift click does nothing), row labels "To storage" and "To network" with tooltips, and a status line synced from the server (current delivery item and target name, or a wait reason). Breaking still drops both buffers. It stops extending the Inbound Station menu.
- Item tooltip via the block item's `appendHoverText`.

### Storage Terminal input
`storeInput` stops emptying slots. It starts one `DataPackets.move` per non empty input slot towards the nearest Packing Station, with the terminal as source; the terminal gives the move an extraction view of its Input row (the public item handler stays insert only), so items leave a slot only when the station accepts them. A slot with a running move is not started again.

### Other transfers
Machine to machine handoffs, machine results to the terminal and crates or controller stock to members cross no gate and move instantly, limited only by what the target accepts. This is what the user asked for: the digital side is instant, the border costs time.

### Persistence
Shipments and handoffs keep their `ProductionLedger` ids. With nothing in flight, server stop no longer settles packets; a handed over stack that waits for room is saved with its delivery owner as before (`PacketStash` for the rest).

### Pure versus Minecraft
- Pure, JUnit: `RateBudget` (fractional carry, cap, two directions independent), the per tick transfer amount (min of source, target, gates).
- Minecraft, GameTests: input draining at about 8 per second, full To storage keeps items in the Input row, two stations double throughput, read-only GUI refuses extraction (menu click on a slot), rename ids and lang.

### PLAN.md open decisions
Settles the throughput model left open since `m8-cable-packets` ("items travel as packets with a size and a speed"): the network is instant and the Packing Station limits per direction. "Later: High-Speed Data Cable" is replaced by `m8-data-bandwidth`.

## Risks / Trade-offs

- [Every packet based test depends on travel time] -> tests that waited fixed ticks for packets are rewritten to wait for conditions; instant moves make most of them faster.
- [Gates shared in creation order can starve a late delivery] -> acceptable for one station; more stations add throughput; `m8-data-bandwidth` revisits fairness with segment budgets.
- [Removing travel removes the "long distance takes time" feel] -> intended by the user; distance may matter again for wireless (M9).
- [Rename churn across many files and tests] -> scripted rename plus a grep that finds no `access_point`, `AccessPoint` or "Access Point" outside archives.

## Decisions made during apply

- `PacketRoutes` stays: the shortest route still tells whether source and target are linked and which faces touch the line. Only `PacketFlight` (with its test) and `DataNetwork.packetRouteFrom` went away. A removed data node makes every running delivery look up its route again on its next tick.
- `DataPackets.move` got an overload with a source `IItemHandler` view. The Packing Station ships with a move from its To network buffer (instead of handing a stack over with `send`), so items stay visible in the buffer until the gate lets them out; the Storage Terminal moves from a one slot view of its Input row that gives nothing once the terminal is removed.
- A move asks the target how much it takes (simulated insert) before it takes items from the source; what a stock sink refuses goes back to the source, and only if the source refuses it too is it held and offered again. A target that takes nothing is asked again every `retry_interval` ticks.
- Handed over stacks that cannot continue are settled back into the source first, then the target, then dropped, as the spec says (before, the target came first).
- Gates limit only deliveries over the data line. Hoppers, ducts and producers that push into a Packing Station through a face are not rate limited.
- `RateBudget` caps at one second's worth but at least one item, so a rate below one per second still moves items.
- Instant picks for a Packing Station wait in `WarehouseNetwork` as deferred picks and are released as the To network buffer has room (on every buffer change). They are not saved: after a reload or a network split the Packing Station's shipment ends short for what never arrived and its owner ships again, the existing shortfall path.
- A Packing Station shipment's ledger progress is booked when its move ends, as before; GameTests measure the buffers instead of the ledger while a move runs.
- The GUI is painted with `PanelPainter` (no texture file). The server writes both buffer sizes into the open packet, so a client with other defaults still builds matching slots. The status line priority is: no rail, warehouse offline, no data line, sending (item and target block name), put-away blocked (the Inbound Station texts), To storage full, waiting for a shuttle, idle.
- No docs icon existed for the old block name, so there was none to rename.
