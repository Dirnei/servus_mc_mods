# Tasks

Paths below are relative to `storage/src/main/java/io/github/dirnei/mc/servus/storage/` (main) and `storage/src/test/java/io/github/dirnei/mc/servus/storage/` (test) unless they start with `storage/` or the repo root.

## 1. Pure lift logic

- [x] 1.1 Add `shuttle/Shafts.java` (`Shaft(bottom, topY)` record, `group(Set<NodePos>)` grouping elevator nodes by column into maximal consecutive y runs, `floorsOf`) and `shuttle/ShaftsTest.java`: one column of three, a column with a gap gives two shafts, single segment, two columns side by side stay apart. Verify `gradlew :storage:test` is green.
- [x] 1.2 Add `shuttle/Flaps.java` (`angle(carY, floorY)`: 90 within half a block, 0 from one block, linear between) and `shuttle/FlapsTest.java`: angles at distance 0, 0.5, 0.75, 1 and 3, above and below. Verify `gradlew :storage:test` is green.
- [x] 1.3 Add `shuttle/LiftCar.java` per design.md (`call`, `canBoard`, `board`, `send`, `arrivedFor`, `leave`, `canCross`, `beginCross`, `endCross`, `tick(speed)`, `resize`, `remove`, `y`, `target`, `moving`) and `shuttle/LiftCarTest.java`: starts at the bottom; FIFO with two callers on different floors; boarding only for the first caller with the car idle at its floor; ride of three floors at 0.1 takes 30 ticks; car parks at the last floor; `canCross` false while the car is within one block and moving, true when it is idle at the floor with no rider; a crosser on a floor in the sweep holds departure; resize clamps the car; `remove` drops a call, a ride and a crossing. Verify `gradlew :storage:test` is green.
- [x] 1.4 Add `shuttle/ShaftSteps.java` (step kinds `PLAIN`, `ENTER_RIDE`, `ENTER_CROSS`, `RIDE`, `EXIT`, plus `rideEnd(path, i)`) and `shuttle/ShaftStepsTest.java`: rail, segment, segment up, rail gives `ENTER_RIDE`, `RIDE`, `EXIT`; a straight crossing gives `ENTER_CROSS` then `EXIT`; a path ending on a segment gives `ENTER_RIDE`; `rideEnd` finds the last node of the vertical run. Verify `gradlew :storage:test` is green.
- [x] 1.5 Extend `shuttle/ShuttleMotion.java` with `advance(blocks, IntPredicate mayStart)` (holds on a node when the gate refuses a new segment) and `skipTo(index)`; keep `advance(blocks)` working. Add to `shuttle/ShuttleMotionTest.java`: gate refuses segment 2 and the shuttle stops on node 2 with progress 0; gate opens and it moves on; `skipTo` lands on the node with progress 0. Verify `gradlew :storage:test` is green and commit.

## 2. Segment block: no collision, climbable, new models

- [x] 2.1 In `rail/ElevatorBlock.java` return `Shapes.empty()` from `getCollisionShape` and `Shapes.block()` from `getShape`. Drop the old shape maps. Add the elevator to `BlockTags.CLIMBABLE` in `datagen/ServusStorageBlockTagsProvider.java`. Verify `gradlew :storage:build` is green.
- [x] 2.2 Rework `datagen/ElevatorModels.java`: the multipart has `elevator_frame` (posts) always and a new `elevator_head` (cap beam and motor box over the posts) when `up=false`; remove `rail_center` and `rail_arm` from the elevator blockstate. Add the head texture with the png helper under `storage/src/main/resources/assets/servus_storage/textures/block/`. Run `gradlew :storage:runData` and verify `gradlew :storage:build` is green.
- [x] 2.3 Add a GameTest `elevatorHasNoCollision` to `gametest/ServusStorageGameTests.java`: a segment's collision shape at its position is empty and the block is in `BlockTags.CLIMBABLE`. Verify `gradlew :storage:runGameTestServer` passes all storage tests and commit.

## 3. Cars in the fleet

- [x] 3.1 Add `lift_speed` (example 0.1) to `storage/src/main/resources/data/servus_storage/storage/warehouse.json`, `liftSpeed()` with a default in `crate/StorageStatsManager.java`, and `liftSpeed()` and `shaftOf(NodePos)` to `ShuttleFleet.Host`. In `network/WarehouseNetwork.java` keep an `elevators` set from the node events, a `shaftsDirty` flag and the `Shafts` lookup, and call `fleet.refreshShafts(...)` at the start of the next tick. Verify `gradlew :storage:build` is green.
- [x] 3.2 In `shuttle/ShuttleFleet.java` add shuttle ids, `Map<BlockPos, LiftCar> lifts`, `refreshShafts` (keep or resize cars by bottom, new cars at the bottom, riders and callers of lost or cut shafts get `dropCargo` then `goHome` or `park`), `absorb` of lifts, and `LiftCar.tick(liftSpeed)` before the shuttles move. Add a GameTest `shaftBreakDropsRiderCargo` (a segment broken during a ride: cargo drops at the garage, request shows it missing, shuttle returns home). Verify `gradlew :storage:runGameTestServer` passes all storage tests.
- [x] 3.3 Gate the shuttle moves with `ShaftSteps` per design.md: `ENTER_RIDE` calls and boards, `RIDE` sends the car, holds and `skipTo`s on arrival, a leg that starts on a segment calls the car to its floor, `ENTER_CROSS` waits for `canCross` and registers the crossing, the rider leaves and the crosser ends on reaching the outside rail. Legs ending on a segment keep the car. Waiting shuttles report speed 0 in `views`. Add GameTests `shuttleRidesCarToUpperRack` (garage and terminal on the ground floor, rack two floors up: the cargo arrives in the output slots, and the car was used), `shuttlesQueueForCar` (two requests from different floors are served in call order), `shuttleCrossesShaftWithoutCar` (crossing on the ground floor with the car parked above: the car does not move). Verify `gradlew :storage:runGameTestServer` passes all storage tests and commit.

## 4. Sync and rendering

- [x] 4.1 Add `shuttle/LiftSyncPayload.java` (`bottom`, `topY`, `y`, `target`, `speed`, `carrying`) and register it in `terminal/ServusStoragePayloads.java`. Send it from the fleet when a car starts or stops and every sync interval while it moves. Store the parked `liftY` on the bottom `rail/ElevatorBlockEntity.java` with `getUpdateTag` and `getUpdatePacket`. Add `client/ClientLifts.java`. Add `Optional<BlockPos> lift` to `shuttle/ShuttleView.java` and set it for riding shuttles. Grep that every payload `TYPE` is registered. Verify `gradlew :storage:runGameTestServer` passes all storage tests.
- [x] 4.2 Add `client/ElevatorRenderer.java` per design.md (only on segments with `down=false`, column walk, decks with two flaps on post to post axles, axis from the arms, rail drawn per arm, angle from `Flaps.angle`, car platform with four post sleeves, column render bounds) and register it in `client/ServusStorageClient.java`. Verify `gradlew :storage:build` is green.
- [x] 4.3 In `client/ShuttleGarageRenderer.java` draw riding shuttles at the extrapolated car height of their shaft, and make `headingAfter` keep the last horizontal heading on vertical segments. Verify `gradlew :storage:build` is green and commit.

- [ ] 4.4 Play check fix in `client/ElevatorRenderer.java`: deck plate at y -1 to 0 px with the rail at 0 to 1 px flush with the outside rail, car platform likewise; flaps hinge only on edges without an arm (two opposite free edges, else one flap on a free edge, else four quarters retracting into the posts), axles only on hinge edges. Verify `gradlew :storage:build` is green and in game the rail joins the outside rail and a boarding shuttle never passes a flap.

## 5. Plan, specs and verification

- [x] 5.1 Add a paragraph to `PLAN.md` section 2.3 on the lift car (one per shaft, FIFO calls, one shuttle per trip, decks that fold, crossing without the car, climbable shaft). Run `openspec validate m6-elevator-car --strict` and fix any finding. Verify that validate passes.
- [ ] 5.2 Full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. In game, from the main checkout only and when the user says so:
  - Build a three floor shaft with rails on floors 1 and 3 and one floor without rails. See posts only on the bare floor and the head on top.
  - Walk through a segment and climb it with jump. Sneak to hold.
  - Request rack items from floor 3 and watch the shuttle wait, board, ride with the decks folding, and deliver.
  - Cross the shaft sideways on floor 1.
  - Break a segment mid ride and see the cargo drop.

  Commit.
