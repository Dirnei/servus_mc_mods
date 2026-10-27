---
title: Data line concepts
description: Nodes, members, roles, upkeep, network events and bandwidth of the Servus Foundation data line.
---

The data line is one of the four lines of the Foundation Utility Duct. This page explains its parts as they are in Foundation 0.4.0. Unless a package is named, the types are in `io.github.dirnei.mc.servus.foundation.data`.

## Nodes and the network

A **data node** is a block that carries the data line. Connected nodes form one `DataNetwork`. There are two kinds of node, both described by the interface `DataNode`:

- **Cables**: ducts with the data line. A cable looks at its six sides and makes the blocks there **members** (see below). `dataCable()` is true, and `dataTier()` names its `CableTier`.
- **Carriers**: blocks that pass the line on to touching nodes, like a piece of cable, and are members themselves. Shuttles terminals work this way. A carrier never makes its other neighbours members. Foundation ships `DataCarrier` for this: create one with your block entity, call `join()` in `onLoad`, and `leave()` in `setRemoved` and `onChunkUnloaded`.

Most machines are neither. They are members: a cable touches them.

## Members and roles

A **member** is a block next to a data cable (or a carrier itself) that exposes at least one of the member capabilities. Its roles come from the capabilities it exposes on the face that touches the cable. All capabilities are sided block capabilities in `io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities`:

| Capability field | Id | Interface | Role (`DataRole`) |
|---|---|---|---|
| `PRODUCTION` | `servus_foundation:production` | `ProductionProvider` | `PRODUCER`: it can make items |
| `STOCK_PROVIDER` | `servus_foundation:stock_provider` | `StockProvider` | `STOCK`: it holds items others may take |
| `STOCK_SINK` | `servus_foundation:stock_sink` | `StockSink` | `STOCK`: it accepts items |
| `DATA_LINK` | `servus_foundation:data_link` | `DataLink` (a marker) | `LINK`: on the line without making or holding items |
| `UPKEEP` | `servus_foundation:upkeep` | `UpkeepHolder` | `UPKEEP`: it keeps a consumable stocked |

A member touched by several cable sides is one member with the union of its roles. `DataMember` is a record of a position and its roles.

These capabilities do not make a block a member on their own, but the line uses them:

| Capability field | Interface | Used for |
|---|---|---|
| `SUPPLIER` | `ItemSupplier` | answering upkeep requests (a warehouse) |
| `TRANSFER_GATE` | `TransferGate` | limiting how fast items enter or leave a member |
| `PACKET_INLET` | `ResourceHandler<ItemResource>` | the item door the data line uses, hidden from pipes and hoppers |

`DRIVE` (interface `Drivable` in the `drive` package) is in the same class, but it belongs to the Hand Crank and Electric Motor, not to the data line.

Chests and other plain inventories are never members. Neither is a machine that only offers `Capabilities.Item`.

## The producer contract

`ProductionProvider` has two methods:

- `List<ProductionRecipe> recipes()`: what the block can run right now.
- `Optional<Identifier> active()`: the id of the recipe it is working on, if any.

A `ProductionRecipe` is a record of `Identifier id`, `List<ItemStack> inputs`, `List<ItemStack> outputs` and `int ticks`. Inputs and outputs are **exact stacks with counts**, one stack per item, for one batch. There are no tags or ingredients: a recipe that takes "any log" must be listed once per log. The id must be unique within the provider.

The Crafting CPU of Shuttles plans with these recipes and the stock of the warehouse. It then sends the inputs into your block as data line deliveries, waits for your block to do the work on its own, and takes the outputs out again. See [Join the data line](/servus_mc_mods/developers/join-the-data-line/) for what that means for your item handler.

## Upkeep

Upkeep is a consumable a machine keeps stocked, such as fuel. It is never part of an order. `UpkeepHolder` names:

- `ItemStack upkeepItem()`: the item, empty for none,
- `int upkeepLevel()`: how many to keep,
- `int upkeepCount()`: how many the block holds right now,
- `void upkeepRequested(int amount, long until)`: an optional callback, told that a supplier will send `amount` items until game time `until`.

Every `DataNetwork.UPKEEP_INTERVAL` ticks (40), the network checks its upkeep members. When one holds less than its level and has no open request, the network asks the members that expose `SUPPLIER` (lowest position first) for the difference. A supplier answers with how many it will send. The request ends when the count has risen by that much, or after `DataNetwork.UPKEEP_TIMEOUT` ticks (1200). A holder with an empty item or a level of 0 is skipped.

In Shuttles 0.4.0 the Warehouse Controller and the Packing Station are suppliers. They send the items as a delivery into the face of your block that touches the duct.

## Network events

The data line follows the Foundation network rules:

- Ducts join and leave the network graph when they are placed, removed, loaded or unloaded. Nothing scans blocks every tick.
- Members are read through NeoForge `BlockCapabilityCache`s. When a cache is invalidated, the network queues a `DataEvent.SidesChanged` for that node.
- Events are queued in a mailbox and drained once per server tick. The sides are read again on the next tick, never while a chunk is loading.

`DataEvent` is a sealed interface with that single record. Other mods do not post events. To make the line read your block again, invalidate its capabilities with `level.invalidateCapabilities(pos)`. `DataNetwork.sidesChanged(ServerLevel, BlockPos)` exists for a node (a duct or carrier) at that position.

## Reading a network

`DataNetworks.networkAt(ServerLevel level, BlockPos pos)` returns the network of the node at `pos`, else the first network that lists `pos` as a member. On a `DataNetwork` you can call, among others:

- `dataMembers()` and `dataMembers(DataRole role)`: the members, as `DataMember` records,
- `hasMember(BlockPos)` and `reaches(BlockPos)`: whether a position is a member, or a node or member,
- `cables()`: how many ducts carry the line,
- `loads()` and `busiest()`: the ducts that carried items during the last second.

Server thread only. Do not keep a `DataNetwork` across ticks: networks merge and split as ducts change.

## Bandwidth

Every cable tier lets a number of items per second through each duct. The tiers are datapack data (`data/<namespace>/data/cable_tiers.json`):

| Cable | Items per second | Duplex |
|---|---|---|
| `servus_foundation:data_cable` | 32 | no: both directions share it |
| `servus_foundation:duplex_data_cable` | 32 | yes: 32 each way |
| `servus_foundation:fast_data_cable` | 128 | yes: 128 each way |

A delivery takes the shortest path, and among equally short paths the one with the most bandwidth. Deliveries that cross the same duct share its budget, oldest first. Carriers have no limit. See [Deliveries](/servus_mc_mods/developers/deliveries/).

## Planner

`io.github.dirnei.mc.servus.foundation.production.OrderPlanner` is the pure planner the Crafting CPU uses. It has no Minecraft world access. You do not need it to join the line.
