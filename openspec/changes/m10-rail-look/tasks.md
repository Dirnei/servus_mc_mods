## 1. Pure geometry

- [x] 1.1 Add `storage/.../rail/RailLook.java` (pieces per mask, boxes for single, end, straight, curve, cross, dock and dock inline, rotation; no Minecraft imports) and JUnit `storage/src/test/.../rail/RailLookTest.java` (pieceForEveryMask, rotationRoundTrip, straightGaugeAndSleepers, curveMeetsEdgesAtGauge, curveIsSymmetric, crossHeadsDoNotOverlap, boxesStayInsideBlock, dockInlineReachesBeam); `gradlew :storage:test`

## 2. Wood rails

- [x] 2.1 `RailBlockEntity` implements `WoodHolder` with a `WoodSlot` (save, load, update tag and packet, components, model data); new `rail/RailItem` (assist plus wood tooltip) for `ServusStorageItems.RAIL` and `FARM_RAIL`; `ServusStorage` registers both rails with `Wood.register`; `ConnectableBlock.onWrench` in Foundation stamps the wood on the dismantled item; `gradlew build`

## 3. Textures and datagen

- [x] 3.1 Draw `rail_head` (steel) and `farm_rail_head` (copper with verdigris) with a Python PIL generator in the job tmp dir; delete `rail`, `rail_curve`, `rail_cross`, `farm_rail`, `farm_rail_curve`, `farm_rail_cross`; `gradlew build`
- [x] 3.2 Rewrite `datagen/RailModels.java` (piece models from `RailLook`, multipart per mask, dock beam and inline parts, item model from the straight piece); `ServusStorageBlockLoot` (rails in `WOODEN`); `ServusStorageRecipeProvider` (iron and planks, copper and planks through `WoodRecipes.output`); `gradlew :storage:runData`, `gradlew build`

## 4. Renderers

- [x] 4.1 `client/ElevatorRenderer.java`: deck, car and ground floor rail from `RailLook` boxes with `rail_head` and oak planks, sleepers clipped to the shaft area; `gradlew build`
- [x] 4.2 `client/ShuttleGarageRenderer.java`: `FLOOR` 2/16 so wheels stand on the heads, also on a lift car; `gradlew build`

## 5. Tests and docs

- [x] 5.1 GameTests `storage/.../gametest/RailLookGameTests.java`: woodSurvivesPlaceAndBreak, dismantleKeepsWood, railRecipeRecordsWood, farmRailRecipeTakesCopper, runKeepsWood (sneak placement); keep rail, elevator, shuttle, field and harvester GameTests green; `gradlew :storage:runGameTestServer`
- [x] 5.2 Docs `docs/src/content/docs/mods/storage.md` rail entries and `PLAN.md` M10 list the change; `gradlew build`

## 6. Verification

- [ ] 6.1 Full `gradlew build`, unit tests and storage GameTests green
- [ ] 6.2 Play check by the user in game: straight, end, curve, crossing and dock pieces of both rails in several woods, the item look, the elevator deck, car and ground floor rail, shuttles with their wheels on the heads on rails and on the lift car
