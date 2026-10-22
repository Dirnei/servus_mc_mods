# Tasks

## 1. Calciner recipes

- [x] 1.1 In `alchemy/.../datagen/ServusAlchemyRecipeProvider.java` craft the Firebox from Polished Deepslate, the Ash Pan from Deepslate Tiles (unlock `has_deepslate_tiles`) and the Calciner from Stone Bricks, patterns unchanged; run `gradlew :alchemy:runData` and check the three recipe JSON files changed
- [x] 1.2 Add GameTests in `alchemy/.../gametest/CalcinerGameTests.java`: `calcinerPartRecipes` (each grid makes one Firebox, Ash Pan, Calciner) and `brickGridsCraftNothing` (the old brick grids make nothing); run `gradlew :alchemy:runGameTestServer`

## 2. Pipe recipes

- [x] 2.1 In `foundation/.../logistics/datagen/LogisticsDataParts.java` make the Item Pipe `CIC/GHG/CIC` and the Fluid Pipe `CIC/GBG/CIC`, eight each; run `gradlew :foundation:runData` and check the two recipe JSON files changed
- [x] 2.2 Add the GameTest `pipeRecipes` in `foundation/.../logistics/gametest/ServusLogisticsGameTests.java` (both grids make eight, the old six iron grid makes nothing); run `gradlew :foundation:runGameTestServer`

## 3. Farm rail corrosion

- [x] 3.1 Set `corrosion_water` to 9000 in `shuttles/src/main/resources/data/servus_shuttles/shuttles/field.json` and `ShuttlesStatsManager.DEFAULT_FIELD_CORROSION_WATER` to 9000; check `RailCorrosionGameTests` and the `field <pos> age` command read the value instead of assuming 6000; run `gradlew :shuttles:runGameTestServer`

## 4. Docs

- [x] 4.1 Update `docs/src/content/docs/mods/alchemy.md`, `getting-started.md` (Calciner materials), `mods/foundation.md` (pipe materials) and `mods/shuttles.md` (9000 mB, about 2h 30m for five rails); run `gradlew :alchemy:runData` so `docs/src/data/gamedata.json` is current

## 5. Athanor and Coagulator recipes

- [x] 5.1 In `alchemy/.../datagen/ServusAlchemyRecipeProvider.java` craft the Athanor from Stone Bricks (pattern unchanged) and the Coagulator as `SGS/GCG/SRS` with cobblestone; run `gradlew :alchemy:runData` and check both recipe JSON files changed
- [x] 5.2 Move the crafting grid helper of `CalcinerGameTests` into `alchemy/.../gametest/CraftingGrids.java` and add `AlchemyGameTests.athanorRecipe` and `CoagulatorGameTests.coagulatorRecipe` (new grid crafts one, old material crafts nothing); run `gradlew :alchemy:runGameTestServer`
- [x] 5.3 Update `docs/src/content/docs/mods/alchemy.md` (Athanor and Coagulator materials) and regenerate `docs/src/data/gamedata.json`; `gradlew build` and `gradlew test` green

## 6. Verification

- [x] 6.1 Full `gradlew build` and `gradlew test` green, GameTest servers of foundation, alchemy and shuttles pass (pre-existing failures noted)
- [ ] 6.2 User play check: the game launches with all mods, the three Calciner parts, the Athanor, the Coagulator and both pipes craft from the new grids in JEI and at a table, and a watered copper field ages more slowly
