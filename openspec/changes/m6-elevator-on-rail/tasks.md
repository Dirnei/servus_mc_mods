# Tasks

Paths are relative to `shuttles/src/main/java/io/github/dirnei/mc/servus/shuttles/` unless written in full. GameTests run with `gradlew :shuttles:runGameTestServer`. Close the play client before building.

## 1. Placement and the remembered rail

- [x] 1.1 `rail/ElevatorItem.java` placement on a Warehouse Rail and `rail/ElevatorBlockEntity.java` remembered rail, saved (design.md D1, D2). Run `gradlew build`.
- [x] 1.2 `rail/ElevatorBlockEntity.java` and `rail/ElevatorBlock.java` put the rail back after removal (design.md D3). Run `gradlew build`.

## 2. Tests

- [x] 2.1 GameTests in `gametest/ElevatorGameTests.java`: `segmentPlacedOnRailTakesItsPlace`, `brokenSegmentGivesTheRailBack`, `railComesBackWithItsWood`, `fallingShaftLeavesTheRail`, `bareGroundSegmentLeavesNothing`, `clickOnRailSegmentBuildsUp`, `farmRailIsNotReplaced`, `rememberedRailSurvivesSave`. Run the Shuttles GameTests.

## 3. Verify

- [x] 3.1 Full `gradlew build`, `gradlew test` and the Shuttles GameTests green; update the elevator paragraph in `docs/src/content/docs/mods/shuttles.md` and run the docs tests.
  Verified 2026-10-20: build and unit tests green, Shuttles GameTests 720 passed, docs tests 50 passed.
- [ ] 3.2 Play check by the user: place an elevator into a laid rail run, build it up, break it and see the rail come back.

## 4. Segment wood

- [x] 4.1 Foundation `wood/WoodRecipe.java` reads the wood component of ingredients, `wood/client/WoodTextures.java` `spriteOf` public (design.md D4). JUnit for `WoodPick` stays as it is. Run `gradlew :foundation:build`.
- [x] 4.2 `rail/ElevatorBlockEntity.java` `WoodHolder` with a `WoodSlot`, sync and drop; `registry/ServusShuttlesItems.java` elevator as a wood item; D1 sets the rail's wood; `datagen/ElevatorModels.java` item sleepers on `Wood.BOARD` and the wood item model type; `datagen/ServusShuttlesRecipeProvider.java` elevator recipe as `wood_shaped`; `client/ElevatorRenderer.java` sleepers in the segment's wood (design.md D4). Run `gradlew :shuttles:runData`, `gradlew build`.
- [x] 4.3 GameTests in `gametest/ElevatorGameTests.java`: `segmentOnRailTakesItsWood`, `craftedElevatorCarriesTheRailWood`, `brokenSegmentDropsItsWood`, `placedItemGivesItsWood`. Run the Shuttles GameTests.
  Verified 2026-10-20: GameTests Foundation 159 and Shuttles 725 passed; the elevator now drops through the wood loot list (the self-drop list went empty and was removed); `gamedata.json` regenerated, docs tests 50 passed.
- [ ] 4.4 Play check by the user: a spruce rail run takes a spruce segment, a birch rail crafts birch elevators, the item shows its wood, a shaft of mixed woods draws each floor in its own wood.
