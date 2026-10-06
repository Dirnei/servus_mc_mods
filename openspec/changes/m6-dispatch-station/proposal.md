# Proposal

## Why

The warehouse can be searched and picked from, and shuttles fetch pulls over the rails, but nothing automated gets items in. Everything entering the warehouse goes through a player at the Counter Terminal or through a hopper into one specific crate, and the insert order (same item, empty crate, anything) scatters stock wherever there is room. This change implements the M6 item "Dispatch station, put-away rules, overflow zones" from `PLAN.md`: an inbound dock where items arrive, zones that give the warehouse a layout, rules that send each item to its zone, and shuttles that carry the put-away over the rails so inbound throughput depends on the fleet like picks do.

## What Changes

- **Zones.** A storage block (crate family, Heavy Duty Rack) can belong to a named zone. Zones are stamped in world with a new Zone Label item that is named in an anvil and right clicked onto blocks. The zone name is drawn on the block's front and shown in the Heavy Duty Rack GUI title.
- **Put-away rules per zone.** Each zone of a warehouse has a filter (ghost items and item tags) and one zone can be the overflow zone. An item that matches a zone's filter goes to that zone; when that zone is full it goes to the overflow zone; when that is full too it waits. Items matching no filter go to general storage (unzoned blocks and zones without a filter), then to the overflow zone. Zoned blocks with a filter take only their own items. Rules are warehouse wide and live on the Warehouse Controller, so every insert (station intake, terminal push, hopper into a station) obeys them.
- **Dispatch Station block.** A new warehouse node with an intake buffer exposed as an item handler, so hoppers, Utility Ducts and players feed it. The warehouse drains the buffer by routing every stack to a target block through the put-away rules. With shuttles and a home rail, a put-away becomes a shuttle job: drive to the station, load, drive to the target, unload, return. Without them the move is instant, like pulls today. Stacks with no permitted room stay in the buffer and the station shows a backed up lamp and the reason in its GUI.
- **Dispatch Station GUI.** Shows the intake buffer and the zones of the warehouse. Selecting a zone edits its filter (nine ghost item slots and a tag line) and its overflow flag. Zones without blocks keep their rule and are listed greyed so a layout can be rebuilt.
- **Rails dock to stations.** The station links like a stock block: rails dock onto it on any side and shuttles stop beside it. The station is never indexed and never pulls neighbouring inventories in, so the chest feeding it is not warehouse stock.
- **Shared job queue.** Pick jobs and put-away jobs share one queue and the free shuttles, taken in order. The controller summary and `/servus_warehouse` report zones and waiting put-aways.

## Capabilities

### New Capabilities
- `storage/zones`: zone membership of storage blocks, the Zone Label item, zone display, zone rules (filters and overflow) and the put-away routing every insert follows.
- `storage/dispatch-station`: the Dispatch Station block, its intake buffer and item handler, draining into targets instantly or by shuttle, the backed up state, its GUI for buffer and zone rules, and crafting.

### Modified Capabilities
- `storage/warehouse-controller`: "Stock operations" inserts route through zone rules instead of the fixed preference order; the controller holds the zone rules and keeps them across reloads and carrot dismantling; `/servus_warehouse zones <pos>` lists zones; the summary reports waiting put-aways.
- `storage/warehouse-rail`: rails connect and dock to a Dispatch Station on any side and carry its membership; the station holds no indexed stock.
- `storage/shuttles`: one job queue for pick and put-away jobs, served in order by the nearest free shuttle; the summary counts both kinds.

## Non-goals

Deferred to later changes:
- Pick rate stats screen (next M6 item). The intake flow records nothing beyond what the index already knows.
- Terminal pushes riding shuttles. A push at the counter stays instant but obeys the zone rules.
- Per station rules or per compartment rules. Rules are per zone and warehouse wide.
- Outbound docks, work orders, machine docks (M7) and wireless delivery (M8).
- A zone naming GUI. Zone Labels are named in a vanilla anvil.
- Zone capacity planning, zone colours or a zone overview map.

## Impact

- `storage` module only. No Lib API changes, no Manufacturing or Logistics changes. Utility Ducts and hoppers integrate through the station's `IItemHandler`.
- New block `dispatch_station` with block entity, menu, screen and payloads; new item `zone_label`; new `WarehouseConnectionRule.NodeKind.STATION` and a station set in `WarehouseNetwork` beside sources and connectors; `StorageNodeBlockEntity` gains a persisted, client synced zone name; `WarehouseControllerBlockEntity` persists the zone rules; `ShuttleFleet` gets put-away jobs and a shared queue; `WarehouseNetwork.insert` routes through a pure `PutAwayPlan`.
- Datapack: `data/servus_storage/storage/warehouse.json` gains `dispatch_buffer_slots` (example 9) and `put_away_per_tick` (example 1); datagen adds blockstates, models, loot, recipes, tags and lang for the station and the label.
- Specs: `storage/warehouse-controller`, `storage/warehouse-rail` and `storage/shuttles` change as listed; `PLAN.md` section 2.3 keeps its text, the M6 item is unchanged.
