# Tasks

## 1. Ducts

- [ ] 1.1 Add the typed constructor to `ItemDuctBlockEntity`, `SplitterDuctBlock`, `SplitterDuctBlockEntity`, `FilterDuctBlock`, `FilterDuctBlockEntity`, `FilterDuctMenu`, `FilterDuctScreen`, network changes (endpoint owner, filter check, splitter pulls), registry entries, capabilities, screen registration, stats JSON, textures and GUI, blockstates, loot, tags, lang, gated assembling recipes and tech update. Verify `gradlew build` and `gradlew :manufacturing:runData` pass.

## 2. Verification

- [ ] 2.1 Server check over RCON: a chest with raw iron beside a splitter duct feeds a smelter on the network; a filter duct listing iron ingots beside chest A and a plain duct beside chest B sort mixed ingots. `gradlew :manufacturing:runGameTestServer` passes, `gradlew clean build` green, `gradlew runClientAll` reaches the title screen. Commit.
