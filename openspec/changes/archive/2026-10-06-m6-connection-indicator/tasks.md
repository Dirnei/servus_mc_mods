# Tasks

## 1. Block state and network push

- [x] 1.1 Add `storage/.../network/StorageNodeBlocks.java` with the `ONLINE` property; add it to the state definitions and default states of `CrateBlock`, `RackBlock`, `WarehouseControllerBlock` and `CounterTerminalBlock`; add the `lampsDirty` flag and the push in `WarehouseNetwork.onTick` per design.md; extend `RailBlock.connectsTo` to stock holding storage nodes. Verify `gradlew :storage:compileJava` passes.

## 2. Textures and datagen

- [x] 2.1 Edit the four front textures to carry a dark lamp and write the four `_front_online` textures with a scratchpad script; replace `oriented` in `ServusStorageBlockStateProvider` with the two model variant builder over `FACING` and `ONLINE`. Run `gradlew :storage:runData` and verify each of the four blockstates has eight variants and the `_online` models exist.

- [x] 2.2 Add the four `dock_*` properties to `RailBlock` with placement and shape update logic, the `rail_dock` model and multipart conditions in `RailModels`, and draw `textures/block/rail_dock.png`. Run `gradlew :storage:runData` and verify the rail blockstate has nine multipart entries and `rail_dock.json` exists.

## 3. Verification

- [x] 3.1 Extend `railsLinkCrateAndController` and `elevatorLinksFloors` in `ServusStorageGameTests` with the `ONLINE` assertions per design.md. Verify `gradlew :storage:runGameTestServer` passes all three storage tests.
- [x] 3.2 Full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. In game: place a crate alone (dark lamp), add a controller beside it (both lit), add a second controller (all dark), remove it (lit again), lay a rail beside a rack and see the arm dock onto it. Commit.
