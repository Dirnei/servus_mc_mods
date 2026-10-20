# Tasks

Paths are relative to `shuttles/src/main/java/io/github/dirnei/mc/servus/shuttles/` unless written in full; JUnit tests mirror them under `shuttles/src/test/java`. GameTests run with `gradlew :shuttles:runGameTestServer`. Needs `m13-fluid-gantry` applied first.

## 1. Pure logic

- [x] 1.1 `gantry/FluidRouting.java` and `gantry/FluidPutAwayPlan.java` (design.md D4). JUnit `gantry/FluidRoutingTest.java`: `lockedBeforeAnyHoldingBeforeEmpty`, `fullTanksSkipped`, `incomingCountsAgainstRoom`, `otherFluidAndWrongLockSkipped`, `nearerWinsWithinRank`; `gantry/FluidPutAwayPlanTest.java`: `belowMinLoadWaits`, `fullTankGoesBelowMinLoad`, `limitedByShuttleCapacity`, `limitedByRoom`, `noRoomIsZero`. Run `gradlew :shuttles:test`.

## 2. Tanker module

- [x] 2.1 `shuttle/module/ModuleSpec.java`, `shuttle/module/JobNeeds.java` (design.md D1), Tanker item in `registry/ServusShuttlesItems.java` and the creative tab, `shuttle/module/ShuttleTiers.java` and `stats/ShuttleStatsReader.java` with `tanker_capacity` in `src/main/resources/data/servus_shuttles/shuttles/shuttle.json` (design.md D2); recipe, lang and tooltip, item texture by job script. JUnit for the tier default in the existing tiers test. Run `gradlew :shuttles:runData`, `gradlew build`.
- [x] 2.2 GameTests `gametest/TankerGameTests.java`: `tankerInstallsAsTool`, `tankerTakesNoItemJobs`, `toolessShuttleTakesNoTankerRun`. Run the Shuttles GameTests.

## 3. Runs

- [x] 3.1 `shuttle/ShuttleJob.java` (`FluidJob`), `shuttle/Shuttle.java` (fluid cargo), `shuttle/ShuttleFleet.java` (assign, legs, stops with `pump_ticks`, fluid sums, `Host` methods), `network/WarehouseNetwork.java` (`tickFluids`, host methods), `warehouse.json` and `gantry.json` keys, stats manager (design.md D3, D4, D8). Run `gradlew build`.
- [x] 3.2 GameTests (in `gametest/TankerRunGameTests.java`): `importToStorageByTanker`, `lockedTankPreferred`, `runsOnTheWayCountAgainstRoom`, `smallAmountWaits`, `fullImportTankGoesBelowMinLoad`, `partialPickWhenDrained`, `leftoverGoesToNextTank`, `leftoverBackToSource`, `targetModeChangedReroutes`, `recallLosesCargo`, `tankerRidesTheLift`, `indexFollowsTransfers`. Run the Shuttles GameTests.

## 4. No room, lamps, state line

- [x] 4.1 `gantry/FluidGantryBlockEntity.java` activity and state line, network sets them in `tickFluids` (design.md D5); GUI state line text in lang. Run `gradlew :shuttles:runData`, `gradlew build`.
- [x] 4.2 GameTests (in `gametest/TankerLampGameTests.java`): `noRoomNoRunRedLamp`, `roomAppearsRunsStart`, `workingLampWhileRunQueued`. Run the Shuttles GameTests.

## 5. Look

- [x] 5.1 Spout and stream: static model keeps the sleeve only, `client/FluidGantryRenderer.java` nozzle and stream, pumping state in the update tag (design.md D6). List the overlapping boxes and the plane check. Run `gradlew :shuttles:runData`, `gradlew build`.
  Flicker check (px of the beam block, rail north to south; the nozzle lowers by `GantryLook.spoutDrop()`, 1.66 px, onto a cap whose top is block px 15.84 of the rail block). Inner pipe (6.75 to 9.25, top 4.4) and sleeve (6 to 10, y 4 to 9): the pipe pierces the sleeve bottom, no face on a sleeve plane, its top inside the sleeve at every height. Pipe and nozzle (6.4 to 9.6, 1 tall): the pipe has no bottom face and ends on the nozzle top, so no two faces share that plane. Nozzle and sleeve: the nozzle stays below y 4 at rest (top 3.5) and goes lower. Stream (7.25 to 8.75, sides only) from the cap top to the nozzle bottom: no top or bottom face, its sides inside the nozzle and cap outlines, touching them only at edges. Pipe and feed pipes: the feeds end on the sleeve faces at x 6 and 10, outside the pipe. No two faces share a plane where they overlap, at rest, while lowering and lowered. JUnit `GantryLookTest.loweredNozzleStaysInTheBeamBlockAboveTheCap`.
- [x] 5.2 Tanker look: `datagen/ShuttleModels.java` `tool_tanker`, sight glass from the synced load fluid (`ShuttleSyncPayload`, shuttle renderer) (design.md D7). List the overlapping boxes and the plane check. Check every payload `TYPE` is registered. Run `gradlew :shuttles:runData`, `gradlew build`.
  Flicker check (shuttle model px, drawn at 0.8 around the block centre). Tank: core 4.5 to 11.5 / y 11.25 to 14.75 / z 2.6 to 13.4, mid 5 to 11 / 10.25 to 15.75 / 2.65 to 13.35, tall 6 to 10 / 9.5 to 16.5 / 2.7 to 13.3: every pair differs on all six planes. Bands (z 4 and 11, 1 deep) per tank box 0.2 proud, z staggered 0, 0.05, 0.1 like the tank: band against band, band against every tank box, no shared plane. Saddles 5.25 to 10.75 / 4.05 to 10.3 / z 5.5 to 6.5 and 9.5 to 10.5: inside the tall and mid boxes at the top, off the chassis top (4) by 0.05, off the mid sides (5, 11) and the Handling hook (z 9 to 10). Cap 7 to 9 / 16.3 to 17.3 sinks into the tall box only. Sight glass frames 4.2 to 4.6 and 11.4 to 11.8 / 11.75 to 14.25 / z 6 to 10 straddle the core sides (4.5, 11.5), clear of bands, saddles and the mid box; the renderer's fluid quads sit 0.02 outside the frames. Against the other parts: Mk1 crate walls end at y 9 and lie at x 2 to 3, 13 to 14, z 2 to 3, 13 to 14, all clear; Mk2 and Mk3 hold (y 4 to 10) and lid (10 to 11) on planes 2, 14, 4, 10, 11 that no tanker box uses; Mk3 gold corners end at x 4, short of the bands (4.3); the Handling crane beam (7 to 9 / 12 to 13 / 1 to 10) and hook cross the tank on planes of their own; the masts (x 3 to 4, 12 to 13, z 1 to 2) and the spoiler wing (z up to 2.5) end before the tank (x 4.3, z 2.6); the spoiler struts are turned, so they share no plane. Steel on every tank face, blackened iron on bands, saddles and frames, a copper cap. Everything within x 4.18 to 11.82 (block 4.9 to 11.1, inside the body's 3.2 to 12.8) and below model y 17.3 (block 15.84). Every payload `TYPE` is registered in `ServusShuttlesPayloads`; `ShuttleView` carries the sight glass, no new payload.

## 6. Plan and docs

- [x] 6.1 `PLAN.md` M13: mark the import runs as built in the archive grooming (no edit now beyond fixing anything this change found wrong); regenerate `docs/src/data/gamedata.json` with `gradlew :alchemy:runData` and run the docs tests. Run `gradlew build`.
  PLAN.md needed no fix; gamedata.json gained the Tanker recipe and name; docs tests 50 passed.

## 7. Verify

- [x] 7.1 Full `gradlew build`, `gradlew test`, the Foundation, Alchemy and Shuttles GameTests green; the game launches with all three mods.
  Verified 2026-10-20: build and unit tests green; GameTests Foundation 159 and Shuttles 686 passed; Alchemy GameTests pass apart from the 9 known Foundation anchor tests that FTB Chunks breaks in Alchemy runs. The client launch is part of the play check.
- [ ] 7.2 Play check by the user: a Tanker shuttle collects from an Import gantry fed by a pump, unloads into Storage gantries by lock, waits when there is no room (red lamp), rides a lift; spout and stream look right, no flicker.
