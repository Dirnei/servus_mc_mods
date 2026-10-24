## Why

Handing a few stacks into the Storage Terminal's Input row makes the Packing Station behave oddly: items cross the line one by one, the first item of a stack already gets a put-away planned, its buffer slot locks, and the next item needs a new slot. Nine single items fill the 9 slot To storage buffer, the station reports "To storage full" and a shuttle drives off with a handful of items instead of a stack. Several Input slots also race for the same buffer slots, and the terminal picks the Packing Station by straight distance, not by where the items will be stored.

This change belongs to PLAN.md milestone M8 (On-demand autocrafting, Packing Station and Storage Terminal). It is the second of three changes and builds on `m8-one-warehouse-per-line`; `m8-wtp-egress` reuses its handshake for the way out of storage. Ingress means items moving into the warehouse, seen from the warehouse.

## What Changes

- **Delivery handshake in Foundation (WTP)**: a block can expose a packet endpoint. Before a delivery to it starts, the sender offers the item and the total; the endpoint answers with a grant (an amount and a ticket, with room reserved for it) or a refusal with a reason. The delivery then sends only what was granted, puts it only into the reserved room, and closes the grant when it ends, done or short. Targets without an endpoint work as today.
- **Packing Station grants whole slots**: an offer into To storage reserves whole empty slots of the buffer, as many as the amount needs and as are free, so a grant for 200 cobblestone with two free slots is 128. No other delivery can use reserved slots. A station refuses with a reason when the warehouse is offline, no rack would take the item, or no slot is free.
- **Put-away waits for the grant**: a slot reserved for a grant is put away only once the grant is filled or has ended, so one shuttle takes the whole stack.
- **The warehouse chooses the Packing Station**: senders into storage (the Input row, a Crafting CPU's outputs and leftovers, wireless transfers without a requesting terminal) ask the warehouse, which offers to its Packing Stations on the sender's line in order of the shuttle trip cost from the station to the rack the put-away rule picks, and uses the first that grants.
- **Rate**: the Packing Station's example rate becomes 10 items per second, one item every 2 ticks.
- **Grants end safely**: a grant ends with its delivery; grants whose delivery is gone are swept; after a reload a Packing Station keeps no grants, and what already arrived is put away.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `foundation/data-packets`: new requirements "Packet endpoint handshake" and "Grants end with their delivery"; "Delivery state" gains the refused failure.
- `shuttles/packing-station`: "Packets are put away" reserves whole slots and waits for the grant; "Conversion rate per direction" uses the example rate of 10; new requirement "The warehouse chooses the Packing Station for ingress".
- `shuttles/storage-terminal`: "Input row" goes to the Packing Station the warehouse chooses and waits with a reason when no station grants.

## Impact

- Foundation: `data/packet` gains the endpoint capability (`PacketEndpoint`, `Grant`, `servus_foundation:packet_endpoint`), `Shipment` and `DataPackets.move` offer, insert as a grant and close it, `DeliveryReason.REFUSED`, a registry of running deliveries for the sweep.
- Shuttles: `packing/PackingStationBlockEntity` exposes the endpoint for its To storage buffer with grants as IN locks on `lock/LockedSlots`; `network/WarehouseNetwork` gets the ingress choice, skips granted slots in `drain`, leaves grant locks to their station in `sweepLocks`; `terminal/StorageTerminalBlockEntity.storeInput`, `cpu/CpuCollector`, `cpu/CpuBlockEntity` (leftovers, buffer on removal) and wireless arrivals in `PackingStationBlockEntity` go through the ingress choice; `packing_station.json` rate 10; lang via datagen.
- Pure logic with JUnit: slot grant planning (whole slots, amount per slot), ingress station ranking.
- GameTests: Input row and Packing Station tests that count buffer slots or timings at 8 per second.
- Saves: grants are not saved.

## Non-goals

- The way out of storage, request slots and the stuck To network buffer (`m8-wtp-egress`).
- Endpoints on blocks other than the Packing Station's To storage side.
- A queue of waiting offers at a station; refused senders try again on their own interval.
- Pulsed transfer (whole seconds' worth at once).
