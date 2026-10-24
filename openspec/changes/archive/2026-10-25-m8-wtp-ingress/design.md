# Design

## Context

Today a delivery (`foundation/data/packet/Shipment`) asks the target each tick how much it accepts (`PacketIo.acceptable`) and inserts through the packet inlet. The Packing Station's To storage buffer is the `LockedSlots` buffer it inherits from `InboundStationBlockEntity`; `WarehouseNetwork.drain` plans a `PutAwayJob` for every unlocked stack as soon as it appears and locks the slot (OUT), and a locked slot refuses inserts. With the rate trickling one item every few ticks, each item lands in a new slot (see proposal). Senders into storage are `StorageTerminalBlockEntity.storeInput` (straight distance `nearestPackingStation`), `CpuCollector` and `CpuBlockEntity.fromBuffer`/`emptyBuffer` (`collectionPoint`, lowest position), and wireless arrivals without a terminal (`PackingStationBlockEntity` inserting into `intake()` while `intakeRoom` allows).

"Ingress" in this design means items moving into the warehouse; the UI keeps "To storage". `m8-one-warehouse-per-line` is assumed applied: a line has one warehouse.

PLAN.md leaves no open decision for this change; the explore session settled the handshake (WTP), whole slot grants, every sender reserving, the rate and the grant lifetime.

## Goals / Non-Goals

**Goals:**
- A handshake in Foundation that any target can implement and any delivery uses automatically.
- No fragmentation of the To storage buffer and one shuttle trip per granted slot.
- One place in the warehouse that chooses the Packing Station for ingress.

**Non-Goals:**
- Endpoints on terminals, Crafting Stations or the To network side (`m8-wtp-egress` adds the terminal).
- Saving grants.

## Decisions

### Foundation: `PacketEndpoint` and `Grant`
New sided block capability `ServusFoundationCapabilities.PACKET_ENDPOINT` (`servus_foundation:packet_endpoint`, `BlockCapability.createSided` like `TRANSFER_GATE`), registered by blocks in `RegisterCapabilitiesEvent` (`ServusShuttlesEvents`).

```java
interface PacketEndpoint {
    Grant offer(ItemStack like, int amount);           // synchronous, server thread
    ItemStack receive(Grant grant, ItemStack stack);   // insert into the grant's room, returns the rest
    void close(Grant grant);                           // FIN: frees what grant.arrived() did not fill
}
final class Grant {                                    // made by the endpoint: Grant.of(endpoint, ticket, amount, now) or Grant.refused(reason)
    ItemStack deliver(ItemStack stack, long now);      // receive through the endpoint, counts arrived, at most left()
    void close();                                      // once; calls endpoint.close(this)
    // ticket(), amount(), arrived(), left(), refusal(), bound(), delivery(), lastUsed()
}
```

OFFER and ACK are one method call in the same tick, no simulated latency. `Grant.ticket` is the endpoint's own id for the reserved room (the Packing Station uses its lock id); a delivery binds its id to the grant when it starts. `GrantBook` is the shared helper an endpoint keeps its open grants in, with the sweep below.

Alternative considered: real messages over the line with a tick of latency each. It adds states (offer in flight, ack lost) without any gameplay value.

### Shipment uses the grant
`DataPackets.move(level, from, to, like, amount, source)` checks the target face for an endpoint. With one, it offers `amount`; a refusal fails the delivery at once with `DeliveryReason.REFUSED` and the refusal key on `Delivery`; a grant shrinks the delivery's requested amount to the granted amount. A new overload takes a `Grant` the caller already holds (the warehouse's ingress choice offers before it knows which station wins). `Shipment.take`/`offer` use `endpoint.receive(grant, ...)` instead of `PacketIo.insert`, and `acceptable` becomes the grant's remaining amount. Every end path (`delivery.done()`, `fail`, `settle`, network dispose through `settleAll`) calls `close(grant, delivered)` exactly once.

### Running deliveries registry for the sweep
`DataPackets` keeps the ids of running deliveries (added on create, removed on end). `DataPackets.running(long id)` lets an endpoint sweep: the Packing Station calls `GrantBook.sweep` every 20 ticks, which closes bound grants whose delivery no longer runs and unbound grants (held by a sender, like a wireless transfer into storage) that took no item for `grant_hold_ticks` (example 1200, `packets.json`). This covers a network that vanished without settling.

A send (a stack the caller holds) needs a grant for the whole stack, otherwise it fails as refused and the caller keeps the stack. A Shipment whose grant was closed by its endpoint (the block was removed) ends short.

### Packing Station grants as IN locks
The To storage buffer stays a non counted `LockedSlots`. A grant reserves whole empty unlocked slots with `SlotLock(Kind.IN, key, amountForSlot, ticketLockId)`; a slot with an IN lock refuses every insert except `insertAs(lockId)`, which is how `receive` fills it. The lock id comes from `WarehouseNetwork.nextJobId()` so it never clashes with job ids. `WarehouseNetwork.sweepLocks` must not release them: it skips IN locks whose id the station reports as one of its grants (`PackingStationBlockEntity.grantLocks()`).

Grant planning is pure: `packing/SlotGrant.plan(freeSlots, stackSize, amount)` returns the slots and amount per slot; JUnit covers partial grants, exact fits, zero free slots, small stack sizes.

Refusal reasons, in order: warehouse offline (`offline`), no storage takes the item (`no_storage`, from `WarehouseNetwork.putAwayTarget` being empty), no free slot (`to_storage_full`).

`close` releases the IN locks of the grant and marks the station pending for `drain`. `drain` already skips locked slots, so it waits for the grant by construction; after the release the normal put-away and `joinOpenPutAway` take over.

`onLoad` drops every IN lock of the To storage buffer (grants are not saved), so their items are put away.

### Ingress choice in the warehouse
`WarehouseNetwork.offerIngress(BlockPos sender, ItemKey key, int amount)` returns an `Ingress` record with the station position and its `Grant`, or the refusal together with the station asked first (the CPU drops a buffer it cannot send beside that station, as `shuttles/crafting-cpu` asks). Candidates are the warehouse's Packing Stations whose data network reaches the sender. They are ranked by `trip(target, station)`, the existing travel cost search (`fleet.costsFrom(dockRails(...))`) from the station to the rack `putAwayTarget` returns, lowest position on a tie, and offered in that order. The ranking is pure: `packing/IngressRanking` over (candidate, cost, position), with JUnit for order and ties. The cost search runs once per offer, not per tick.

Senders use it:
- `StorageTerminalBlockEntity.storeInput`: per input slot without a running move, `offerIngress` then `DataPackets.move(..., grant)`; a refusal sets `inputRefusal` to its key. `nearestPackingStation` goes.
- `CpuCollector` and `CpuBlockEntity.fromBuffer`/`emptyBuffer`: `collectionPoint` is replaced by `offerIngress`; a refusal keeps the items in the producer or buffer as today when the station was full, and `emptyBuffer` still drops what nothing grants.
- Wireless arrivals without a terminal: the sending station asks the receiver for a grant once per transfer (`offer` on the receiver's endpoint) and moves only into it, replacing `intakeRoom`.

### Rate
`packing_station.json` `items_per_second` becomes 10. `RateBudget` already carries fractions, so 10 per second is one item every 2 ticks; no code change for the rate.

### APIs
NeoForge `BlockCapability.createSided` and `RegisterCapabilitiesEvent.registerBlockEntity` (as for `TRANSFER_GATE`). No other new API.

## Risks / Trade-offs

- [A grant reserves a whole slot for a small amount, for example 3 crafted items] → The slot is freed as soon as the 3 arrive; `joinOpenPutAway` still lets same item slots share a trip.
- [Offers hammer the cost search when many Input slots retry] → Input slots only offer from `storeInput`, which runs on change or every 20 ticks; the CPU collector on its own interval.
- [Foreign mods sending packets to a Packing Station] → They go through `DataPackets.move`, which offers automatically, so they reserve like everyone else.
- [Delivery shrinks to the grant] → Callers that compare `delivered` with what they asked must read the granted amount; the terminal and CPU book what was delivered, which already handles short deliveries.

## Migration Plan

None. Grants are not saved; Packing Stations loaded from an old save have no IN locks.
