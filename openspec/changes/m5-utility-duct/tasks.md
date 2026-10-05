# Tasks

## 1. Lib move and duct

- [x] 1.1 Move `ConnectableBlock` to Lib with `ConnectableModels`, repoint Manufacturing. Add `Channel`, `DuctStatsManager`, `UtilityDuctBlock`, `UtilityDuctBlockEntity`, `DuctItemHandler`, `ChannelItem`, `DuctEvent`, `ItemChannelNetwork`, `EnergyChannelNetwork`, payloads and their registration, `ClientHooks`, `UtilityDuctScreen`, registries, events, datagen providers, stats JSON, textures, recipes and lang. Verify `gradlew build`, `gradlew :logistics:runData` and `gradlew :manufacturing:runData` pass with no change to Manufacturing's generated files.

## 2. Verification

- [x] 2.1 Server check over RCON with all mods: chest A (pull in side) to chest B through two item ducts moves 20 cobblestone; a disabled side keeps chest B empty; a coal generator powers an electric extractor through three energy ducts; items pass a duct row where energy has a gap and energy does not. `gradlew :manufacturing:runGameTestServer` and `gradlew :storage:runGameTestServer` pass, `gradlew clean build` green, `gradlew runClientAll` reaches the title screen. Commit.
