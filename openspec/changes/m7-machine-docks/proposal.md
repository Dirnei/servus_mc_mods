# Proposal

## Why

Machines and the warehouse are still two worlds: an Assembler takes what a hopper feeds it and pushes results into a chest beside it, and the warehouse knows nothing about either. Autocrafting needs a place on the rails where shuttles hand ingredients to a machine and collect what it made. This change implements the M7 item "Input and output docks on Manufacturing machines" from `PLAN.md` as one Storage block, the Machine Dock, that sits beside a machine and on a rail.

## What Changes

- **Machine Dock block.** A warehouse node with a horizontal facing that points at the machine it serves. It has two buffers: a feed buffer whose stacks it pushes into the machine's inventory through the machine's item handler, and a collect buffer that the machine fills through the dock's item handler, the same way machines already push outputs into any adjacent inventory. The collect buffer is put away into the warehouse exactly like an Inbound Station's buffer: zone routing, shuttles when they can reach, backed up lamp otherwise.
- **Docks know their producer.** A dock facing a block with the Lib production capability reports that block's recipes and its active recipe to the warehouse. `/servus_warehouse producers <pos>` lists every dock of the warehouse with its machine and recipes.
- **Feeding through the warehouse.** The warehouse can be asked to deliver a number of an item to a dock's feed buffer. It plans the delivery like a terminal request: instantly from crates no shuttle reaches, by shuttle for stock on the rails, never instantly from a rack. `/servus_warehouse feed <dock> <item> <count>` exposes it for ops; work orders will use it next.
- **Dock GUI.** Right click shows the feed row, the collect row and the state line.

## Capabilities

### New Capabilities
- `storage/machine-dock`: the Machine Dock block, its two buffers and item handler, feeding the machine, collecting from it, producer discovery, warehouse feeding, GUI and crafting.

### Modified Capabilities
- `storage/warehouse-controller`: `/servus_warehouse producers <pos>` and `/servus_warehouse feed <dock> <item> <count>`.
- `storage/shuttles`: a pick job's destination may be a dock as well as a terminal.

## Non-goals

Deferred to later changes:
- Work orders that decide what to feed and when (`m7-work-orders`).
- Docks for machines without the production capability beyond feeding and collecting; a dock still moves items for any block with an item handler.
- Pulling outputs out of a machine that does not push them. Servus machines push into adjacent inventories; other mods' machines must do the same or use a hopper into the dock.
- Side configuration on the dock.

## Impact

- `storage` only. New block `machine_dock` with block entity (extending the Inbound Station block entity, so draining, claims and the activity lamp are shared), menu, screen, datagen and textures. `WarehouseNetwork` tracks docks among its stations, resolves producers through `ServusLibCapabilities.PRODUCTION`, gains `producers()` and `feedDock(...)`, and delivers pick jobs to any block with a delivery buffer. `ServusStorageCommands` gains two subcommands.
- Datapack: `warehouse.json` gains `dock_feed_interval` (example 8); the dock buffers use `inbound_buffer_slots`.
- `PLAN.md` needs no change.
