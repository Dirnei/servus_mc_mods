---
title: Deliveries
description: How items move over the Servus data line, the item doors and gates of a member, and how to start a delivery from your own code.
---

The data line moves items only when something asks it to: a Crafting CPU feeding a producer, a warehouse answering an upkeep request, a terminal request. Each such move is a **delivery**. The types on this page are in `io.github.dirnei.mc.servus.foundation.data.packet` unless named otherwise. This is Foundation 0.4.0.

## How a delivery moves

- **Instant.** Items do not travel and distance does not matter. Both ends must be on the same data network, linked by a route of data nodes.
- **Every tick**, a delivery moves as many items as the source has, the target takes, both transfer gates allow and every duct on the path still has budget for (see [Bandwidth](/servus_mc_mods/developers/data-line/#bandwidth)).
- **Items wait at the source.** A move takes items out only as far as the target accepts them. Nothing is ever in flight.
- **Nothing is lost.** When the route breaks, what a move has not moved stays at the source. A handed over stack goes back to the source, else into the target, else onto the ground at a loaded place.

## Item doors

Items enter and leave a member on the face that touches the data line:

| Direction | Tried in this order |
|---|---|
| Into the target | `PACKET_INLET`, then `Capabilities.Item.BLOCK`, then `STOCK_SINK` |
| Out of the source | `STOCK_PROVIDER`, then `PACKET_INLET`, then `Capabilities.Item.BLOCK` |

`PacketInlets.handler(Level, BlockPos, Direction)` returns the handler the line would use, wrapped as Foundation's `io.github.dirnei.mc.servus.foundation.transfer.ItemView`, or `null`.

A packet inlet (`ServusFoundationCapabilities.PACKET_INLET`) has the same type as `Capabilities.Item.BLOCK` but its own id. Use it when the data line should reach slots that pipes and hoppers must not. A packet inlet alone does not make a block a member.

## Transfer gates

A member that exposes `ServusFoundationCapabilities.TRANSFER_GATE` limits how fast items enter or leave it. `TransferGate` has:

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
| `send(ServerLevel, BlockPos from, BlockPos to, ItemStack stack)` | Hands over a stack you hold. If it fails at once, you keep the stack; otherwise the network owns it and offers what does not fit again every `retry_interval` ticks. |
| `move(ServerLevel, BlockPos from, BlockPos to, ItemStack like, int amount)` | Takes up to `amount` items like `like` out of the source member as the target accepts them. |
| `move(..., ItemView source)` | Like `move`, but takes the items from `source` instead of the member's own handler. The member at `from` still decides the route and its gate. |

Each returns `Optional<Delivery>`. It is empty only for an empty stack or an amount below 1. A delivery that cannot start is returned **already failed**, so always check its state.

## The Delivery object

`Delivery` tells you how it went:

- `id()`, `requested()` and `delivered()`,
- `state()`: `Delivery.State.RUNNING`, `DONE` or `FAILED`, and `ended()`,
- `reason()`: why it failed, a `DeliveryReason` (`NO_ROUTE`, `ROUTE_BROKEN` or `NO_TARGET`), or `null`,
- `onEnd(Consumer<Delivery>)`: runs once when it ends, or at once if it already has.

```java
started.ifPresent(delivery -> delivery.onEnd(ended -> {
    if (ended.state() == Delivery.State.FAILED) {
        // ended.reason() says why; ended.delivered() items did arrive.
    }
}));
```

Deliveries are **not saved**. When the server stops, Foundation settles every running delivery: items go back to their source or somewhere safe, and listeners do not run again after a restart. Ids start again at 1 on every start. Keep your own record if you need to resume after a restart.

## Suppliers

A member that can send items on request exposes `ServusFoundationCapabilities.SUPPLIER` with an `ItemSupplier`:

- `int supply(BlockPos target, ItemStack like, int amount)`: send up to `amount` items like `like` to the data member at `target`, by your own means, and answer how many will come, `0` when none.

The upkeep check asks suppliers only among the members of the network, so a supplier must also be a member (for example through `STOCK_PROVIDER` or `DATA_LINK`). In Shuttles 0.4.0 the Warehouse Controller and the Packing Station are suppliers.

## Data

The retry interval is datapack data in `data/<namespace>/data/packets.json`:

```json
{
  "retry_interval": 20
}
```
