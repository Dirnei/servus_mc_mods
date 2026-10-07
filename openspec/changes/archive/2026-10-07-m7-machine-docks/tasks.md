# Tasks

## 1. Dock block entity and block

- [x] 1.1 Add a protected typed constructor to `storage/src/main/java/io/github/dirnei/mc/servus/storage/station/InboundStationBlockEntity.java`, add `storage/src/main/java/io/github/dirnei/mc/servus/storage/network/DeliveryTarget.java` and implement it on `CounterTerminalBlockEntity`, add `storage/src/main/java/io/github/dirnei/mc/servus/storage/dock/MachineDockBlockEntity.java` (feed buffer, `serverTick` feeding, `acceptsRail`, `producer()`, `deliveries()`, drop both buffers) and `MachineDockBlock.java` (facing, lamps, ticker, menu, carrot, drops), register block, block entity, item and capability, read `dock_feed_interval` in `StorageStatsManager.java` and add it to `warehouse.json`. Verify `gradlew :storage:build` is green.

## 2. Network: deliveries to any buffer, feeding, producers

- [x] 2.1 In `WarehouseNetwork.java` replace `deliverToTerminal` by `deliverTo` over `DeliveryTarget`, factor `request` into `planAndStart`, add `feedDock(...)`, `producers()` and the `Producer` record; add `producers` and `feed` to `ServusStorageCommands.java`. Add GameTests `dockFeedsTheBlockItFaces`, `dockCollectsAndPutsAway`, `dockFacingAChestIsNoProducer`, `feedDockFromCrate`, `feedDockByShuttle` and `dockFrontTakesNoRail` to `ServusStorageGameTests.java`. Verify `gradlew :storage:runGameTestServer` passes all storage tests.

## 3. GUI, datagen and textures

- [x] 3.1 Add `storage/src/main/java/io/github/dirnei/mc/servus/storage/menu/MachineDockMenu.java` and `storage/src/main/java/io/github/dirnei/mc/servus/storage/client/MachineDockScreen.java`, register them, add datagen (blockstate via `station()`, loot, tag, recipe, lang) and placeholder textures for the block and the GUI. Run `gradlew :storage:runData` and verify `gradlew :storage:build` is green and `gradlew runClientAll` launches; in the client with all four mods place a dock facing an Assembler, check `/servus_warehouse producers`, feed ingots and watch gears flow into a crate. (Done autonomously on 2026-10-07: feeding, collecting and the producer lookup are covered by GameTests with vanilla blocks and by an RCON check on the all mods server where a dock facing an Assembler is listed as a producer; the full Assembler loop in the client is left for the user's play test.)

## 4. Verification

- [x] 4.1 Run `openspec validate m7-machine-docks --strict`, full `gradlew clean build` green, `gradlew test` green. Commit.
