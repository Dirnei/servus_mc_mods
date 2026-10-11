# Tasks

## 1. Pure logic

- [x] 1.1 `storage/src/main/java/io/github/dirnei/mc/servus/storage/site/SiteChunks.java` (chunk keys, span from block positions, merge with unloaded chunks), `SiteCapacity.java` (capacity, `NO_UPGRADE`, `COVERED`, `OVER`), `SiteName.java` (display and clean), `ChunkMapLayout.java` (cells in a box, clamped cell size, window around the home chunk) per design.md; JUnit tests `SiteChunksTest` (key round trip with negative coordinates, positions in one chunk count once, merge keeps unloaded, drops loaded and empty, adds new), `SiteCapacityTest` (no upgrade, equal is covered, one over), `SiteNameTest` (blank gives the position name, trim, 32 character cut, control characters dropped), `ChunkMapLayoutTest` (single chunk centred, two chunks side by side, cell size clamp, wide span windowed around home) under `storage/src/test/java/io/github/dirnei/mc/servus/storage/site/`. Run `gradlew :storage:test`.

## 2. Item, data and block state

- [x] 2.1 `storage/.../registry/ServusStorageItems.java` (Chunk Loader Upgrade, creative tab), `storage/.../crate/StorageStatsManager.java` (`chunk_loader_upgrade_slots`, `chunks_per_upgrade`), `storage/src/main/resources/data/servus_storage/storage/warehouse.json`, `storage/.../controller/ChunkLoaderUpgradeSlots.java`, item texture `textures/item/chunk_loader_upgrade.png`, controller front textures `warehouse_controller_front_idle/working/blocked.png`. Run `gradlew build`.
- [x] 2.2 Datagen: `ServusStorageBlockStateProvider` (controller through the activity lamp models, upgrade item model), `ServusStorageRecipeProvider` (upgrade recipe), `ServusStorageLanguageProvider` (item, tooltip, GUI strings). Run `gradlew :storage:runData`, then `gradlew build`.

## 3. Controller site

- [x] 3.1 `storage/.../controller/WarehouseControllerBlock.java` (`ACTIVITY` property, open the GUI, `setPlacedBy` resets span and sets the team, upgrades drop on removal and are taken out before a carrot dismantle), `WarehouseControllerBlockEntity.java` (name, team, span, upgrades, state, lamp, save and load, `MenuProvider`). Run `gradlew build`.
- [x] 3.2 `storage/.../network/WarehouseNetwork.java`: `spanDirty` from node add, remove, absorb and source changes, indexed foreign sources, span recompute once per tick with exactly one controller; `storage/.../command/ServusStorageCommands.java` info prints the site. Run `gradlew build`.

## 4. GUI

- [x] 4.1 `storage/.../controller/SiteStatusPayload.java`, `RenameSitePayload.java`, `storage/.../menu/WarehouseControllerMenu.java`, `storage/.../registry/ServusStorageMenus.java`, `storage/.../terminal/ServusStoragePayloads.java`; grep that every payload type is registered. Run `gradlew build`.
- [x] 4.2 `storage/.../client/WarehouseControllerScreen.java` (name field, status lines, summary tooltip, chunk map, upgrade row), `ClientSiteStatus.java`, `ServusStorageClient.java` (screen registration), lang keys. Run `gradlew :storage:runData`, then `gradlew build`.

## 5. GameTests

- [ ] 5.1 `storage/.../gametest/ControllerSiteGameTests.java`: `spanFollowsRailIntoNextChunk` (crate over a chunk border by rail: 2 chunks; rail broken: 1), `foreignChestCountsItsChunk` (chest across the border touching a crate: 2; chest broken: 1), `overCapacityLampAndBackWithUpgrade` (one upgrade, chunks per upgrade set low through the test setter: red lamp, second upgrade: green), `upgradeSlotsTakeOnlyChunkLoaderUpgrades`, `nameSavedAndCarriedByCarrot` (name and rules survive a carrot dismantle and placement, upgrades drop, span restarts at the new chunk), `menuRenamesAndCleans`. Run `gradlew :storage:runGameTestServer`; all existing storage GameTests stay green.

## 6. Docs and plan

- [ ] 6.1 `docs/src/content/docs/mods/storage.md` (controller GUI, sites, Chunk Loader Upgrade, chunk span and error state) and `PLAN.md` (M11 item 1 with a Built note, and that the upgrade item and slots came in this change). Run `openspec validate m11-controller-site`.

## 7. Verification

- [ ] 7.1 Play check by the user: right click a controller, name it, see the chunk map and count grow when a rail carries a crate into the next chunk; craft a Chunk Loader Upgrade and put it in; with a site larger than the upgrades cover the lamp and the GUI turn red, another upgrade makes it green; dismantle with the carrot and the name comes back on placement while the upgrades drop.
