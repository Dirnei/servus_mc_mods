---
title: "Servus: Storage"
description: An in-world warehouse with crates, racks, rails, shuttles, a counter terminal, zones and work orders.
---

Servus: Storage (`servus_storage`) lets you build a real warehouse. There are no storage disks: items sit in crates and racks you can see, rails link them into one network, and shuttles drive along the rails to fetch and put away. It depends only on Servus Lib.

Numbers below are the current defaults. They live in datapack JSON under `data/<namespace>/storage/` (`crate.json`, `rack.json`, `warehouse.json`, `crafting_station.json`) and modpacks can change them.

## Storage blocks

- **Crate, Double Crate, Quad Crate**: bulk storage with one, two or four compartments that split a capacity of 4096 items. Each compartment holds one item type and shows it with its count on the front. Right click to insert, double right click to insert all, left click to take a stack.
- **Heavy Duty Rack**: a chest sized bay (27 slots) for mixed items with a GUI. Racks in a row join into sections of up to three bays and show the items they hold the most of on their shelves. The sides climb like a ladder. Racks are only served by shuttles.

## Warehouse blocks

- **Warehouse Controller**: exactly one per network makes it online. Indexes every storage block and every foreign inventory that touches one, polling a few sources per tick.
- **Counter Terminal**: search the stock, request an amount, push items in. Deliveries land in its nine output slots. Also edits stock rules.
- **Warehouse Rail**: a flat floor rail that connects to neighboring rails and docks onto storage blocks and stations. Rails link everything they touch into one network and carry shuttles.
- **Rail Elevator**: stacked shaft segments that link rails on different floors.
- **Shuttle Garage**: holds up to four Shuttles. Its back rail is their home. More garages mean more shuttles.
- **Shuttle**: an item placed in a garage. Shuttles are simulated as data and drawn moving on the client.
- **Inbound Station**: an intake buffer. The warehouse puts its contents away, by shuttle when it has shuttles.
- **Outbound Station**: keeps a configured set of items stocked in its buffer and pushes them into the block it faces, usually a machine.
- **Order Board**: shows every Outbound Station's machine and every work order with its steps and why it is blocked.
- **Crafting Station**: a powerless crafter for up to four vanilla recipes, so a warehouse can autocraft without Manufacturing.
- **Zone Label**: an item with a name and an item or tag filter. Stamp it onto crates and racks; put-away fills matching zoned blocks first and uses unzoned blocks as overflow.

Every storage block that is part of a working warehouse shows a connection lamp.

## Automation

- **Requests** at the Counter Terminal become pick jobs: instant from crates and foreign inventories no shuttle reaches, by shuttle otherwise, never instant from a rack.
- **Work orders**: when a request cannot be filled and an Outbound Station faces a machine that can make the item, the warehouse plans the production, reserves stock on hand, feeds the machine and counts the results collected by Inbound Stations.
- **Stock rules**: a minimum and maximum per item. When stock drops below the minimum, a work order refills it up to the maximum.
- **Commands**: `/servus_warehouse` has subcommands to inspect a network (`info`, `zones`, `producers`, `orders`, `rules`) and to act on it (`take`, `feed`, `order`, `rule`, `unrule`).

## How it connects

- [Servus Lib](/servus_mc_mods/mods/lib/): network graph, side configuration, the Configuration Carrot, and the production and stock capabilities.
- [Manufacturing](/servus_mc_mods/mods/manufacturing/): not required. When installed, an Outbound Station in front of an Assembler lets the warehouse plan work orders from the Assembler's recipes, through the Lib production capability.
- [Logistics](/servus_mc_mods/mods/logistics/): not required. Ducts can move items in and out of any storage block that exposes an item handler.
