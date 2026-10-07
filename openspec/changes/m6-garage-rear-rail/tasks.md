# Tasks

## 1. Rail at the back as a plain arm

- [x] 1.1 Change `ShuttleGarageBlockEntity.acceptsRail` to the back and add `showsDock()` returning false; add `showsDock()` to `StorageNodeBlockEntity.java` and use it for the `DOCK_*` flags in `RailBlock.java`. Add the `garage` model helper and the `shuttle_garage_back` texture, regenerate the front texture without the doorway. Run `gradlew :storage:runData` and verify `gradlew :storage:build` is green.

## 2. Tests

- [x] 2.1 Flip the garage facings in `ServusStorageGameTests.java` so every rail lies behind its garage and add `garageRailIsAnArm`. Verify `gradlew :storage:runGameTestServer` passes all storage tests.

## 3. Verification

- [ ] 3.1 Run `openspec validate m6-garage-rear-rail --strict`, full `gradlew clean build` green, `gradlew runClientAll` launches. Commit.
- [ ] 3.2 Play check: garage placed from the aisle has the doorway on the rail and the lamp towards the player, the rail shows a plain arm, shuttles leave and return through the back.
