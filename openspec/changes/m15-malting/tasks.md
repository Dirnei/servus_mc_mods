# Tasks

## 1. Pure timing

- [x] 1.1 `malting/Germination` in `brewery/src/main/java/io/github/dirnei/mc/servus/brewery/`; JUnit `GerminationTest` in `brewery/src/test/java/...`: not done before the time, done at the time, remaining never below zero, a turn adds the bonus, a second turn within the cooldown is refused, a turn after the cooldown is allowed, a fresh batch can be turned at once, bonus past the time makes it done; verify `gradlew :brewery:test`

## 2. Textures

- [x] 2.1 `tools/brewery-textures/gen.py`: port `tex_noise`, the `malz` and `malt_floor` sprites; add `pale_malt`, `dark_malt`, `wheat_malt`, `green_malt`, `green_wheat_malt`, `soaked_barley`, `soaked_wheat`, the three grists and the block sprite `soaked_grain_floor`; write the PNGs, check them in an 8x preview; verify `gradlew build`

## 3. Recipe type, items, block

- [x] 3.1 `malting/GerminatingRecipe`, `registry/ServusBreweryRecipes` (type, serializer, client sync); items in `registry/ServusBreweryItems` (tab order); verify `gradlew build`
- [x] 3.2 `malting/FloorContent`, `malting/MaltingFloorBlock`, `malting/MaltingFloorBlockEntity` (slot, germination, scheduled checks, turning, hand use, drops), `registry/ServusBreweryBlocks`, `registry/ServusBreweryBlockEntities`, `ServusBreweryEvents` (item capability), registered from `ServusBrewery`; verify `gradlew build`

## 4. Datagen

- [x] 4.1 `ServusBreweryBlockStateProvider` (floor variants and block item model; check the face pair table of design.md D10 against the generated models), `ServusBreweryItemModelProvider` (skip the floor), lang, block loot, axe tag, compostables, recipes (soaking, floor, germinating, milling); run `gradlew :brewery:runData`, commit `src/generated/resources`; verify `gradlew build`

## 5. GameTests

- [x] 5.1 `gametest/MaltingGameTests` (design.md D13), listed in `BreweryGameTests`; verify `gradlew :brewery:runGameTestServer` and `gradlew build test`

## 6. Play check

- [ ] 6.1 Play check by the user: craft Soaked Barley and Soaked Wheat (bucket comes back) and a Malting Floor; the floor is a low spruce tray without flicker from several angles, empty, soaked and green; right click spreads soaked grain, empty hand turns it (sound and particles, not again right away), after a day it shows Green Malt and right click collects it; sneaking takes soaked grain back; breaking drops floor and grain; a pipe fills the floor and pulls only green malt; Pale and Dark Malt (creative tab) ground in a cranked Mill give Pale and Dark Malt Grist, also shown in JEI's Milling category
