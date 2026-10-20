# Tasks

## 1. Pure logic

- [x] 1.1 `brewhouse/BrewhouseLayout` (D1) in `brewery/src/main/java/io/github/dirnei/mc/servus/brewery/`; JUnit `BrewhouseLayoutTest` (every clicked cell finds the layout, missing kettle block, kettle on the wrong side, missing brick, three by three bricks prefer the front row, a kettle click finds its cell); verify `gradlew :brewery:test`
- [x] 1.2 `brewhouse/ModelSlicer` with the part table of D9 (`BrewhouseModel`); JUnit `ModelSlicerTest` (cut across columns and rows drops the cut faces and keeps the outer ones, mirrored u, two layers, stretched door UVs, UV shift above 16 px, part outside the layout refused, clipping); verify `gradlew :brewery:test`
- [x] 1.3 `brewhouse/KilnClock`, `brewhouse/KilnMode` (D6); JUnit `KilnClockTest` (pale target, dark target, no dark result in dark mode, pause while cold, done, toggle); verify `gradlew :brewery:test` and `gradlew build`

## 2. Textures

- [x] 2.1 `tools/brewery-textures/gen.py`: `IRON`, `BRASS`, `COPPER` ramps; `brew_kettle_copper`, `brewhouse_door`, `brewhouse_door_lit` with `.png.mcmeta`, `kiln_grate`, `kiln_green_malt`, `kiln_dark_malt` (D11); run it, check an 8x preview; verify `gradlew build`

## 3. Recipes, fluids, stats

- [x] 3.1 `brewhouse/BrewingRecipe`, `BrewingInput`, `KilningRecipe`; types and serializers in `registry/ServusBreweryRecipes` with client sync; `brewhouse/BrewhouseStats`, `BrewhouseStatsManager` with `data/servus_brewery/brewhouse/stats.json`; `registry/BreweryFluids` (types, fluids, liquid blocks, buckets); registration in `ServusBrewery` and `ServusBreweryEvents` (reload listener) (D4, D6, D7, D8); verify `gradlew build`

## 4. Brewhouse blocks

- [x] 4.1 `brewhouse/BrewhousePart`, `TrayLook`, `BrewhouseBricksBlock`, `BrewKettleBlock`, `BrewhouseForming`, `BrewhouseShapes`, `BrewhouseBlockEntity` (forming, unforming, firebox, status), `BrewKettle`, `KettleFluids`, `KilnTray`; registration in `ServusBreweryBlocks`, `ServusBreweryItems` (tab order), `ServusBreweryBlockEntities`; capabilities (`FIREBOX`, item, fluid) in `ServusBreweryEvents` (D1 to D6, D10); verify `gradlew build`

## 5. Datagen and client

- [x] 5.1 Datagen: Brewhouse models from `ModelSlicer` and block states, loose models, bucket item models, liquid block models, lang, loot, pickaxe tag, recipes (parts, brewing, kilning) (D9, D12); check the face pair table of D9 against the generated models; run `gradlew :brewery:runData`, commit `src/generated/resources`; verify `gradlew build`
- [x] 5.2 Client: `client/BreweryClient` with the wort fluid models (D8); verify `gradlew build`

## 6. JEI

- [x] 6.1 JEI API `compileOnly` in `brewery/build.gradle`; `integration/jei/ServusBreweryJeiPlugin`, `BrewingRecipeCategory`, `KilningRecipeCategory`, lang (D13); verify `gradlew build`

## 7. GameTests

- [x] 7.1 `gametest/BrewhouseGameTests` (D14), listed in `BreweryGameTests`; verify `gradlew :brewery:runGameTestServer` and `gradlew build test`

## 8. Play check

- [ ] 8.1 Play check by the user:
  - Craft Brewhouse Bricks and Brew Kettle blocks; build six bricks three wide and two deep with four kettle blocks on the left; the carrot on the front forms it (also from a kettle block side); without the kettle blocks it is refused with the layout message; breaking any part unforms it and drops fuel, grist and malt; sneak carrot dismantles a part.
  - Look from every side and while lit: low brick firebox, two iron doors on the front that glow and flicker with flames and crackle, copper kettle with dome and vent pipe on the left, grate tray on the right; no flicker on any face, copper on every kettle face, doors and tray edges in family iron.
  - Coal by hand and through a hopper into a brick under the kettle; the fire lights only when kettle or kiln has work.
  - Kettle: water bucket in, Pale Malt Grist and Hops in, steam from the pipe while it brews, then an empty bucket gives a Pale Wort Bucket; Dark Wort and Wheat Wort the same; nothing brews without fire; empty hand tells water and wort, sneaking takes the last input back; pipes fill grist, hops and water and drain only wort.
  - Kiln: Green Malt on a tray turns Pale Malt (the tray shows green, then pale); carrot sets dark mode, then Dark Malt; Green Wheat Malt gives Wheat Malt; a little vapour while drying; empty hand takes the malt; a hopper feeds a tray and a hopper under it pulls only dried malt.
  - JEI shows the Brewing and Kilning categories; wort buckets show their colours.
