# Design

## Context

`OrderBook` (storage/order) plans with `OrderPlanner`, keeps reservations and drives steps through a `Host`. Today `WarehouseNetwork` is the host: producers are Outbound Stations facing a machine, feeding goes into the station buffer, and Inbound Stations credit outputs through `OrderBook.collected`. Since `m8-item-terminal-cable` the terminal finds warehouses over the data line (`TerminalWarehouses.reachable`) and creates shortfall orders on the first warehouse. `WarehouseNetwork.ship(target, key, amount, requestId)` ships through the Access Point with the shortest shuttle trip and returns an `AccessPointBlockEntity.Shipment` (planned, delivered, ended). `DataPackets.move(level, from, to, like, amount)` moves items out of a member as packets and returns a `Delivery` with `onEnd` listeners. Multiblock precedent: the Manufacturing Sink (block states FORMED and PART, carrot forms, `onRemove` unforms the rest).

## Goals / Non-Goals

**Goals:**
- Reuse `OrderBook`, `WorkOrder`, `OrderStep` and the planner; swap the host.
- One order route: over the data line.
- Pure logic for the cube search and the CPU choice, unit tested.

**Non-Goals:**
- Order persistence, a CPU GUI, patterns.

## Decisions

### Blocks
- `storage/cpu/CpuBlock` (`servus_storage:crafting_cpu`): `BaseEntityBlock`, `Wrenchable`, states `FORMED` and `PART` (0..26, `x + 3z + 9y` from the cube's lowest corner). Part 0 is the master. Carrot on an unformed block calls `tryForm`; sneak carrot pops the block. `onRemove` of a formed block unforms the rest (like `SinkBlock.unformRest`) and tells the master to cancel. After forming and unforming every part gets `level.invalidateCapabilities`, so data line probes re-read.
- `CpuBlockEntity` on every part: exposes `servus_foundation:data_link` while formed (capability provider returns null when unformed). The master holds the `OrderBook` and ticks it; `setRemoved` and `onChunkUnloaded` of the master cancel all open orders.
- Textures: `crafting_cpu` (unformed casing) and `crafting_cpu_formed`, cube_all models chosen by `FORMED`. Recipe: iron ingots, redstone and a crafting table give 4 (`IRI/RCR/IRI`).

### Pure logic
- `CpuShape.origins(x, y, z)`: the 27 candidate lowest corners whose cube contains the clicked block, lowest first; the block forms the first cube whose 27 positions are unformed CPU blocks.
- `CpuChoice.choose(List<Load>)` with `Load(open, slots)` in position order: first with `open < slots`, else fewest open, earliest on a tie.

### Order book changes
- `OrderBook.Host` becomes: `producers()`, `stock()`, `feed(producer, key, amount)` returning `Feed(planned, refusal, List<Pending>)`, `collect(step)`, `busyElsewhere(producer, book)`, `slots()`, `gameTime()`. `WarehouseNetwork.Producer` moves to `OrderBook.Producer(pos, name, recipes, active)`; `OrderStep` keeps one position (`producer()`), the station and machine pair goes.
- `WorkOrder.State` gains `WAITING`; `tick` advances only the `slots()` oldest open orders, the others are WAITING with reason "waiting for CPU".
- Feeding records `Pending` shipments on the step; when one ends short, the shortfall is taken off `fed` and the step delivers again.
- `collected(inbound, ...)` goes. Collection: `collect(step)` runs every book tick for delivering and producing steps; the CPU counts extractable outputs of the step's recipe in the producer (max over the producer's sided item handlers), subtracts what is already travelling and starts `DataPackets.move` to an Access Point; the delivery's `onEnd` adds `delivered` to the step. Extra outputs are collected too and simply put away.
- One step per producer: `stationBusy` checks this book and `busyElsewhere` the other CPUs of the line.

### CPU host
- Data network: the first data network found at any of the 27 positions (`DataNetworks.networkAt`).
- Producers: members with `DataRole.PRODUCER`, `ProductionProvider` queried with side null; name from the block.
- Stock: `TerminalWarehouses.reachable` from a member position, entries added up.
- Feed: warehouses sorted by `available(key)` descending, `ship(producer, key, min(available, owed))` each, until covered. Refusals map to the blocked reasons (`needs_shuttle`, `no_access_point`, ...); no stock reads "missing <item>".
- Collection target: the Access Point of the first reachable warehouse whose data network reaches the producer, lowest position first.

### Who hands orders to a CPU
`CpuOrders` (storage/cpu): `network(level, pos)` resolves a data network from a data member, CPU block or warehouse block (via its Access Points); `cpus(level, network)` lists formed masters on the line sorted by position; `create(level, pos, key, amount, requestId)` picks with `CpuChoice` and returns the order plus the CPU position, or refusal `no_cpu`; `openAmountFor`, `hasOpenOrderFor`, `cancelForRequest`, `busyElsewhere`.
- `TerminalRequests`: shortfall goes to `CpuOrders.create(level, terminal, ...)`; the request remembers the CPU position (was the controller); PRODUCING asks that CPU; cancel asks all CPUs of the line.
- `WarehouseNetwork.tickStockRules`: uses `CpuOrders` on the warehouse's line (controller or Access Points); state `no_cpu` when there is none. `order()` for the command goes through `CpuOrders` too.

### Removals
- Order Board: `storage/board/*`, `OrderBoardMenu`, `OrderBoardScreen`, `ClientOrders`, payload registrations, block, item, block entity, menu type, creative tab entry, datagen blockstate, loot, recipe, lang, textures; spec removed.
- `WarehouseNetwork`: the `OrderBook` field, `orders()`, `producers()`, `machineRows()`, `orderRows()`, `cancelOrder()`, the Inbound credit call. `OutboundStationBlockEntity.producer()` goes; `feedStation` and the keep slot restock stay.
- Old GameTests on the station route (`outboundReportsProducer`, `outboundFacingChestIsNoProducer`, `orderBoard*`) are deleted; order and rule tests move to a CPU setup.

### Datapack
`data/servus_storage/storage/cpu.json` with `order_slots` (2), read by `StorageStatsManager`.

### PLAN.md
Section 5's open decision "which CPU takes an order" is settled (first free slot by lowest position, else fewest open orders) and removed from the list; the M8 item text and 2.4 stay accurate.

### Verification
JUnit: `CpuShapeTest`, `CpuChoiceTest`. GameTests (`CraftingCpuGameTests`): form and unform, request shortfall crafted on a Crafting Station over the line and delivered to the terminal, slots and choice with one and two CPUs, stock rule through the CPU and `no_cpu`, cancelled orders on unform, migrated order tests. Manual: forming in game, textures, summary click.

## Risks / Trade-offs

- [Orders are lost when the CPU unloads or the server stops] -> same as before on the controller; requests then show missing and are retried, rules order again.
- [Collected outputs come from the producer's output, so a player taking them by hand starves the step] -> the step waits; cancel the request. Acceptable for now.
- [Shipments counted as fed before they arrive] -> short endings are subtracted and re-shipped.
- [All 27 blocks have a block entity] -> they are cheap and only the master ticks.
