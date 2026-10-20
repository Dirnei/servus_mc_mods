# Tasks

Paths are relative to `shuttles/src/main/java/io/github/dirnei/mc/servus/shuttles/` unless written in full; JUnit tests mirror them under `shuttles/src/test/java`. GameTests run with `gradlew :shuttles:runGameTestServer`. Close the play client before building.

## 1. Pure logic

- [x] 1.1 `garage/GarageFacing.java` (design.md D1). JUnit `garage/GarageFacingTest.java`: `clickedRailWins`, `railInLookDirectionWins`, `onlyRailBesideIsUsed`, `severalRailsFallBackToLook`, `noRailFallsBackToLook`. Run `gradlew :shuttles:test`.

## 2. Placement

- [x] 2.1 `garage/ShuttleGarageBlock.java` `getStateForPlacement` with the inputs of design.md D2. Run `gradlew build`.
- [x] 2.2 GameTests `gametest/GaragePlacementGameTests.java` placing the garage item with a mock player through a `BlockPlaceContext`: `garageClickedOnRailSideFacesIt`, `garageTurnsToOnlyRailBeside`, `railInLookDirectionWins`, `severalRailsFaceThePlayer`, `noRailFacesThePlayer`, `railShowsArmToPlacedGarage`. Run the Shuttles GameTests.

## 3. Verify

- [x] 3.1 Full `gradlew build`, `gradlew test` and the Shuttles GameTests green.
  Verified 2026-10-20: build and unit tests green, Shuttles GameTests 707 passed; the garage paragraph of `docs/src/content/docs/mods/shuttles.md` says how it turns, docs tests 50 passed.
- [x] 3.2 Play check by the user: place garages against and beside rails from several angles; the doorway meets the rail, and the carrot still turns it.
