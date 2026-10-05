# Tasks

## 1. Block base and duct

- [x] 1.1 Add `ConnectableBlock`, refactor `PowerCableBlock` onto it, add `ItemDuctBlock`, `ItemDuctBlockEntity`, `ItemDuctHandler`, `ItemDuctNetwork`, `items_per_tick` in `MachineStats`, registry entries, capability registration, network type registration, stats JSON, texture, generalized multipart generator, loot, tag, lang and recipe. Verify `gradlew build` passes and `gradlew :manufacturing:runData` writes the duct multipart blockstate, three duct models, loot table and recipe.

## 2. Verification

- [x] 2.1 Server check over RCON: iron deposit, extractor, two ducts, smelter, two ducts, chest, fuel injected; after 15 seconds the chest holds iron ingots. Two chests on one duct network beside a smelter alternate. A full target refuses items and the machine keeps its output. Verify with `data get block`.
- [x] 2.2 Full check: `gradlew clean build` green, `gradlew runClientAll` reaches the title screen with no model or texture errors. Commit.
