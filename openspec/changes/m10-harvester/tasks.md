# Tasks

## 1. Pure field run logic

- [x] 1.1 `PlotState`, open plots in `FieldRipeness`, `PlotReach`, `FieldRoute`, `PlotWork`, `FieldCargo`, `SeedPlan`, `HarvestStep` and `FieldDue` in `storage/src/main/java/.../storage/field/` (design D2, D3, D4, D5, D6, D7), no Minecraft imports. JUnit in `storage/src/test/java/.../storage/field/`: `PlotReachTest`, `FieldRouteTest`, `PlotWorkTest`, `FieldCargoTest`, `SeedPlanTest`, `HarvestStepTest`, `FieldDueTest`, open plot cases in `FieldRipenessTest`. Verify: `gradlew test`.

## 2. Harvester module and data

- [x] 2.1 `HARVESTER` in `ServusStorageItems` (creative tab), `JobNeeds.HARVESTER`; `work_ticks`, `harvester_stacks`, `field_check_interval` in `field.json` and `StorageStatsManager` with test setters (design D1, D9). Verify: `gradlew build`.
- [x] 2.2 Datagen: item model, recipe, lang (item, tooltip) in `storage/.../datagen/`; placeholder `item/harvester.png` via the png helper. Verify: `gradlew runData`, then `gradlew build`.

## 3. Open plots in the head

- [x] 3.1 `FieldHeadBlockEntity` classifies plots into `PlotState` with `plotState`, `tillableCount`, `plantableCount`; `FieldEvents` queues the plot above for break, place and hoe events; the field command prints the open counts (design D2). GameTest `openPlotsCounted` in `FieldGameTests` (dirt and farmland counted, hoeing moves a plot from till to plant). Verify: `gradlew build` and `gradlew :storage:runGameTestServer`.

## 4. Field jobs and runs

- [x] 4.1 `FieldJob` in `storage/.../shuttle/`, `ShuttleJob` permits it; `Host.putAwayTarget` and `Host.available` in `ShuttleFleet` and `WarehouseNetwork`; `WarehouseNetwork.tickFields` with `FieldDue`; `ShuttleFleet.requestField`, `hasFieldJob`, `hasFieldShuttles`; field jobs in `assignJobs` are dropped instead of served instantly (design D3, D8). Verify: `gradlew build`.
- [x] 4.2 `HarvesterWork` in `storage/.../field/` (classify, harvest with drops, till, plant) and the field run in `ShuttleFleet` (`HarvestRun`, legs `TO_FIELD`, `WORKING`, `TO_SEEDS`, `TO_STORE`, `WAIT_STORE`, lost handling, `dropCargo`, `shownCargo`) (design D5, D6, D7, D8). Verify: `gradlew build` and the existing shuttle and field GameTests green.

## 5. GameTests

- [x] 5.1 `HarvesterGameTests` in the storage gametest package: `harvesterHarvestsIntoStorage` (ripe wheat into a crate, replanted from the harvest), `harvesterTillsInsideStripOnly`, `harvestOnlyDoesNotPlant`, `plainShuttleTakesNoFieldJob`, `harvesterTakesNoWarehouseJob`, `seedsFromStorage`, `fullCargoComesBack`, `headBrokenMidRun`, `warehouseFullWaits`, `workshopFitsHarvester`. Verify: `gradlew :storage:runGameTestServer` green.

## 6. Plan and docs

- [x] 6.1 `PLAN.md` M10 item 3 notes what was built; `docs/src/content/docs/mods/storage.md` gets the Harvester. Verify: `openspec validate m10-harvester`.

## 6b. Play check follow-ups

- [x] 6b.1 The Field Head shows why a field is idle: pure `FieldWork` reason (`storage/.../field/FieldWork.java`, `FieldWorkTest`), `FieldHeadBlockEntity.work()` from the rails' warehouse, synced by `FieldHeadMenu`, a status line in `FieldHeadScreen`, a Work line in `/servus_warehouse field`. Verify: `gradlew build test`.
- [x] 6b.2 Plots in sight of the rails: pure `FieldReach` flood from the rails through open plots (`FieldReachTest`); the head keeps every candidate's state and rebuilds plots and ripeness when a plot opens or closes; short grass and other replaceable plants count as open and are cleared by the harvester; GameTest `plotsBehindAGapAreOutOfSight`, field test plots moved beside the rails. Verify: `gradlew build test`, `:storage:runGameTestServer`.

## 7. Integration

- [x] 7.1 Full gradle build and tests green: `gradlew build`, `gradlew test`, `gradlew :storage:runGameTestServer`. The game launch happens in the user's play check (7.2).
- [ ] 7.2 Play check in game by the user from main: craft a Harvester, fit it with a Cargo Hold, put it into a garage of a warehouse with a field, set wheat, watch it till, plant, come back for ripe wheat, the wheat lands in storage and seeds are replanted; try harvest only, a full warehouse and breaking the head mid run. Verify: the user confirms in game.
