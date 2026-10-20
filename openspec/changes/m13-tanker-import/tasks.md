# Tasks

Paths are relative to `shuttles/src/main/java/io/github/dirnei/mc/servus/shuttles/` unless written in full; JUnit tests mirror them under `shuttles/src/test/java`. GameTests run with `gradlew :shuttles:runGameTestServer`. Needs `m13-fluid-gantry` applied first.

## 1. Pure logic

- [ ] 1.1 `gantry/FluidRouting.java` and `gantry/FluidPutAwayPlan.java` (design.md D4). JUnit `gantry/FluidRoutingTest.java`: `lockedBeforeAnyHoldingBeforeEmpty`, `fullTanksSkipped`, `incomingCountsAgainstRoom`, `otherFluidAndWrongLockSkipped`, `nearerWinsWithinRank`; `gantry/FluidPutAwayPlanTest.java`: `belowMinLoadWaits`, `fullTankGoesBelowMinLoad`, `limitedByShuttleCapacity`, `limitedByRoom`, `noRoomIsZero`. Run `gradlew :shuttles:test`.

## 2. Tanker module

- [ ] 2.1 `shuttle/module/ModuleSpec.java`, `shuttle/module/JobNeeds.java` (design.md D1), Tanker item in `registry/ServusShuttlesItems.java` and the creative tab, `shuttle/module/ShuttleTiers.java` and `stats/ShuttleStatsReader.java` with `tanker_capacity` in `src/main/resources/data/servus_shuttles/shuttles/shuttle.json` (design.md D2); recipe, lang and tooltip, item texture by job script. JUnit for the tier default in the existing tiers test. Run `gradlew :shuttles:runData`, `gradlew build`.
- [ ] 2.2 GameTests `gametest/TankerGameTests.java`: `tankerInstallsAsTool`, `tankerTakesNoItemJobs`, `toolessShuttleTakesNoTankerRun`. Run the Shuttles GameTests.

## 3. Runs

- [ ] 3.1 `shuttle/ShuttleJob.java` (`FluidJob`), `shuttle/Shuttle.java` (fluid cargo), `shuttle/ShuttleFleet.java` (assign, legs, stops with `pump_ticks`, fluid sums, `Host` methods), `network/WarehouseNetwork.java` (`tickFluids`, host methods), `warehouse.json` and `gantry.json` keys, stats manager (design.md D3, D4, D8). Run `gradlew build`.
- [ ] 3.2 GameTests: `importToStorageByTanker`, `lockedTankPreferred`, `runsOnTheWayCountAgainstRoom`, `smallAmountWaits`, `fullImportTankGoesBelowMinLoad`, `partialPickWhenDrained`, `leftoverGoesToNextTank`, `leftoverBackToSource`, `targetModeChangedReroutes`, `recallLosesCargo`, `tankerRidesTheLift`, `indexFollowsTransfers`. Run the Shuttles GameTests.

## 4. No room, lamps, state line

- [ ] 4.1 `gantry/FluidGantryBlockEntity.java` activity and state line, network sets them in `tickFluids` (design.md D5); GUI state line text in lang. Run `gradlew :shuttles:runData`, `gradlew build`.
- [ ] 4.2 GameTests: `noRoomNoRunRedLamp`, `roomAppearsRunsStart`, `workingLampWhileRunQueued`. Run the Shuttles GameTests.

## 5. Look

- [ ] 5.1 Spout and stream: static model keeps the sleeve only, `client/FluidGantryRenderer.java` nozzle and stream, pumping state in the update tag (design.md D6). List the overlapping boxes and the plane check. Run `gradlew :shuttles:runData`, `gradlew build`.
- [ ] 5.2 Tanker look: `datagen/ShuttleModels.java` `tool_tanker`, sight glass from the synced load fluid (`ShuttleSyncPayload`, shuttle renderer) (design.md D7). List the overlapping boxes and the plane check. Check every payload `TYPE` is registered. Run `gradlew :shuttles:runData`, `gradlew build`.

## 6. Plan and docs

- [ ] 6.1 `PLAN.md` M13: mark the import runs as built in the archive grooming (no edit now beyond fixing anything this change found wrong); regenerate `docs/src/data/gamedata.json` with `gradlew :alchemy:runData` and run the docs tests. Run `gradlew build`.

## 7. Verify

- [ ] 7.1 Full `gradlew build`, `gradlew test`, the Foundation, Alchemy and Shuttles GameTests green; the game launches with all three mods.
- [ ] 7.2 Play check by the user: a Tanker shuttle collects from an Import gantry fed by a pump, unloads into Storage gantries by lock, waits when there is no room (red lamp), rides a lift; spout and stream look right, no flicker.
