# Design

## Context

- **Field jobs.** `WarehouseNetwork.tickFields` queues `FieldJob` (Harvester) and `FertilizeJob` (Spreader) per serving head at every field check. `ShuttleFleet` hands them to shuttles whose `JobNeeds` fit (`HARVESTER`, `SPREADER`, both `field`, so wide shuttles that never ride a lift).
- **Runs.** `FieldRuns` drives the runs:
  - `HarvestRun` fetches missing seeds from racks (`fetchSeeds`, `SeedPlan.toFetch`) before the field.
  - `SpreadRun` fetches fertilizer from racks (`fetchFertilizer`, `FertilizePlan.toFetch`), skipping racks it reaches only by lift.
  - Both put leftovers away through the shared put-away (`unloadToStore`, `continueStore`).
- **Head.** `FieldHeadBlockEntity` holds the anti-corrosion `ItemSlots(1)`, exposed through `ServusStorageEvents` as `PACKET_INLET` (data line packets) and `UPKEEP` (`AntiCorrosionUpkeep`), and as a GUI slot in `FieldHeadMenu`.
- **Statuses.** `FertilizeStatus` and the field work status (`FieldWork` / the Field tab line) report what keeps a field idle.

## Goals / Non-Goals

**Goals:**
- Everything a field run consumes waits at the head, so the lift rule for wide shuttles no longer hides stock.
- One way in for all three parts: supply runs by shuttles without a tool.
- A field never waits silently: every reason to wait has a status.

**Non-Goals:**
- Stock limits on harvesting and fertilizing (`m10-field-stock-levels`).
- Changes to how other blocks use the data line or upkeep.

## Decisions

**Buffer model.** A small `SupplyBuffer` in the head holds three parts, each an item and a count up to its level, stored as item id plus int (not vanilla slots), so a level above 64 from data works. It is saved with the head and drops as stacks when broken. The existing anti-corrosion `ItemSlots` becomes the third part; aging takes from it as before. Alternative: three vanilla slots. Rejected because levels are data and could exceed a stack.

**Pure logic.** `SupplyPlan` (no Minecraft types, JUnit):
- `low(held, level)`: below half the level.
- `toBring(held, level, stock, reserve, room)`: what a part is short of, never stock at or below the reserve, never more than the cargo room.
- `due(...)`: the trigger over the three parts.

`FertilizePlan.toFetch` keeps its shape; the Spreader now passes the buffer count where it passed the warehouse stock, and a reserve of 0.

**Supply job and run.** `SupplyJob(head)` is queued in `tickFields` with `JobNeeds.WAREHOUSE`, so only shuttles without a field tool take it, and they ride lifts like picks. `SupplyRun` (a `FieldRun`):
1. Plans per part with `SupplyPlan`.
2. Fetches from `host.sourcesHolding(item)` like a pick, lifts allowed.
3. Drives to the head's rail: the first farm rail beside the head in the order north, east, south, west, the same one that decides the field.
4. Unloads into the buffer, loads stale items (not the current crop or fertilizer), puts them away and goes home.

One supply run per head at a time, tracked like fertilize jobs (`hasSupplyJob`). Supply checks run even while no Harvester or Spreader exists, so the anti-corrosion part stays stocked.

**Field shuttles use the buffer.**
- `spreadTurn` replaces `fetchFertilizer` with a stop at the head's rail that takes `toFetch` from the buffer.
- `HarvestRun` replaces `fetchSeeds` with the same stop for the shortfall.
- Put-aways first fill the matching buffer part when the cargo holds the crop item (Harvester) or the fertilizer (Spreader) and the run passes the head's rail. Only then do they go to racks.
- `FertilizeJob` is queued only while the buffer holds fertilizer. The warehouse stock check moves to the supply job.

**Head leaves the data line.** The `PACKET_INLET` and `UPKEEP` registrations for the head, `packetInlet()`, `upkeep()` and `AntiCorrosionUpkeep` go. `FieldHeadMenu` drops the anti-corrosion slot and its shift click rule. The Range Upgrade slot stays.

**Statuses.**
- `FertilizeStatus` gains `NO_SUPPLY_SHUTTLE` (a problem, red marker) and `SUPPLY_COMING` (buffer empty, a supply run queued or under way; not a problem).
- The field work status gains "farmland but no seeds in the buffer and none in stock", "waiting for a supply run with seeds" and "no shuttle that can bring seeds" (a warning).
- The field command prints the buffer.

**Data.** `seed_buffer` (64) and `fertilizer_buffer` (64) join `storage/field.json` and `StorageStatsManager`; `anti_corrosion_level` stays.

**GUI.** Counts travel in the menu's container data: the Field tab shows "Seeds 40 / 64", the Fertilizer tab "In buffer 12 / 64", and the Water tab "Honeycomb 5 / 8", each with its item icon, drawn, not as slots.

**NeoForge APIs.** Only `RegisterCapabilitiesEvent` registrations are removed; nothing new.

**PLAN.md.** Settles no open decision. Not a PLAN.md item, so no grooming beyond the archive rule.

## Risks / Trade-offs

- [More shuttle traffic: supply runs on top of field runs] → A part refills only below half its level, and fills to the level in one trip.
- [Two shuttles at the head rail at once (supply and Spreader)] → Runs already share rails; a stop at the head is a normal stop.
- [Players lose hand filling of anti-corrosion] → Wanted: one way in. A warehouse with stock and a shuttle without a tool keeps it filled.
- [Worlds with honeycomb in the old slot] → The slot's items become the anti-corrosion part, so nothing is lost.
