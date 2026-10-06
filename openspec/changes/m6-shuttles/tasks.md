# Tasks

## 1. Pure path finding and movement

- [ ] 1.1 Add `storage/.../shuttle/RailPaths.java` (breadth first search over a neighbours function) and `storage/src/test/.../shuttle/RailPathsTest.java` (straight line, branch picks the shorter, unreachable, start is goal); add `ShuttleMotion.java` (segments, progress, advance, position, reverse) and `ShuttleMotionTest.java` (crossing segments, arrival, one node path, interpolation). Verify `gradlew :storage:test` is green.

## 2. Fleet, controller and terminal

- [ ] 2.1 Add `Shuttle`, `PickJob` and `ShuttleFleet` in the `shuttle` package per design.md; give `WarehouseControllerBlockEntity` a persisted `shuttleCount` with `addShuttle` and `takeShuttle`; wire the fleet into `WarehouseNetwork` (home rail, `canShuttle`, `requestPick`, tick, controller add, remove and absorb, summary); add `max_shuttles` and `shuttle_speed` to `warehouse.json` and `StorageStatsManager`; register `ServusStorageItems.SHUTTLE`; handle shuttle add and take in `WarehouseControllerBlock` and drop shuttles on dismantle and break. Verify `gradlew :storage:compileJava` passes.
- [ ] 2.2 Route `CounterTerminalMenu.pull` through `requestPick` when `canShuttle` is true. Add the GameTest `shuttlePicksFromRail` per design.md. Verify `gradlew :storage:runGameTestServer` passes all six storage tests.

## 3. Sync and rendering

- [ ] 3.1 Add `ShuttleView` and `ShuttleSyncPayload`, register it in `ServusStoragePayloads`, send it from the fleet tick while moving and on fleet changes; add `ClientShuttles` and `WarehouseControllerRenderer` with an infinite render box, registered in `ServusStorageClient`. Verify `gradlew build` passes.

## 4. Assets, data and docs

- [ ] 4.1 Draw `textures/item/shuttle.png` with a scratchpad script; add the shuttle item model (box with block display transforms) to the storage datagen, the recipe and the lang entry. Run `gradlew :storage:runData` and verify the item model, recipe and lang entry exist.
- [ ] 4.2 Update `PLAN.md`: section 2.3 states how shuttles work and section 5 drops the shuttle open decision. Verify `openspec validate m6-shuttles` passes.

## 5. Verification

- [ ] 5.1 Full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. In game: craft a shuttle, add it to the controller, pull from a crate at the end of a rail at the terminal, watch the cart drive there and back and the stack arrive, queue three pulls, sneak click the controller to take the shuttle out. Commit.
