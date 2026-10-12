# Tasks

Paths are relative to `storage/src/main/java/io/github/dirnei/mc/servus/storage/` and `storage/src/main/resources/assets/servus_storage/` unless written in full. GameTests run with `gradlew :storage:runGameTestServer`. No pure logic is added, so there are no JUnit tests.

## 1. Wood on the workshop

- [x] 1.1 `shuttle/module/ShuttleWorkshopBlockEntity.java`: implement `WoodHolder` with a `WoodSlot` like `production/ProductionTerminalBlockEntity.java` (save, load, implicit components, `removeComponentsFromTag`, `getModelData`); `ServusStorage.java`: add `SHUTTLE_WORKSHOP` to the `Wood.register` list (design.md D3). Run `gradlew build`.
- [x] 1.2 Datagen: `datagen/ServusStorageBlockLoot.java` moves the workshop from `SELF_DROPPING` to `WOODEN`; `datagen/ServusStorageRecipeProvider.java` saves the workshop recipe through `WoodRecipes.output(output, registries)`. Run `gradlew :storage:runData` and `gradlew build`.
- [x] 1.3 GameTest `gametest/BlockStyleGameTests.java`: `workshopRecipeRecordsAcacia`; extend `woodSurvivesPlaceAndBreak` (or add `workshopWoodSurvivesBreak`) for the workshop. Run the Storage GameTests.

## 2. Carrot dismantle

- [ ] 2.1 `shuttle/module/ShuttleWorkshopBlockEntity.java`: the shuttle and supplies as `DataComponents.CONTAINER` (7 entries) in `collectImplicitComponents` and `applyImplicitComponents`, removed from the tag in `removeComponentsFromTag`; a method that clears both handlers and resets `handBuild` and `buildStep` (design.md D4). `shuttle/module/ShuttleWorkshopBlock.java`: implement `Wrenchable`; sneak carrot drops one item with `collectComponents()`, clears the handlers, removes the block; a plain carrot click passes. Run `gradlew build`.
- [ ] 2.2 GameTests in `gametest/ShuttleModuleGameTests.java`: `workshopDismantleKeepsContents` (one item entity dropped, placed again it holds the Mk2 shuttle with Speed 1, 8 iron ingots and spruce), `workshopDismantleCancelsHandBuild`, `workshopPlainCarrotDoesNothing`; the existing break test still drops the contents. Run the Storage GameTests.

## 3. Look

- [ ] 3.1 Texture script in `C:\Users\Dirnei\AppData\Local\Temp\servus-helpers\jobs\workshop-look\gen_workshop.py` (not in the repo) drawing `textures/block/shuttle_workshop_side.png`, `shuttle_workshop_top.png` and `shuttle_workshop_bottom.png` as overlays with transparent board panels (design.md D2: tools on the sides, bench plate with shuttle outline and vise on top, plain frame bottom, no teal, no lamp). Check the PNGs are 16x16 RGBA. Run `gradlew build`.
- [ ] 3.2 `datagen/ServusStorageBlockStateProvider.java`: replace the `cubeBottomTop` workshop model with `LayeredModels.cube` (four sides, top, bottom) and keep `simpleBlock` and `simpleBlockItem` (design.md D1). Run `gradlew :storage:runData` and `gradlew build`.

## 4. Docs and plan

- [ ] 4.1 `PLAN.md` M10: add item 12 `m10-workshop-look` (family look with tools and bench, recipe wood, carrot dismantle keeping contents, no facing); `docs/src/content/docs/mods/storage.md` workshop entry names the wood and the carrot dismantle; regenerate `docs/src/data/gamedata.json` with `gradlew :manufacturing:runData`. Run `cd docs && npm test`.

## 5. Verification

- [ ] 5.1 Full `gradlew build`, `gradlew test`, `gradlew :storage:runGameTestServer` and `gradlew :foundation:runGameTestServer` green, the game launches with all three mods. Then the play check by the user from the main checkout: an oak and a dark oak workshop next to a Shuttle Garage show the family frame, the boards in their wood, the hanging tools and the bench top, no lamp and no teal; the item looks the same and its tooltip names the wood; sneak carrot dismantles a workshop with a shuttle and supplies into one item whose tooltip lists them, and placing it restores them.
