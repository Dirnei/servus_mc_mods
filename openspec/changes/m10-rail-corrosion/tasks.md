## 1. Pure rules

- [x] 1.1 Add `storage/.../field/FieldCorrosion.java` (stages, next, previous, join, age, progress, axeCanScrape; no Minecraft imports) and JUnit `storage/src/test/.../field/FieldCorrosionTest.java` (stepsAndBounds, joinPrefersHead, joinTakesYoungest, joinOfLooseRailsStaysLoose, agingCountsWater, agingUsesUpkeepFirst, agingStopsAtCorroded, agingOffWithoutStep, progressPercent, axeDurabilityCheck); `gradlew :storage:test`

## 2. Model swap and textures

- [x] 2.1 Foundation `wood/Wood.java` (`SWAPS` model property) and `wood/client/WoodModel.java` (swap quads by sprite name, cache key with swaps); `gradlew build`
- [x] 2.2 Generate `farm_rail_head_exposed`, `_weathered`, `_oxidized`, `_corroded` and `field_head_top_corroded` with a PIL generator in the job tmp dir from vanilla copper textures; `gradlew build`

## 3. Stage on rails and the head

- [x] 3.1 `rail/RailBlockEntity.java` (corrosion stage saved and synced, model data swap of `farm_rail_head`); `gradlew build`
- [x] 3.2 `crate/StorageStatsManager.java` and `storage/field.json` (`corrosion_water`, `anti_corrosion_level`); `field/FieldHeadBlockEntity.java` (stage, join on serve, aging from used water, corroded stops hydration, anti-corrosion slot, upkeep holder, item view); `field/FieldHeadBlock.java` (`CORRODED`, slot drop); `field/FieldHeads.java` and `field/FarmRailBlock.java` (headless joins); `ServusStorageEvents.java` (item handler and upkeep capability); `gradlew build`
- [x] 3.3 `field/FarmRailBlock.java` axe scrape over the field with the durability check, sound and particles; `gradlew build`

## 4. GUI, command, datagen

- [x] 4.1 `menu/FieldHeadMenu.java` (anti-corrosion slot, stage and progress data, shift click) and `client/FieldHeadScreen.java` (taller panel, corrosion row); `gradlew build`
- [x] 4.2 `command/ServusStorageCommands.java` (stage in `field <pos>`, `field <pos> age <mB>`); `gradlew build`
- [ ] 4.3 Datagen: new `datagen/ServusStorageItemTagsProvider.java` (`servus_storage:anti_corrosion` with honeycomb), `ServusStorageBlockStateProvider` (corroded top models), language; `gradlew :storage:runData`, `gradlew build`

## 5. Tests and docs

- [ ] 5.1 GameTests `storage/.../gametest/RailCorrosionGameTests.java`: agingWithoutUpkeep, honeycombHoldsStage, corrodedStopsHydration, axeScrapesField, wornAxeRefused, placedRailAdoptsStage, droppedRailIsFresh, headlessJoinTakesYoungest, hopperFillsUpkeep, waterUseCountsForAging; keep field, harvester and rail tests green; `gradlew :storage:runGameTestServer`
- [ ] 5.2 Docs `docs/src/content/docs/mods/storage.md` (Farm Rail, Field Head, command) and `PLAN.md` M10 lists the change; `gradlew build`

## 6. Verification

- [ ] 6.1 Full `gradlew build`, unit tests and storage GameTests green
- [ ] 6.2 Play check by the user in game: the four stage looks on straight, curve, crossing and dock pieces in two woods, honeycomb by hand, hopper and data line, a corroded field drying with the rust rim and GUI line, axe scrape with sound and particles and the worn axe message, placed and dropped rails taking or losing the stage
