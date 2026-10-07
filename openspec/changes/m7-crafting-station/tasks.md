# Tasks

## 1. Block entity, block and capabilities

- [ ] 1.1 Add `storage/src/main/java/io/github/dirnei/mc/servus/storage/crafting/CraftingStationBlockEntity.java` (buffers, pattern, `StationRecipe`, planning with `IngredientGatherer` and `RoundRobin`, crafting tick, output push, `sideHandler()`, `production()`, `saveFromPattern`, `removeRecipe`, NBT) and `CraftingStationBlock.java` (`FACING`, `LIT`, ticker, menu, carrot with `saveToItem`, drops), register block, block entity, item and both capabilities, add `crafting_station.json` and read it in `StorageStatsManager.java`. Verify `gradlew :storage:build` is green.

## 2. GUI

- [ ] 2.1 Add `storage/src/main/java/io/github/dirnei/mc/servus/storage/menu/CraftingStationMenu.java` and `storage/src/main/java/io/github/dirnei/mc/servus/storage/client/CraftingStationScreen.java`, register them, add lang keys and the GUI texture. Run `gradlew :storage:runData` and verify `gradlew :storage:build` is green.

## 3. Datagen, textures and tests

- [ ] 3.1 Add blockstate, loot, tag, recipe and lang datagen plus placeholder block textures. Add GameTests `craftingStationCraftsConfiguredRecipe`, `craftingStationReportsProduction`, `craftingStationRefusesUnusedItems` and `dockServesCraftingStation` to `ServusStorageGameTests.java`. Run `gradlew :storage:runData` and verify `gradlew :storage:runGameTestServer` passes all storage tests.

## 4. Verification

- [ ] 4.1 Run `openspec validate m7-crafting-station --strict`, full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. Commit.
