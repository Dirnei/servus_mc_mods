# Tasks

## 1. Index and controller

- [ ] 1.1 Add `WarehouseIndex` with tests, `ItemKey`, `WarehouseEvent`, the new `WarehouseNetwork`, `StorageStatsManager` (rename) with `warehouse.json`, node reporting in crate and rack, `WarehouseControllerBlock` and block entity, `ServusStorageCommands`, registry entries, textures, blockstate, loot, tag, recipe and lang. Verify `gradlew build` and `gradlew :storage:runData` pass.

## 2. Verification

- [ ] 2.1 Server check over RCON: crate with 100 cobblestone, rack with bread, chest with iron beside the crate and one controller; the info command lists all three; breaking the chest drops the iron from the list; a second controller reports offline. `gradlew :manufacturing:runGameTestServer` passes, `gradlew clean build` green, `gradlew runClientAll` reaches the title screen. Commit.
