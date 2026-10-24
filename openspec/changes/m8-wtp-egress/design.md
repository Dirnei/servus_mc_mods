# Design

## Context

`PackingStationBlockEntity.ship` runs every `pushInterval` ticks. It returns at once while a data delivery runs (`dataSending()`), otherwise starts one `DataPackets.move` for the first non wireless shipment with items in the To network buffer, and only when nothing starts does it call `returnOrphans()`. A delivery whose target stays full retries every `retry_interval` forever (`Shipment.take`), so one full Output slot holds up the station (see proposal). Picks are planned and their rack items locked when the request is made (`WarehouseNetwork.shipThrough`, `startJobs`); `WarehouseNetwork.readyToSend(PickJob)` dispatches a pick only when the destination's `deliveries()` buffer (the To network buffer) can lock room for it. A terminal request owns one output slot (`Request.slot`, pure slot choice in `terminal/`).

"Egress" in this design means items moving out of the warehouse; the UI keeps "To network". `m8-wtp-ingress` is assumed applied: `PacketEndpoint`, `Grant`, `DataPackets.move` with a held grant, the running delivery registry and `DeliveryReason.REFUSED` exist.

PLAN.md leaves no open decision for this change; the explore session settled pick on grant, several output slots per request, re-offer instead of blocking and the stall timeout.

## Goals / Non-Goals

**Goals:**
- A shipment that cannot be delivered never holds up another.
- Nothing is picked into the To network buffer that its target cannot take.
- Deliveries to targets without an endpoint end on their own.

**Non-Goals:**
- Endpoints on Crafting Stations, CPUs and Machine Adapters.
- Changing how wireless relays reach the terminal beyond the terminal's endpoint, which they meet through `DataPackets.move` like any delivery.

## Decisions

### Storage Terminal endpoint
`StorageTerminalBlockEntity` registers `PACKET_ENDPOINT` next to its packet inlet. `offer(like, amount)`:
- an open request waiting for the item: grant `min(amount, room)` where room is the free space of the request's slots less the grants it already holds;
- no request waiting: grant the room of the Input row, as the inlet takes such packets today;
- otherwise refuse `output_full`.

`receive` inserts into the request's slots (or the Input row); `close` drops the grant. A grant's room is a counter, not a slot lock, because only the request's own deliveries can land in its slots anyway.

### Several output slots per request
`Request.slot` becomes `Request.slots` (list). The pure slot choice (`terminal` slot chooser) returns up to `ceil(amount / stackSize)` slots: the same item with room first, then the leftmost free empty slots, at least one. Saving writes `slots` as an int list; loading also reads the old single `slot` key as a list of one. Requests adding to an open request keep their slots; when a request grows, it takes more free slots up to what it now needs.

### Station: grant, admit, send
Each non wireless `Shipment` of the station holds at most one `Grant` and an `admitted` count.

1. **Offer**: in `ship`, a shipment with `missing() > 0`, no grant and an endpoint at the target face offers `missing()` minus what is already in the buffer for it. A refusal sets the shipment's `waitingReason` (shown in the status line) and is tried again on the next push interval.
2. **Admit**: `WarehouseNetwork.readyToSend(PickJob)` for a Packing Station destination also asks `station.admits(job)`: the job's shipment has granted room not yet admitted. Admitting adds the job's amount to `admitted`. Jobs for a shipment whose target has no endpoint are admitted as today, by buffer room only. Picks stay planned and their rack locks stay, so the requestable count and other requests are unaffected.
3. **Send**: once the buffer holds the admitted items for a grant, or the admitted jobs have all ended, the station starts `DataPackets.move` with the held grant for what is there. The grant ends with that delivery, and the next round offers again for what is still missing.

Several deliveries run at once, at most one per shipment; they share the station's `toNetwork` budget through `TransferGate` as deliveries of one gate already do. `dataSending()` no longer gates the loop, so `returnOrphans()` runs every push interval. The status line picks the running delivery, else the oldest shipment's waiting reason.

Pure logic: `packing/PickAdmission` (granted, admitted, job amount, has endpoint, buffer room) and the terminal's grant room (slot counts, stack size, held grants), both with JUnit.

Alternative considered: keep picking ahead and only fix the blocking. Simpler, but parked items still fill the To network buffer, which is a second way into the same clog.

### Held grants and stalls in Foundation
`PacketLimits` already has `grantHoldTicks` (`grant_hold_ticks`, example 1200) from `m8-wtp-ingress`, and `GrantBook.sweep` already ends grants not bound to a delivery after it; this change adds `stallTicks` (`stall_ticks`, example 600), read by `PacketLimitsManager` from `data/servus_foundation/data/packets.json`. A grant held by the station is used through `Grant.deliver` and `DataPackets.move(..., held)`. `Shipment` counts ticks without a moved item when it has no grant and fails the delivery with `DeliveryReason.STALLED` after `stallTicks`. The station treats a stalled delivery like a short one: the ledger books what arrived, the shipment stays open with its missing amount and is retried, and `returnOrphans` sends items no shipment wants back to To storage.

### CPU deliveries to a terminal
`CpuCollector` moves craft results to `order.deliverTo()`, which is now a terminal endpoint. A refused or partly granted delivery leaves the rest in the producer or CPU buffer; the collector already retries each round, and it must not open a ledger transfer for a delivery that failed at once (`state() == FAILED`).

### APIs
No new NeoForge API; the endpoint capability is registered like in `m8-wtp-ingress`. Datapack values through the existing `PacketLimitsManager` reload listener.

## Risks / Trade-offs

- [Second stack of a big request waits a shuttle trip after the slot empties] → Only when the request got fewer slots than stacks; with free slots it owns enough room up front.
- [A request takes several Output slots and fills the row] → It takes only free slots and at least one; the row full message tells the player to take items.
- [Grant held while a shuttle is stuck] → `grant_hold_ticks` frees the room; the station offers again.
- [Old saves with `slot`] → Loaded as a list of one.

## Migration Plan

Requests saved with a single `slot` load as a list of one. Nothing else is saved.
