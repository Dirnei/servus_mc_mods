# Tasks

## 1. Pure logic

- [x] 1.1 `storage/src/main/java/io/github/dirnei/mc/servus/storage/field/FertilizeMode.java`, `FertilizePlan.java`, `FertilizeStatus.java` and `FieldRipeness.growing()` per design.md; JUnit tests `FertilizePlanTest` (fetch is the min of plots x doses, stock above the reserve and room; stock equal to the reserve fetches nothing; reserve 0 may take all; targets per mode, none at or above the threshold, none when off; interval passed with no call yet and after the interval, not before; due needs targets, stock above the reserve and the interval; reserve steps clamp at 0 and the max), `FertilizeStatusTest` (order of the reasons, problems) and a `growing` case in the existing `FieldRipenessTest`, under `storage/src/test/java/io/github/dirnei/mc/servus/storage/field/`. Run `gradlew :storage:test`.

## 2. Module, data and look

- [x] 2.1 `storage/.../shuttle/module/ModuleSpec.java` (`SPREADER`), `JobNeeds.java` (`SPREADER`), `storage/.../registry/ServusStorageItems.java` (Spreader, creative tab), `storage/.../crate/StorageStatsManager.java` (four field numbers, test setters, reserve floor 0), `storage/src/main/resources/data/servus_storage/storage/field.json`, item texture `storage/src/main/resources/assets/servus_storage/textures/item/spreader.png`. Run `gradlew build`.
- [x] 2.2 Datagen: `ServusStorageItemTagsProvider` (fertilizer tag), `ServusStorageRecipeProvider` (Spreader), `ServusStorageBlockStateProvider` (item model), `ShuttleModels` (`tool_spreader`), `ServusStorageLanguageProvider` (item, tooltip, tag). Run `gradlew :storage:runData`, then `gradlew build`.

## 3. Head

- [ ] 3.1 `storage/.../field/FieldHeadBlockEntity.java`: fertilizer, mode, threshold, reserve, `unfed` set from `update`, `fertilizeTargets`, `dosed`, `fertilizeDue`, `fertilizeCalled`, `fertilizeStatus`, `fertilizerStock`, `spreaderName`, saving and update tag; `FERTILIZER` tag key. Run `gradlew build`.

## 4. Fleet and warehouse

- [ ] 4.1 `storage/.../shuttle/FieldRun.java` (shared base), `HarvestRun.java` (extends it), `SpreadRun.java`, `FertilizeJob.java`, `ShuttleJob.java` (permits), `ShuttleFleet.java` (needs per kind, fertilize runs with fetch, dosing, shared put-away, recall and loss). Run `gradlew build`.
- [ ] 4.2 `storage/.../network/WarehouseNetwork.java`: `tickFields` checks harvest and fertilize separately, `requestFertilize`, fleet queries per kind; `storage/.../command/ServusStorageCommands.java` prints the fertilize line. Run `gradlew build`.

## 5. GUI

- [ ] 5.1 `storage/.../field/ChooseFertilizerPayload.java`, `FieldFertilizerStockPayload.java`, `FieldWorkerPayload.java` (second name), `storage/.../terminal/ServusStoragePayloads.java`, `storage/.../menu/FieldHeadMenu.java` (FERTILIZER page, data slots, buttons, `chooseFertilizer`, stock sync). Grep that every payload type is registered. Run `gradlew build`.
- [ ] 5.2 `storage/.../client/FertilizerPickerEntries.java`, `ClientFieldWorker.java`, `FieldHeadScreen.java` (Fertilizer tab with picker, modes, reserve, threshold, status, red marker), lang keys in `ServusStorageLanguageProvider`. Run `gradlew :storage:runData`, then `gradlew build`.

## 6. GameTests

- [ ] 6.1 `storage/.../gametest/SpreaderGameTests.java`: `workshopFitsSpreader` (fitted as the tool, field only, no harvest job fits), `spreaderTakesNoWarehouseJob`, `afterPlantingDosesFreshPlots` (two fresh wheat from age 0 get two doses each from a crate of bone meal, an old growing wheat is untouched, the crate holds 4 less), `reserveRespected` (stock equal to the reserve: no run, crate unchanged, status below reserve), `untilRipeShareStopsAtThreshold`, `ripePlotsUntouchedAndLeftoverPutAway` (a ripe plot and a plot one dose from ripe, doses 3: the leftover 2 go back to the crate), `menuChoosesFertilizerAndSettings` (bone meal by id, cobblestone and unknown refused, Off, mode, threshold and reserve buttons, saved and loaded). Adapt `FieldGameTests` to the moved Upgrades page if needed. Run `gradlew :storage:runGameTestServer`; existing field, harvester, rail, corrosion and picker tests stay green.

## 7. Docs and plan

- [ ] 7.1 `docs/src/content/docs/mods/storage.md` (Spreader, Fertilizer tab) and `PLAN.md` (section 2.3 field work and the M10 list with `m10-spreader`). Run `gradlew :manufacturing:runData` for the docs gamedata, `npx vitest run` in `docs/`, and `openspec validate m10-spreader`.

## 8. Verification

- [ ] 8.1 Play check by the user: craft a Spreader, fit it in the workshop and see the hopper box on the shuttle; the Fertilizer tab picks bone meal with stock counts, sets the modes, threshold and reserve and shows the status and red marker; after planting, the Spreader picks bone meal above the reserve, doses the new crops (particles, growth), puts the rest away and parks; Until ripe share stops at the threshold; stock at the reserve keeps it home.
