## Why

A Packing Station's To network buffer clogged in play and only breaking the station cleared it. The cause: a request for 128 copper owns one Output slot, so after 64 the slot is full and the delivery of the rest retries forever. A Packing Station runs one data line delivery at a time, so every other shipment (iron, bread, a CPU's inputs) waits behind the copper until the player happens to empty the slot, and items that nobody waits for are never sent back to storage. Picks for the rest also park in the To network buffer, so a few large requests can fill it on their own.

This change belongs to PLAN.md milestone M8 (On-demand autocrafting, Packing Station and Storage Terminal). It is the third of three changes and builds on `m8-wtp-ingress`, whose delivery handshake (WTP) it uses for the way out of storage. Egress means items moving out of the warehouse, seen from the warehouse.

## What Changes

- **The terminal grants room**: the Storage Terminal answers offers for its requests with a grant of the room left in the request's output slots, or a refusal when they are full. Deliveries to it never wait for room.
- **A request owns several Output slots**: a request owns as many whole Output slots as its amount needs at the item's stack size, as far as they are free, at least one. A request for 128 copper with free slots gets two and the copper arrives in one go; with one free slot the rest waits until the player takes the first stack.
- **No head-of-line blocking**: a Packing Station sends a shipment only as far as its target grants; a refused shipment waits as data without a running delivery, is offered again every push interval, and the station serves its other shipments meanwhile.
- **Pick on grant**: shuttles bring picks for a shipment into the To network buffer only up to what its target has granted, so nothing parks there that nobody can take. The picks stay planned and their rack items stay locked.
- **Stall timeout**: a delivery to a target without an endpoint that moves nothing for `stall_ticks` (example 600, datapack JSON) ends short with what arrived; its shipment stays open and is tried again later without holding up the station, and items no shipment wants go back into storage.
- **Status**: the Packing Station's status line names a target that refuses (for example "Waiting for room at the Storage Terminal") in the alert colour.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `foundation/data-packets`: new requirements "Stalled deliveries end" and "Held grants expire" (a grant taken before the items arrive ends after `grant_hold_ticks`, example 1200).
- `shuttles/packing-station`: "Shipping to a data member by shuttle" picks on grant and serves shipments side by side; "Read-only buffer GUI" names a refusing target.
- `shuttles/storage-terminal`: "Output slots" and "Output row full" let a request own several slots and grant room; "Request slots survive a reload" and "Slot choice is pure" cover several slots.

## Impact

- Foundation: `data/packet/Shipment` stall counter, `PacketLimits`/`PacketLimitsManager` read `stall_ticks` from `data/servus_foundation/data/packets.json`, `DeliveryReason.STALLED`.
- Shuttles: `terminal/StorageTerminalBlockEntity` exposes the endpoint for request slots and owns several slots per request (saved as a list); `terminal/TerminalRequests` and the pure slot choice pick several slots; `packing/PackingStationBlockEntity.ship` offers per shipment, runs several deliveries, re-offers each push interval; `network/WarehouseNetwork` and the fleet admit pick room in the To network buffer only up to a shipment's grant; `cpu/CpuCollector` retries refused deliveries to a terminal; lang via datagen.
- Pure logic with JUnit: several slot choice, terminal grant from slot room, pick admission by grant.
- GameTests: request and Packing Station tests that expected one slot per request or a waiting delivery.
- Saves: requests save a list of slots; a saved single slot loads as a list of one.

## Non-goals

- Data Potato inspect of a Packing Station's deliveries (PLAN.md keeps "why items are stuck" open).
- Endpoints on Crafting Stations, CPUs or Machine Adapters; they are covered by the stall timeout.
- A queue that orders waiting shipments by age beyond the station's shipment order.
