# Tasks

Paths are relative to `shuttles/src/main/java/io/github/dirnei/mc/servus/shuttles/` unless written in full; JUnit tests mirror them under `shuttles/src/test/java`. GameTests run with `gradlew :shuttles:runGameTestServer`. Close the play client before building.

## 1. Pure logic

- [x] 1.1 `shuttle/ShuttleDrive.java` `cornerLimit` and the corner arguments of `glide` (design.md D1). JUnit in `shuttle/ShuttleDriveTest.java`: `noCornerNoLimit`, `insideCornerZoneIsCornerSpeed`, `limitRisesWithDistanceBeforeCorner`, `nearestTightestCornerWins`, `fastShuttleReachesCornerAtCornerSpeed` (ticks `next` along an L path and checks the speed in the zone), `speedsUpAfterCorner`, `glideMatchesTickByTick`. Run `gradlew :shuttles:test`.
  Named `nearestCornerWins` (all corners share one speed); added `cornerIsATurnOnOneLevel` and `glideThroughCornerMatchesTickByTick`.
- [x] 1.2 `shuttle/module/ShuttleTiers.java` (`corner`, `cornerPerCount`, `cornerSpeed`), `shuttle/module/ShuttleProfile.java` (`cornerSpeed`), `stats/ShuttleStatsReader.java` and `src/main/resources/data/servus_shuttles/shuttles/shuttle.json` (design.md D2). JUnit: `cornerSpeedGrowsALittlePerUpgrade`, `cornerSpeedNeverAboveTop`, `cornerSpeedAtLeastOneAccelStep` in the tiers test, the reader default in its test. Run `gradlew :shuttles:test`.
  Unit tests have no Gson, so the reader is checked by the GameTest `shuttleJsonHasCornerSpeed` instead.

## 2. Server and clients

- [x] 2.1 `shuttle/ShuttleFleet.java` `drive` passes the corner limit (design.md D1); `shuttle/ShuttleView.java` `Drive.corner` and codec, `client/ShuttleGarageRenderer.java` corner predicate (design.md D3). Check every payload `TYPE` is registered. Run `gradlew build`.
- [x] 2.2 GameTests in `gametest/ShuttleCornerGameTests.java`: `fastShuttleSlowsAtCorner`, `straightRunKeepsTopSpeed`, `plainShuttleBarelySlows`; read a shuttle's speed through a test accessor on the fleet if none exists. Run the Shuttles GameTests.
  The speed is read from `WarehouseNetwork.shuttleViews`, no new accessor; added `shuttleJsonHasCornerSpeed`.

## 3. Verify

- [x] 3.1 Full `gradlew build`, `gradlew test` and the Shuttles GameTests green; regenerate `docs/src/data/gamedata.json` with `gradlew :alchemy:runData` if the docs read `shuttle.json`, and run the docs tests.
  Verified 2026-10-20: build and unit tests green, Shuttles GameTests 707 passed. `gamedata.json` does not read `shuttle.json`; the Speed paragraph of `docs/src/content/docs/mods/shuttles.md` names the corner speed, docs tests 50 passed.
- [x] 3.2 Play check by the user: a Mk3 with eight Speed upgrades and a plain shuttle on a winding rail; both slow into corners, the fast one only a little faster there, no jumps in the drawn shuttle.
