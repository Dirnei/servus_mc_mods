# Proposal

## Why

The warehouse can be searched and picked from, and shuttles fetch pulls over the rails, but nothing automated gets items in. Everything entering the warehouse goes through a player at the Counter Terminal or through a hopper into one specific crate, and the insert order (same item, empty crate, anything) scatters stock wherever there is room. This change implements the M6 item "Dispatch station, Zone Labels with put-away filters, unzoned storage as overflow" from `PLAN.md`: an inbound dock where items arrive, zones that give the warehouse a layout, and shuttles that carry the put-away over the rails so inbound throughput depends on the fleet like picks do.

## What Changes

- **Zone Labels.** A new item. Right clicking the air with it opens a GUI where the player names the zone and sets its filter: nine ghost items and a line of item tags. Right clicking a crate or Heavy Duty Rack with the label copies name and filter onto that block; sneak right click clears it. The zone name is drawn on the block's front and shown in the rack GUI title. Blocks keep the copy they were stamped with.
- **Zones are optional and prioritised.** A zoned block with a filter takes only matching items. Unzoned blocks, and zoned blocks with an empty filter, take anything. Put-away fills zoned blocks listing the exact item first, then zoned blocks matching by tag, then general storage. There is no separate overflow zone: unzoned storage is the overflow. Every insert obeys this, including terminal pushes and hoppers.
- **Dispatch Station block.** A new warehouse node with an intake buffer exposed as an item handler, so hoppers, Utility Ducts and players feed it. The warehouse drains the buffer by routing every stack to a target block. With shuttles and a home rail, a put-away becomes a shuttle job: drive to the station, load, drive to the target, unload, return. Without them the move is instant, like pulls today. Stacks with no room stay in the buffer and the station shows a backed up lamp and the reason in its GUI.
- **Dispatch Station GUI.** Shows the intake buffer and the station's state. Zones are seen on the blocks and listed by `/servus_warehouse zones`.
- **Rails dock to stations.** The station links like a stock block: rails dock onto it on any side and shuttles stop beside it. The station is never indexed and never pulls neighbouring inventories in, so the chest feeding it is not warehouse stock.
- **Shared job queue.** Pick jobs and put-away jobs share one queue and the free shuttles, taken in order. The controller summary and `/servus_warehouse` report zones and waiting put-aways.

## Capabilities

### New Capabilities
- `storage/zones`: the Zone Label item and its GUI, stamping zones onto storage blocks, zone display, zone filters on blocks and the put-away routing every insert follows.
- `storage/dispatch-station`: the Dispatch Station block, its intake buffer and item handler, draining into targets instantly or by shuttle, the backed up state, its GUI for the buffer and state, and crafting.

### Modified Capabilities
- `storage/warehouse-controller`: "Stock operations" inserts route through zones instead of the fixed preference order; `/servus_warehouse zones <pos>` lists zones; the summary reports waiting put-aways.
- `storage/warehouse-rail`: rails connect and dock to a Dispatch Station on any side and carry its membership; the station holds no indexed stock.
- `storage/shuttles`: one job queue for pick and put-away jobs, served in order by the nearest free shuttle; the summary counts both kinds.

## Non-goals

Deferred to later changes:
- Pick rate stats screen (next M6 item). The intake flow records nothing beyond what the index already knows.
- Terminal pushes riding shuttles. A push at the counter stays instant but obeys the zones.
- A dedicated overflow zone flag. Unzoned storage is the overflow; a flagged overflow zone can come back if layouts need it.
- Editing a label after stamping does not update stamped blocks. Re-stamp to update.
- Per compartment filters on crates. A zone applies to the whole block.
- Outbound docks, work orders, machine docks (M7) and wireless delivery (M8).

## Impact

- `storage` module only. No Lib API changes, no Manufacturing or Logistics changes. Utility Ducts and hoppers integrate through the station's `IItemHandler`.
- New block `dispatch_station` with block entity, menu, screen and payload; new item `zone_label` with a data component, menu, screen and payloads; new `WarehouseConnectionRule.NodeKind.STATION` and a station set in `WarehouseNetwork` beside sources and connectors; `StorageNodeBlockEntity` gains a persisted, client synced zone (name plus filter); `ShuttleFleet` gets put-away jobs and a shared queue; `WarehouseNetwork.insert` routes through a pure `PutAwayPlan`.
- Datapack: `data/servus_storage/storage/warehouse.json` gains `dispatch_buffer_slots` (example 9) and `put_away_per_tick` (example 1); datagen adds blockstates, models, loot, recipes, tags and lang for the station and the label.
- Specs: `storage/warehouse-controller`, `storage/warehouse-rail` and `storage/shuttles` change as listed. `PLAN.md` section 2.3 and the M6 item are reworded: zones are stamped with labels and unzoned storage is the overflow.
