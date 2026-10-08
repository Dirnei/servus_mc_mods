# Tasks

## 1. Wood on the rack

- [x] 1.1 Add pure `rack/RackWood.java` with JUnit `RackWoodTest` (three equal, two to one in both orders, three different picks the middle, single entry, empty list). Verify `gradlew :storage:test`.
- [x] 1.2 Add the `rack_wood` component in `registry/ServusStorageComponents.java`, store, sync and expose it in `rack/HeavyRackBlockEntity.java` (D1), add `rack/HeavyRackItem.java` with the tooltip and register it in `registry/ServusStorageItems.java`. Verify `gradlew build`.
- [x] 1.3 Add `rack/HeavyRackRecipe.java` and its serializer (D2), register it, switch the rack recipe in `datagen/ServusStorageRecipeProvider.java`. Run `gradlew :storage:runData` and `gradlew build`.
- [x] 1.4 GameTests in `gametest/ServusStorageGameTests.java`: recipe picks spruce, majority and middle; placed item carries the wood; drops keep it; no component means oak. Verify `gradlew :storage:runGameTestServer`.

## 2. Look

- [x] 2.1 Client: `client/HeavyRackModel.java` (D3) and the item override (D4), installed in `client/ServusStorageClient.java` through `ModelEvent.ModifyBakingResult`. Verify `gradlew build`.
- [x] 2.2 Generate `heavy_rack_iron.png`, repaint `heavy_rack_board.png` oak-like, point `datagen/HeavyRackModels.java` at the iron texture and remove `heavy_rack_steel.png`, lang for the tooltip. Run `gradlew :storage:runData` and `gradlew build`.

## 3. Joins by wood

- [x] 3.1 `rack/HeavyRackBlock.java`: joins and `UP`/`DOWN` need the same wood at placement, neighbours fixed in `setPlacedBy` (D6). GameTest in `gametest/ServusStorageGameTests.java`: spruce beside oak does not join, spruce beside spruce does. Verify `gradlew build` and `gradlew :storage:runGameTestServer`.
- [x] 3.2 Stacked racks keep their frames (D7): drop `UP` and `DOWN` from `rack/HeavyRackBlock.java` and the beam conditions in `datagen/HeavyRackModels.java`, adjust the rack GameTests. Run `gradlew :storage:runData`, `gradlew build` and `gradlew :storage:runGameTestServer`.

## 4. Verification

- [x] 4.1 Run `openspec validate m6-rack-wood-material --strict`, full `gradlew build` and `gradlew test` green, all GameTests green. Commit. Launch the client from main only when the user asks.
- [x] 4.2 Play check: racks crafted from different woods show their boards in world and in hand, the tooltip names the wood, breaking particles show it, the frame is dark iron, different woods do not join, stacked bays show an iron band between them.
