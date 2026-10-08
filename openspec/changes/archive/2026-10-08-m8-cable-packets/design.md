# Design

## Context

The data network (`foundation.data`) keeps data nodes (Utility Ducts with the Data line, `DataNode`) in a `NetworkGraph` and members beside it in `DataMembers`, found by the production and stock capabilities. Its `onTick` does nothing yet. Datapack values elsewhere in Foundation load through `SimpleJsonResourceReloadListener` subclasses registered in `ServusLogisticsEvents` (`DuctStatsManager`, `TankStatsManager`); the packet listener registers itself in `foundation.data.packet.DataPacketEvents` next to the stash tick. See proposal.md for why and specs/foundation/data-packets/spec.md for the behavior.

## Goals / Non-Goals

**Goals:**
- A small, stable delivery API for the Access Point and the CPU: start a delivery, poll or listen to it.
- Route, flight and delivery bookkeeping as pure Java with JUnit tests.
- No item is ever voided.

**Non-Goals:**
- Client rendering, persistence of packets across restarts, any player facing block.

## Decisions

### Packages and classes
`foundation.data.packet`:
- Pure (no Minecraft imports): `PacketRoutes.shortest(starts, goals, neighbours)` (breadth first search over `NodePos`, deterministic order), `PacketFlight` (the route including the source and target positions, progress in blocks, `advance(blocksPerTick)`, `arrived()`, `current()` node), `PacketLimits` record (`size`, `blocksPerSecond`, `retryTicks`, defaults 8, 4.0, 20), `Delivery` (id, requested, delivered, state `RUNNING`, `DONE`, `FAILED`, reason, end listeners) and `DeliveryReason` (`NO_ROUTE`, `ROUTE_BROKEN`, `NO_TARGET`).
- Minecraft side: `PacketLimitsManager` (reload listener for `data/<ns>/data/packets.json`, file `servus_foundation:packets`), `Shipment` (one running delivery inside a network: source and target positions, either a fixed stack to send or an item and amount to take, the packet in flight, the stack waiting at the target, a retry timer), `PacketStash` (`SavedData` per level holding stacks that could not be placed yet, dropped when their position is loaded), `DataPackets` (static entry points `send`, `move`, `delivery(id)`, `settleAll` for server stop).

Ids come from one counter per server in `DataPackets`; deliveries are kept in a map by id until a while after they end (1200 ticks), so pollers see the final state.

### API
```java
Optional<Delivery> DataPackets.send(ServerLevel level, BlockPos from, BlockPos to, ItemStack stack)
Optional<Delivery> DataPackets.move(ServerLevel level, BlockPos from, BlockPos to, ItemStack like, int amount)
Optional<Delivery> DataPackets.delivery(long id)
```
`send` takes a stack the caller already holds (the Access Point later), `move` takes from the source member when each packet leaves. Both return a delivery that is `FAILED` with `NO_ROUTE` at once when `from` and `to` are not on one data network or no route links them; `send` then leaves the stack with the caller (the returned delivery says so, the caller keeps its stack). Empty only for invalid input (empty stack, amount below 1).

### Routing
Starts are the data nodes next to `from` whose side toward it is open (or `from` itself when it is a data node); goals the same for `to`. The route is computed when a packet leaves, so a later packet of the same delivery uses the network as it is then. Distance counts every step between consecutive positions of the route, endpoints included.

### Ticking
`DataNetwork.onTick` ticks its shipments. A shipment without a packet in flight launches the next one (size from `PacketLimitsManager` at launch), a packet advances `blocksPerSecond / 20` per tick, an arriving packet inserts into the target, remainders wait and retry. When nothing remains, nothing is in flight and nothing waits, the delivery is `DONE`. A move whose source gives nothing more also ends `DONE`, with the delivered count.

### Insertion and extraction
Target: item handler capability on the face of the target that touches the last duct of the route (`ItemHandlerHelper.insertItemStacked`), else the stock sink. Source of a move: stock provider (`take`), else item handler on the touching face, extracting stacks matching `ItemStack.isSameItemSameComponents`.

### Broken routes
`onNodeRemoved` marks shipments whose current packet route contains the removed position. On the next tick a marked packet re-routes from its current duct if that duct is still in this network and a route to the target exists; otherwise it is settled and the delivery fails with `ROUTE_BROKEN`. Settling inserts into the target if loaded, the rest into the source if loaded, the rest drops as an item entity at the first loaded of target, source and current duct, and anything left goes into `PacketStash`, which drops it as soon as one of those positions is loaded (checked on level tick only while the stash is not empty). `onAbsorb` takes over the other network's shipments; `onDisposed` settles all of its shipments. `ServerStoppingEvent` settles every shipment of every level before the worlds save.

### Command
`/servus_foundation data move <from> <to> <item> <count>` (permission level 2) in a new `foundation.command.ServusFoundationCommands` registered on `RegisterCommandsEvent`; reports the id or the failure reason.

### NeoForge APIs
- `SimpleJsonResourceReloadListener`, `AddReloadListenerEvent` (as `TankStatsManager`).
- `Capabilities.ItemHandler.BLOCK`, `ItemHandlerHelper.insertItemStacked`, `ServusFoundationCapabilities.STOCK_PROVIDER` and `STOCK_SINK`.
- `SavedData` with `SavedData.Factory` and `level.getDataStorage().computeIfAbsent` (1.21 signature with `HolderLookup.Provider` in load and save).
- `ServerStoppingEvent` (`net.neoforged.neoforge.event.server`), `RegisterCommandsEvent`, `LevelTickEvent.Post`.

### Testing
- JUnit: `PacketRoutesTest` (straight line, shorter branch, no route, start equals goal), `PacketFlightTest` (travel time 7 blocks at 4 per second is 35 ticks, current node, re-route keeps position), `DeliveryTest` (counting, done, failed, listeners once).
- GameTests in Storage (`DataPacketGameTests`): move from controller to station, travel time, send splits into packets, target full waits and retries, source short ends done, no route fails and keeps items, broken duct with detour arrives, broken duct without detour keeps the items.
- Manual: the command in game.

### PLAN.md open decisions
Settles "one packet in flight per delivery" as the reading of the throughput formula in 2.4. No section 5 item.

## Risks / Trade-offs

- [One packet in flight per delivery makes long routes slow] -> intended by the plan's throughput rule; more Access Points or a High-Speed cable add throughput later.
- [Settling teleports items to the target when a route breaks] -> rare, keeps items safe; the delivery reports `ROUTE_BROKEN` so callers can react.
- [Packets are not saved] -> settled at server stop; a crash can lose packets in flight, which is acceptable for an unreleased mod and noted here.
