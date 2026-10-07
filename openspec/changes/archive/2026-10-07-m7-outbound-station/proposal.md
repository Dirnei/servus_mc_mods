# Proposal

## Why

The Machine Dock mixes two jobs into one block: it feeds the block it faces and collects from it. In play that was confusing next to the Inbound Station, which also collects. The agreed model (2026-10-07) is one block per direction: the **Inbound Station** takes items into the warehouse, the **Outbound Station** hands items out of it. An Outbound Station keeps a configured set of items at configured amounts in its buffer, like the Refined Storage Interface, and pushes them into the block in front of it. Machines are then served by an Outbound Station in front and an Inbound Station beside them, and the Machine Dock becomes obsolete.

## What Changes

- **Outbound Station block.** Buffer of nine slots and nine keep slots, each a ghost item with a target amount. The warehouse keeps the buffer stocked at those amounts through the normal delivery path (instant from reachable crates, by shuttle from racks). The buffer is pushed into neighbours on push sides and may be extracted on extract sides.
- **Side configuration on both stations.** Every side of an Outbound Station is push, extract or off; every side of an Inbound Station is accept or off. The Configuration Carrot on a face cycles that face's mode; the GUIs show six side buttons. The carrot no longer rotates stations; their facing is set at placement.
- **Machines through stations.** An Outbound Station facing a block with the production capability reports its recipes. Work order steps run on outbound stations: the step's inputs are delivered into the station's buffer on top of the keep amounts, the station pushes them into the machine, and the outputs are counted when an Inbound Station collects them.
- **Machine Dock removed.** Block, GUI, commands and spec go. `producers` lists outbound stations, `feed` delivers into an outbound station's buffer.
- **PLAN.md** section 2.4 and the M7 item describe the two stations instead of docks.

## Capabilities

### New Capabilities
- `storage/outbound-station`: block, keep slots and restocking, side modes, push and extract, producer reporting, GUI, crafting.

### Modified Capabilities
- `storage/inbound-station`: side modes, carrot cycles sides instead of turning.
- `storage/work-orders`: steps run on outbound stations, outputs counted at inbound stations.
- `storage/order-board`: machines section lists outbound stations.
- `storage/stock-rules`, `storage/counter-terminal`, `storage/warehouse-controller`, `storage/crafting-station`: wording and commands follow the new blocks.

### Removed Capabilities
- `storage/machine-dock`: replaced by the Outbound Station plus Inbound Station.

## Non-goals

- Returning unconfigured items from an Outbound Station to storage. Inbound is the way in.
- Per side filters. Side modes only.
- Migrating placed Machine Docks. Nothing is released; placed docks vanish with the block.

## Impact

- `storage`: new package `io.github.dirnei.mc.servus.storage.outbound`; `dock` package deleted; `SideModes` helper shared by both stations; `WarehouseNetwork` producers, feeding, order execution and rows keyed by outbound station; `OrderBook.collected` credited from inbound stations; commands; GUIs; textures; datagen; GameTests rewritten for the station pair.
- Datapack: `warehouse.json` renames `dock_feed_interval` to `push_interval` and gains `outbound_check_interval` (example 40).
