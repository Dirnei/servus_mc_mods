# Proposal

## Why

PLAN.md M8 item 6, `m8-crafting-cpu`: work orders still live on the Warehouse Controller and run through Outbound Stations that face a machine, which is the M7 station route M8 replaces. The Crafting CPU becomes the owner of work orders on the data line: it plans with the recipes the producers on the line report, ships their inputs as packets through an Access Point and brings their outputs back the same way. More CPUs run more orders at once, which gives on-demand production its scaling lever.

## What Changes

- New Storage block **Crafting CPU**: 27 of them in a full 3x3x3 cube form one CPU when the Configuration Carrot clicks any of them. A formed block touching a data duct is a data member with the link role. Breaking any block unforms the cube and cancels its orders.
- Work orders and the order book move from the controller to the CPU. Terminal shortfalls, stock rules and `/servus_warehouse order` hand their orders to a CPU on the same data line. Without a CPU no order is made: a terminal request keeps its shortfall as missing and a rule shows "no CPU".
- Each formed CPU runs `order_slots` orders at a time (datapack JSON `storage/cpu.json`, example 2); further orders wait for a free slot. A new order goes to the CPU with a free slot and the lowest position; when every CPU is full, to the one with the fewest open orders. This settles the open PLAN.md decision.
- Steps run on the producers that are members of the data line (Assembler, Crafting Station with its own list): inputs are shipped to the producer through the warehouse's Access Point as packets, outputs are taken out of the producer as packets back to an Access Point, put away there and counted for the step. One step at a time per producer, over all CPUs of the line, orders in creation order.
- **Removed**: the Outbound Station order route (it stays a plain keep slot buffer), the Inbound Station step credit, the producer report of Outbound Stations, and the Order Board block with its screen, payloads, recipe and lang. `/servus_warehouse orders`, `order` and `producers` stay for diagnosis and now look at the data line.
- Docs and PLAN.md follow.

## Capabilities

### New Capabilities
- `storage/crafting-cpu`: the CPU multiblock, its order slots, the choice of CPU, the step execution over the data line and the diagnosis commands.

### Modified Capabilities
- `storage/work-orders`: orders live on CPUs, plan over data line producers, and steps run over packets.
- `storage/stock-rules`: rules hand orders to a CPU; new state "no CPU".
- `storage/counter-terminal`: shortfall orders go to a CPU on the terminal's line.
- `storage/outbound-station`: no producer report and no work order deliveries.
- `storage/order-board`: removed.

## Non-goals

- Patterns and Pattern Storage (`m8-pattern-storage`), the Crafting Station as the CPU's hands (`m8-station-hands`).
- A GUI for the CPU or for orders: the Production Terminal (`m8-production-terminal`) comes right after; until then the summary click and the commands show the state.
- Saving orders across a restart or chunk unload: orders live in memory on the CPU like they did on the controller.
- Machine Adapter and upkeep (`m8-machine-adapter`, `m8-native-machines`).

## Impact

- `storage`: new package `cpu`; `order` (OrderBook host, producers, slots, packet feeding and collection); `network/WarehouseNetwork` (order book, producers, board rows and step credit removed, rules via CPU); `terminal/TerminalRequests`; `outbound`; commands; removal of `board` and its client, menu and payload classes; registries, datagen, lang, textures; GameTests.
- `docs/`: Storage page; PLAN.md section 2.4, M8 list and section 5.
