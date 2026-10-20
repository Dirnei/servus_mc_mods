# Tasks

Paths are relative to `shuttles/src/main/java/io/github/dirnei/mc/servus/shuttles/`. GameTests run with `gradlew :shuttles:runGameTestServer`.

## 1. Bucket on the head

- [x] 1.1 `field/FieldHeadBlock.java`: `useItemOn` as in design.md D1. Run `gradlew build`.
- [x] 1.2 `gametest/FieldGameTests.java`: `fieldHeadTakesWaterBucket`, `fieldHeadRefusesLavaBucket`, `fieldHeadGivesNoWaterToEmptyBucket`, `fullFieldHeadKeepsBucket` (follow `FluidTankGameTests.bucketsFillAndEmpty` for the mock player and `useItemOn`). Run the Shuttles GameTests.

## 2. Verify

- [x] 2.1 Full `gradlew build`, `gradlew test` and the Shuttles GameTests green; the game launches with all three mods.
- [x] 2.2 Play check by the user: a water bucket fills a Field Head, the window water rises, a lava bucket does nothing.
