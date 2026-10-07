# Tasks

## 1. Block, rows and network view

- [ ] 1.1 Add `storage/src/main/java/io/github/dirnei/mc/servus/storage/board/OrderBoardBlock.java`, `OrderBoardBlockEntity.java`, `StepRow.java`, `MachineRow.java`, `OrderRow.java`, `OrderBoardPayload.java` and `CancelOrderPayload.java`; add `machineRows`, `orderRows` and `cancelOrder` to `WarehouseNetwork.java`; register block, block entity, item, creative tab entry and payloads. Verify `gradlew :storage:build` is green.

## 2. Menu, screen, assets

- [ ] 2.1 Add `menu/OrderBoardMenu.java`, `client/ClientOrders.java` and `client/OrderBoardScreen.java`, register the menu and screen, add datagen (blockstate, loot, tag, lang, recipe) and the block and GUI textures. Run `gradlew :storage:runData` and verify `gradlew :storage:build` is green.

## 3. Tests

- [ ] 3.1 Add GameTests `orderBoardListsMachinesAndOrders` and `orderBoardCancelsOrder` to `ServusStorageGameTests.java`. Verify `gradlew :storage:runGameTestServer` passes all storage tests.

## 4. Verification

- [ ] 4.1 Run `openspec validate m7-order-board --strict`, full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. Commit.
