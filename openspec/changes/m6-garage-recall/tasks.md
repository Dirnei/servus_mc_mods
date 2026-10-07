# Tasks

## 1. Fleet

- [ ] 1.1 In `ShuttleFleet.java`: tick motions without a home, `recall(pos)`, recall in `refreshHome`, `active` on the fleet garage, `adoptGarage(pos, count, active)`, `freeShuttles` skipping paused garages, summary suffix. Pass `active` through `WarehouseNetwork.java` and add `recallGarage(pos)`. Verify `gradlew :storage:build` is green.

## 2. Garage block and GUI

- [ ] 2.1 `ShuttleGarageBlockEntity.java`: `active` flag with NBT and `setActive`. `ShuttleGarageMenu.java`: `active` data slot, `clickMenuButton` for recall and toggle. `ShuttleGarageScreen.java`: the two buttons and the status suffix. Lang keys in `ServusStorageLanguageProvider.java`. Run `gradlew :storage:runData` and verify `gradlew :storage:build` is green.

## 3. Tests

- [ ] 3.1 Add GameTests `garageRecallBringsShuttlesHome`, `pausedGarageTakesNoJobs`, `losingTheRailRecalls` and `shuttlesFinishWithoutAnyHome` to `ServusStorageGameTests.java`. Verify `gradlew :storage:runGameTestServer` passes all storage tests.

## 4. Verification

- [ ] 4.1 Run `openspec validate m6-garage-recall --strict`, full `gradlew clean build` green (with no client running), `gradlew runClientAll` launches. Commit.
- [ ] 4.2 Play check: Recall frees the stuck shuttles, Paused stops a garage from sending shuttles, breaking the rail behind a garage brings its shuttles home.
