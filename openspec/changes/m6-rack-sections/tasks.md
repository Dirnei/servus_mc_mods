# Tasks

## 1. Pure section rule

- [x] 1.1 Replace `RackRow` with `joinLeft` and `joinRight` and rewrite `RackRowTest` (lone bay, join a section of two, refuse a full section, join two single bays, refuse the right side beyond the cap). Verify `gradlew :storage:test` is green.

## 2. Placement logic and data

- [x] 2.1 Rework `HeavyRackBlock`: section measurement at placement, neighbour mirroring in `onPlace`, flag clearing only in `updateShape`, rotation without joins; rename the key to `bays_per_section` in `rack.json` and `StorageStatsManager`. Add the GameTests `rackSectionsStayPut` and `upperRowFollowsLowerSections` per design.md. Verify `gradlew :storage:runGameTestServer` passes all ten storage tests.

## 3. Verification

- [x] 3.1 Full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. In game: place three racks in a row, then a fourth at the left end and see the first three unchanged and the fourth alone, place a fifth beside the fourth and see them join. Commit.
