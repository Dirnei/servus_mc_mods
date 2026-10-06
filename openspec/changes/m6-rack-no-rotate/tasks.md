# Tasks

## 1. No rotation

- [x] 1.1 Remove the rotate branch from `HeavyRackBlock.onWrench` and `facingChanged` from `HeavyRackBlockEntity`. Add the GameTest `carrotDoesNotTurnRack` per design.md. Verify `gradlew :storage:runGameTestServer` passes all thirteen storage tests.

## 2. Verification

- [x] 2.1 Full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. In game: click a rack with the carrot and see nothing change, sneak click to dismantle. Commit.
