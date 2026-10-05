# Tasks

## 1. Storage plumbing and crate

- [ ] 1.1 Add Storage registries, events, datagen providers, `WarehouseNetwork`, `StorageNodeBlockEntity`, `CrateStatsManager` with `crate.json`, `CrateBlock`, `CrateBlockEntity`, `CrateItemHandler`, `CrateRenderer`, client registration, textures, blockstate, loot, tag, recipe and lang. Verify `gradlew build` and `gradlew :storage:runData` pass.

## 2. Verification

- [ ] 2.1 Server check over RCON: hopper feeds cobblestone into a crate and another hopper draws from it; a crate merged to 4090 refuses the eighth item of a hopper stack; a crate with 32 dirt and a hopper offering stone keeps the stone out. `gradlew clean build` green, `gradlew runClientAll` reaches the title screen with the Storage renderer registered and no errors. Commit.
