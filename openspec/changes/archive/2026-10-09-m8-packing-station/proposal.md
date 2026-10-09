# Proposal

## Why

Play test of PLAN.md M8 (on-demand autocrafting): the Access Point name says where it sits, not what it does, its GUI shows only one of its two buffers and lets players pull items out of system buffers, and throughput is set by the cable (packet size, travel speed and distance) instead of by a block the player can see and add more of. Worst of all, the Storage Terminal hands whole stacks to the network at once (`StorageTerminalBlockEntity.storeInput` calls `DataPackets.send` per slot), so the Input row empties immediately even when the Access Point's intake is full; the items then sit invisible in flight and are lost on a crash.

This change is a play test follow-up to the M8 list, added as item `m8-packing-station`. It makes the converter between the physical warehouse and the data line an explicit, visible limiter.

## What Changes

- **Rename:** Access Point becomes **Packing Station** everywhere (block and item id `servus_storage:packing_station`, classes, lang, textures, docs, PLAN.md, specs). Tooltip: "Packs items from the warehouse into packets for the data line, and unpacks arriving packets for storage". Unreleased: no migration, placed Access Points vanish.
- **Read-only GUI:** two labelled rows, **To storage** (items waiting for put-away) and **To network** (picked items waiting to be sent), each with a tooltip, plus a status line naming the current delivery (item and target) or why it waits. Nothing can be taken out; breaking the block still drops both buffers.
- **Throughput model:** the data network becomes instant: no packet size, no travel speed, no travel time. The **Packing Station is the limiter**: it converts items to packets and packets to items at `items_per_second` per direction (example 8, datapack JSON), the two directions independent. More stations add throughput.
- **Backpressure:** items stay at their source until the station can take them. The terminal's Input row shrinks as the station converts (move semantics instead of handing whole stacks over); "To storage" accepts only while it has room; shuttle picks fill "To network" only as fast as it drains.
- Transfers that cross no Packing Station (machine to machine, machine to terminal, crate or controller stock to a member) are instant and limited only by what the target accepts.
- `packets.json` loses `packet_size` and `packet_speed`; Packing Station limits live in Storage data.
- PLAN.md: M8 list item, 2.4 data line and Access Point text, "items travel with a speed" removed, "Later: High-Speed Data Cable" replaced by `m8-data-bandwidth`.

## Capabilities

### New Capabilities
None. `storage/access-point` is renamed to `storage/packing-station` (moved, not new).

### Modified Capabilities
- `storage/packing-station` (moved from `storage/access-point`): put-away and shipping go through the rate limited buffers, read-only GUI replaces the intake GUI, tooltip, conversion rate.
- `foundation/data-packets`: deliveries are instant and gated by the members they pass; no packet size, travel speed or travel time; items wait at the source instead of in flight.
- `storage/storage-terminal`: the Input row drains at the Packing Station's pace and keeps what the station cannot take yet.

## Non-goals

- Cable tiers with bandwidth, simplex and duplex, in-world cable replacement, Packing Station speed upgrades and the load overlay: `m8-data-bandwidth`.
- A queue of upcoming deliveries in the GUI (the status line shows the current one).
- Rendering packets on the client.
- Changing how shuttles choose racks or which Packing Station a shipment uses (shortest shuttle trip stays).

## Impact

- `storage`: `access` package renamed (`AccessPoint*` to `PackingStation*`), its menu and screen (today the Inbound Station GUI), `StorageTerminalBlockEntity.storeInput`, `WarehouseNetwork.ship` picks into the outgoing buffer, lang, datagen, textures, GameTests that build Access Points, `warehouse.json` or a new `packing_station.json` for the rate.
- `foundation`: `data.packet` (`Shipment`, `PacketFlight`, `PacketLimits`, `PacketLimitsManager`, `DataPackets`), a small gate API so Storage can limit transfers through its station, `packets.json`.
- Docs pages, `gamedata.json`, the docs icon, PLAN.md.
- Dev worlds lose placed Access Points.
