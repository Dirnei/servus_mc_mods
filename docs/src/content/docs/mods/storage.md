---
title: "Servus: Storage"
description: An in-world warehouse with crates, racks, rails, shuttles, a counter terminal, zones and work orders.
---

Servus: Storage (`servus_storage`) lets you build a real warehouse. There are no storage disks: items sit in crates and racks you can see, rails link them into one network, and shuttles drive along the rails to fetch and put away. It depends only on Servus: Foundation.

Numbers below are the current defaults. They live in datapack JSON under `data/<namespace>/storage/` (`crate.json`, `rack.json`, `warehouse.json`, `crafting_station.json`) and modpacks can change them.

## Storage blocks

- **Crate, Double Crate, Quad Crate**: bulk storage with one, two or four compartments that split a capacity of 4096 items. Each compartment holds one item type and shows it with its count on the front. Right click to insert, double right click to insert all, left click to take a stack.
- **Heavy Duty Rack**: a chest sized bay (27 slots) for mixed items with a GUI. Racks in a row join into sections of up to three bays and show the items they hold the most of on their shelves. The sides climb like a ladder. Racks are only served by shuttles.

## Warehouse blocks

- **Warehouse Controller**: exactly one per network makes it online. Indexes every storage block and every foreign inventory that touches one, polling a few sources per tick.
- **Counter Terminal**: sits on a data line, not on the rails. Search the stock of every warehouse it reaches, request an amount, push items in. Deliveries arrive as packets in its nine output slots. Also edits stock rules.
- **Warehouse Rail**: a flat floor rail that connects to neighboring rails and docks onto storage blocks and stations. Rails link everything they touch into one network and carry shuttles.
- **Rail Elevator**: stacked shaft segments that link rails on different floors.
- **Shuttle Garage**: holds up to four Shuttles. Its back rail is their home. More garages mean more shuttles.
- **Shuttle**: an item placed in a garage. Shuttles are simulated as data and drawn moving on the client.
- **Inbound Station**: an intake buffer. The warehouse puts its contents away, by shuttle when it has shuttles.
- **Access Point**: the warehouse's door to the data line. It sits beside a rail like a station and touches a duct with the data line. Packets that arrive are put away like an Inbound Station delivery; shipments to a block on the data line are picked into it (instantly from crates, by shuttle from racks) and leave as packets, one stream at a time. With several Access Points, a shipment uses the one with the shortest shuttle trip from the racks holding the item, so more Access Points mean more throughput.
- **Outbound Station**: keeps a configured set of items stocked in its buffer and pushes them into the block behind it, for pipe lines that run all the time. It takes no part in work orders.
- **Crafting CPU**: 27 CPU blocks in a 3x3x3 cube, formed with the Configuration Carrot. A formed CPU on the data line owns the work orders of that line and runs two at a time (`order_slots` in `storage/cpu.json`); more CPUs run more orders at once. Right click it with an empty hand to see its slots. Breaking a block unforms the cube and cancels its orders.
- **Production Terminal**: the production overview on the data line. Its Orders tab lists every Crafting CPU of the line with its slots in use and waiting orders, and under each CPU its orders with their steps, states and reasons; click an open order to cancel it. Its Machines tab lists every producer of the line with its state and the recipes it reports; a Crafting Station shows idle or what it is crafting with the crafts left, and that it crafts any pattern. Its Patterns tab lists every pattern of the line with a capacity bar; the plus button opens the recipe editor (with your inventory below it and JEI transfer) and encodes a Blank Pattern from your inventory into the first Pattern Storage with room, and clicking a pattern opens it to change or delete it (deleting gives the Blank Pattern back). The lamp is lit while a CPU or a warehouse is on the line. Made from glass panes, copper ingots, a comparator, iron ingots and redstone.
- **Pattern Storage**: holds nine encoded Patterns (`patterns_per_block` in `storage/pattern_storage.json`) in a slot GUI. It sits on the data line; more blocks hold more patterns. It drops its patterns when broken.
- **Blank Pattern** and **Pattern**: a Blank Pattern (paper and redstone) is encoded in the Production Terminal into a Pattern that holds one vanilla crafting recipe with concrete items; its tooltip shows the result and the inputs. A Pattern alone in a crafting grid wipes back to a Blank Pattern.
- **Crafting Station**: a powerless crafter and the hands of the Crafting CPU. It has nothing to configure: a CPU hands it a pattern and a number of crafts, packets bring the inputs, and it crafts until the job is done. Its GUI shows the job's result and how many crafts are left, or idle. Without a job it crafts nothing and hoppers and ducts cannot put items in. More stations craft more orders at the same time.
- **Zone Label**: an item with a name and an item or tag filter. Stamp it onto crates and racks; put-away fills matching zoned blocks first and uses unzoned blocks as overflow.

Every storage block that is part of a working warehouse shows a connection lamp.

## Automation

- **Requests** at the Counter Terminal become pick jobs to the Access Point with the shortest shuttle trip: instant from crates and foreign inventories no shuttle reaches, by shuttle otherwise, never instant from a rack. The Access Point sends them on as packets. A terminal reaches a warehouse when the warehouse's controller or one of its Access Points shares its data line; with several warehouses the stock adds up and the fullest one ships first.
- **Work orders**: when a request cannot be filled and a producer on the data line (a Crafting Station, an Assembler) can make the item, a Crafting CPU of the line plans the production, reserves stock on hand, ships the inputs to the producer as packets through an Access Point and brings the outputs back the same way, where they are put away. Each producer runs one step at a time. A new order goes to the first CPU with a free slot. Without a CPU the shortfall stays missing. A CPU also plans with the patterns of its data line: any Crafting Station on the line can run any pattern, and a new order prefers a station that has no work planned yet.
- **Stock rules**: a minimum and maximum per item. When stock drops below the minimum, a CPU on the warehouse's data line gets a work order that refills it up to the maximum. Without a CPU the rule reads "no CPU".
- **Commands**: `/servus_warehouse` has subcommands to inspect a network (`info`, `zones`, `rules`; `producers` and `orders` look at the data line) and to act on it (`take`, `feed`, `order`, `rule`, `unrule`, `ship <controller> <target> <item> <count>` to send items through an Access Point to a block on the data line).

## How it connects

- [Servus: Foundation](/servus_mc_mods/mods/foundation/): network graph, side configuration, the Configuration Carrot, the production and stock capabilities, and ducts that move items in and out of any storage block that exposes an item handler.
- [Manufacturing](/servus_mc_mods/mods/manufacturing/): not required. When installed, an Assembler on the data line is a producer, so Crafting CPUs plan work orders with its recipes, through the Foundation production capability.
