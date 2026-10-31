---
title: Deliveries
description: How items move over the Servus data line, the handshake that reserves room before items are sent, the item doors and gates of a member, and how to start a delivery from your own code.
---

The data line moves items only when something asks it to: a Crafting CPU feeding a producer, a warehouse answering an upkeep request, a terminal request. Each such move is a **delivery**. The types on this page are in `io.github.dirnei.mc.servus.foundation.data.packet` unless named otherwise. This is Foundation %foundation_version%.

## How a delivery moves

- **Instant.** Items do not travel and distance does not matter. Both ends must be on the same data network, linked by a route of data nodes.
- **Every tick**, a delivery moves as many items as the source has, the target takes, both transfer gates allow and every duct on the path still has budget for (see [Bandwidth](/servus_mc_mods/developers/data-line/#bandwidth)).
- **Items wait at the source.** A move takes items out only as far as the target accepts them. Nothing is ever in flight.
- **Nothing is lost.** When the route breaks, what a move has not moved stays at the source. A handed over stack goes back to the source, else into the target, else onto the ground at a loaded place.
- **A full target ends it.** A delivery into a target that takes nothing for `stall_ticks` (600 ticks) ends with what arrived, so it no longer holds up its sender. This does not apply to a target with a packet endpoint: there the grant decides how much is sent.

## The handshake

A block can answer **offers** before items are sent to it. It then reserves room for them, or says why not. A delivery into such a block sends only what was granted, so the sender never has to try until something fits, and the block never receives items it has no room for.

To take part, expose `ServusFoundationCapabilities.PACKET_ENDPOINT` on the face that touches the duct, with a `PacketEndpoint`:

| Method | Does |
|---|---|
| `Grant offer(ItemStack like, int amount)` | Answers an offer of `amount` items like `like` (a stack of one). Reserve room and return `Grant.of(this, ticket, granted, gameTime)`, or return `Grant.refused(reason)`. You may grant less than offered. |
| `ItemStack receive(Grant grant, ItemStack stack)` | Puts items of an open grant into the room it reserved. Return what did not go in. |
| `void close(Grant grant)` | The grant ended after `grant.arrived()` items. Free the room no item filled. |

`Grant` carries the `ticket()` you gave it, the granted `amount()`, `arrived()`, `left()` and `closed()`. The ticket is your own id for the reserved room; any number that is unique among your open grants will do.

`GrantBook` keeps your open grants and does the bookkeeping every endpoint needs:

- `add(grant)` after you granted, `remove(grant)` from your `close`,
- `holds(grant)` in `receive`, so an old or foreign grant puts nothing in,
- `grants()` to add up the room your open grants still hold (`left()`),
- `sweep(gameTime)` every tick or so. It closes grants whose delivery has ended, and grants that no delivery took up and that took no items for `grant_hold_ticks` (1200 ticks),
- `closeAll()` when your block goes away.

A refusal reason is a short string such as `input_full`. Servus uses it as the last part of a lang key and shows it to the player where it can, so keep it to lowercase letters and underscores.

```java
private final GrantBook grants = new GrantBook();
private long nextTicket = 1;

private final PacketEndpoint endpoint = new PacketEndpoint() {
    @Override
    public Grant offer(ItemStack like, int amount) {
        int room = roomFor(like) - reserved(like);
        if (level == null || room <= 0) {
            return Grant.refused("input_full");
        }
        Grant grant = Grant.of(this, nextTicket++, Math.min(amount, room), level.getGameTime());
        grants.add(grant);
        return grant;
    }

    @Override
    public ItemStack receive(Grant grant, ItemStack stack) {
        return grants.holds(grant) ? insert(stack) : stack;
    }

    @Override
    public void close(Grant grant) {
        grants.remove(grant);
    }
};
```

Here `roomFor`, `reserved` and `insert` are your machine's own code: how many items like this fit into the slot they belong in (input or fuel), how much of that your open grants already hold, and putting them there. Call `grants.sweep(level.getGameTime())` from your server tick and `grants.closeAll()` from `setRemoved`. Shuttles' Inbound Station, Packing Station and Storage Terminal are endpoints.

Item Pipes use the handshake too: a Pull side that brings items to your block offers them first and puts them into the grant.

## Item doors

Items enter and leave a member on the face that touches the data line:

| Direction | Tried in this order |
|---|---|
| Into the target | `PACKET_ENDPOINT` (through the grant), then `PACKET_INLET`, then `Capabilities.Item.BLOCK`, then `STOCK_SINK` |
| Out of the source | `STOCK_PROVIDER`, then `PACKET_INLET`, then `Capabilities.Item.BLOCK` |

`PacketInlets.handler(Level, BlockPos, Direction)` returns the handler the line would use without an endpoint, wrapped as Foundation's `io.github.dirnei.mc.servus.foundation.transfer.ItemView`, or `null`.

A packet inlet (`ServusFoundationCapabilities.PACKET_INLET`) has the same type as `Capabilities.Item.BLOCK` but its own id. Use it when the data line should reach slots that pipes and hoppers must not. A packet inlet or a packet endpoint alone does not make a block a member, and a target needs one of the inlet, the item capability or a stock sink to count as a target at all.

## Transfer gates

A member that exposes `ServusFoundationCapabilities.TRANSFER_GATE` limits how fast items enter or leave it. `TransferGate` (in the `production` package) has:

- `int allowance(TransferGate.Flow flow)`: how many items may pass this tick, for `Flow.IN` or `Flow.OUT`,
- `void consume(TransferGate.Flow flow, int items)`: told how many passed.

A delivery asks the gate of its source for `OUT` and the gate of its target for `IN`, each tick, and moves no more than both allow. Members without a gate do not limit a delivery. The Shuttles Packing Station is a gate.

## Starting a delivery

`DataPackets` starts deliveries. Call it on the server thread only.

```java
// Move up to 16 iron ingots from the member at `from` to the member at `to`.
Optional<Delivery> started = DataPackets.move(level, from, to, new ItemStack(Items.IRON_INGOT), 16);
```

| Method | Does |
|---|---|
| `send(ServerLevel, BlockPos from, BlockPos to, ItemStack stack)` | Hands over a stack you hold. If it fails at once, you keep the stack; otherwise the network owns it and offers what does not fit again every `retry_interval` ticks. Into a target with an endpoint it starts only when the whole stack is granted. |
| `move(ServerLevel, BlockPos from, BlockPos to, ItemStack like, int amount)` | Takes up to `amount` items like `like` out of the source member as the target accepts them. Into a target with an endpoint it asks for no more than was granted. |
| `move(..., ItemView source)` | Like `move`, but takes the items from `source` instead of the member's own handler. The member at `from` still decides the route and its gate. |
| `offer(ServerLevel, BlockPos from, BlockPos to, ItemStack like, int amount)` | Asks the target for room before you have the items ready. Returns the `Grant`, or empty when the target has no endpoint or no route links the two. |
| `send(..., ItemStack stack, Grant held)` and `move(..., ItemView source, Grant held)` | Start the delivery into a grant you already hold from `offer`, instead of offering again. |

A grant you hold belongs to you until a delivery takes it up: close it yourself with `grant.close()` if you do not send, else the endpoint's sweep closes it after `grant_hold_ticks`. A delivery started with a grant closes it when it ends, and one that cannot start closes it at once.

Each method returns `Optional<Delivery>`. It is empty only for an empty stack or an amount below 1. A delivery that cannot start is returned **already failed**, so always check its state.

## The Delivery object

`Delivery` tells you how it went:

- `id()`, `requested()` and `delivered()`,
- `state()`: `Delivery.State.RUNNING`, `DONE` or `FAILED`, and `ended()`,
- `reason()`: why it failed, a `DeliveryReason`, or `null`:
  - `NO_ROUTE`: no route links the two members,
  - `ROUTE_BROKEN`: the route broke while it ran,
  - `NO_TARGET`: the target takes no items on that face, or its grant was used up or closed; a loaded target whose block was broken or replaced while the delivery waited fails this way at once,
  - `REFUSED`: the target's endpoint refused; `refusal()` gives its reason,
  - `STALLED`: a target without an endpoint took nothing for `stall_ticks`,
- `onEnd(Consumer<Delivery>)`: runs once when it ends, or at once if it already has.

```java
started.ifPresent(delivery -> delivery.onEnd(ended -> {
    if (ended.state() == Delivery.State.FAILED) {
        // ended.reason() says why; ended.delivered() items did arrive.
    }
}));
```

`DataPackets.running(long id)` tells whether the delivery with that id still runs.

Deliveries are **not saved**. When the server stops, Foundation settles every running delivery: items go back to their source or somewhere safe, and listeners do not run again after a restart. Ids start again at 1 on every start. Keep your own record if you need to resume after a restart. Grants are not saved either; an endpoint starts with none.

## Suppliers

A member that can send items on request exposes `ServusFoundationCapabilities.SUPPLIER` with an `ItemSupplier`:

- `Supply supply(BlockPos target, ItemStack like, int amount)`: send up to `amount` items like `like` to the data member at `target`, by your own means. Answer with a `Supply(amount, ticket)`: how many will come and a ticket that names this request, or `Supply.NONE` when none will.
- `boolean open(long ticket)`: whether the request is still on its way. A ticket you do not know is not open. Once you have sent everything, or given up, answer `false`.
- `void cancel(long ticket)`: give up the request and take back what you had not sent yet. Do nothing for a ticket you do not know.

The upkeep holder saves the ticket with its block, and after a restart the network asks `open` about it again. So a ticket should stay valid across a restart: the Shuttles warehouse uses the id of its saved shipment. A supplier that no longer knows a ticket answers `false`, and the request ends without a cancel.

The upkeep check asks suppliers only among the members of the network, so a supplier must also be a member (for example through `STOCK_PROVIDER` or `DATA_LINK`). In Shuttles %shuttles_version% the Warehouse Controller and the Packing Station are suppliers.

## Data

The timings are datapack data in `data/<namespace>/data/packets.json`:

```json
{
  "retry_interval": 20,
  "grant_hold_ticks": 1200,
  "stall_ticks": 600
}
```

| Field | Default | Meaning |
|---|---|---|
| `retry_interval` | 20 | Ticks before a delivery whose target was full tries again. |
| `grant_hold_ticks` | 1200 | How long a grant that no delivery took up stays open without taking items. |
| `stall_ticks` | 600 | How long a delivery without a grant may move nothing into a full target before it ends as `STALLED`. |
