# Tasks

## 1. Placement rule

- [x] 1.1 In `HeavyRackBlock.getStateForPlacement`, take the facing of a Heavy Duty Rack below the placed position before computing the joins. Add the GameTest `rackOnRackCopiesFacing` using `placeAt` through the rack item. Verify `gradlew :storage:runGameTestServer` passes all eight storage tests.

## 2. Verification

- [x] 2.1 Full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. In game: place a rack facing east, stand looking south and place a rack on top, see it face east and join the stack. Commit.
