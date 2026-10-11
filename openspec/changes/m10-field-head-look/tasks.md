## 1. Pure logic

- [x] 1.1 Add `storage/.../field/FieldHeadLook.java` (ripe step 0..5 and water level 0..8, no Minecraft imports) and JUnit `storage/src/test/.../field/FieldHeadLookTest.java` (ripeStepEmpty, ripeStepProgress, ripeStepBelowDueStaysGold, ripeStepDue, waterLevelEmptyAndFull, waterLevelSteps, waterLevelZeroCapacity); `gradlew :storage:test`

## 2. Block, wood and state

- [x] 2.1 `FieldHeadBlock`: `FACING` and `RIPE` properties, placement facing the player, rotate and mirror; `ServusStorageItems.FIELD_HEAD` becomes a `WoodBlockItem`; `ServusStorage` registers the head with `Wood.register`; `gradlew build`
- [x] 2.2 `FieldHeadBlockEntity`: `WoodHolder` with `WoodSlot` forwarding (save, load, update tag, components, model data), update packet, `WaterLevel`, `Crop` and `HarvestOnly` in the update tag, `lookDirty` flag applied on the server tick, water level sync only on level change; `gradlew build`

## 3. Textures and datagen

- [x] 3.1 Draw `field_head_side`, `field_head_top`, `field_head_bottom` and `field_head_front_0` .. `_5` overlays with a Python generator in the job tmp dir (frame pixels from `inbound_station_side` and `_top`); `gradlew build`
- [x] 3.2 Datagen: `ServusStorageBlockStateProvider` (layered models per RIPE step, rotated by FACING, item model step 0), `ServusStorageBlockLoot` (head in `WOODEN`), `ServusStorageRecipeProvider` (planks recipe through `WoodRecipes.output`); `gradlew :storage:runData`, `gradlew build`

## 4. Renderer

- [x] 4.1 `storage/.../client/FieldHeadRenderer.java`: water quads per side up to the synced level with the water still texture and biome tint, crop item on the top plate; register in `ServusStorageClient`; `gradlew build`

## 5. Tests and docs

- [x] 5.1 GameTests `storage/.../gametest/FieldLookGameTests.java`: placementFacesPlayer, woodSurvivesPlaceAndBreak, recipeRecordsWood, ripeStateFollowsRipeness, ripeStateFollowsThreshold, updateTagCarriesWaterAndCrop; keep `FieldGameTests` and `HarvesterGameTests` green; `gradlew :storage:runGameTestServer`
- [x] 5.2 Docs `docs/src/content/docs/mods/storage.md` Field Head entry and `PLAN.md` M10 list the change; `gradlew build`

## 6. Verification

- [ ] 6.1 Full `gradlew build`, unit tests and storage GameTests green
- [ ] 6.2 Play check by the user in game with all three mods: wood per recipe, facing, ripe bar steps and green, water window rising and biome tint, crop icon size on the plate
