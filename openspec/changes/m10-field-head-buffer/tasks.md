# Tasks

## 1. Pure logic and data

- [x] 1.1 Add pure `SupplyPlan` (`low`, `toBring`, `due`) in `storage/.../field/` with JUnit `SupplyPlanTest`: below and at half the level, a fill to the level, reserve kept, cargo room cap, nothing in stock, a level of 0 turns a part off. Add `seed_buffer` and `fertilizer_buffer` to `storage/field.json` and `StorageStatsManager`. Verify: `gradlew :storage:test` green.

## 2. The head's buffer

- [x] 2.1 Add `SupplyBuffer` to `FieldHeadBlockEntity` (seeds, fertilizer, anti-corrosion parts; save, load, drop when broken), move aging onto its anti-corrosion part, and remove `packetInlet()`, `upkeep()`, `AntiCorrosionUpkeep` and the head's `PACKET_INLET` and `UPKEEP` registrations in `ServusStorageEvents`. Verify: `gradlew build` green.
- [x] 2.2 `FieldHeadMenu` and `FieldHeadScreen`: drop the anti-corrosion slot and its shift click, sync the three counts, and draw them on the Field, Fertilizer and Water tabs; lang keys in `ServusStorageLanguageProvider`. Verify: `gradlew :storage:runData` and `gradlew build` green.

## 3. Supply runs

- [x] 3.1 Add `SupplyJob` and `SupplyRun`, queue them in `WarehouseNetwork.tickFields` with `JobNeeds.WAREHOUSE`, and add `hasSupplyJob`. Plan with `SupplyPlan`, fetch with lifts allowed, unload at the head's rail, take stale items back, and handle runs that cannot go on (in `FieldRuns` and `ShuttleFleet`). Verify: `gradlew build` green.
- [x] 3.2 Field shuttles use the buffer. The Spreader takes fertilizer at the head's rail instead of racks, and `FertilizeJob` needs a non empty buffer. The Harvester takes its seed shortfall from the buffer. Leftovers fill the buffer before racks. Verify: `gradlew build` green.
- [x] 3.3 Statuses: `FertilizeStatus.NO_SUPPLY_SHUTTLE` and `SUPPLY_COMING`, the new field work statuses, red markers and the head's ripe share bar problem state; the field command prints the buffer. Verify: `gradlew :storage:runData` and `gradlew build` green.

## 4. GameTests and docs

- [x] 4.1 GameTests in `storage/.../gametest/`:
  - `supplyRunFillsTheBuffer` (seeds, bone meal, honeycomb)
  - `supplyRidesTheLift` (stock behind a shaft, then the Spreader doses)
  - `supplyKeepsTheReserve`
  - `noToollessShuttleShowsStatus`
  - `staleSeedsGoBack`
  - `bufferDropsWhenBroken`
  - `headIsNotOnTheDataLine`

  Adjust the Spreader, Harvester and corrosion tests that filled racks or the anti-corrosion slot. Verify: `gradlew :storage:runGameTestServer` green.
- [x] 4.2 Describe the buffer and supply runs in `docs/src/content/docs/mods/storage.md` (Field Head, Spreader, Harvester, anti-corrosion). Verify: docs tests green.

## 5. Integration

- [ ] 5.1 Full `gradlew build`, unit tests and storage GameTests green. The game launches with all three mods.
- [ ] 5.2 Play check in game by the user from main:
  - The bone meal behind the elevator reaches the buffer on a supply run, and the field gets fertilized.
  - The buffer counts show on the three tabs, and honeycomb can no longer be put in by hand.
  - The statuses appear without a tool-less shuttle or without stock.

  Verify: the user confirms in game.
