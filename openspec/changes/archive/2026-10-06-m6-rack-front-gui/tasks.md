# Tasks

## 1. Front only interaction

- [x] 1.1 Check the hit face in `HeavyRackBlock.useItemOn` and `useWithoutItem` so only the front opens the GUI and other faces let the held item act. Add the GameTest `rackPlacesAgainstRackSide` per design.md. Verify `gradlew :storage:runGameTestServer` passes all twelve storage tests.

## 2. Verification

- [x] 2.1 Full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. In game: hold a rack, click the side and top of a rack to place more, click the front to open it. Commit.
