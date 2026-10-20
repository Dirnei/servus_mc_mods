# Tasks

Paths are relative to `shuttles/src/main/java/io/github/dirnei/mc/servus/shuttles/` unless written in full; JUnit tests mirror them under `shuttles/src/test/java`. GameTests run with `gradlew :shuttles:runGameTestServer`. Needs `m13-capacity-upgrade` applied first.

## 1. Pure logic

- [x] 1.1 `gantry/GantryLayout.java` (design.md D1). JUnit `gantry/GantryLayoutTest.java`: `fivePartsAroundARailOnZ`, `fivePartsAroundARailOnX`, `everyPartFindsTheMaster`, `leftIsTheNegativeSide`. Run `gradlew :shuttles:test`.
- [x] 1.2 `gantry/GantryTanks.java` and `gantry/GantryCapacity.java` (design.md D3). JUnit `gantry/GantryTanksTest.java`: `lockedTankRefusesOtherFluids`, `anyTakesFirstFluidThenOnlyIt`, `fillOrderPrefersLockedThenHoldingThenEmpty`, `lockChangeKeepsContentAndWarns`; `gantry/GantryCapacityTest.java`: `capacityGrowsPerUpgrade`, `removalRefusedWhenContentWouldNotFit`, `removalAllowedAtExactFit`. Run `gradlew :shuttles:test`.
- [x] 1.3 `gantry/GantryLook.java` (fill steps, design.md D6). JUnit `gantry/GantryLookTest.java`: `emptyIsZero`, `fullIsTopStep`, `anyFluidShowsAtLeastOneStep`. Run `gradlew :shuttles:test`.

## 2. Block, item and block entity

- [x] 2.1 `gantry/FluidGantryBlock.java`, `gantry/FluidGantryItem.java`, `gantry/FluidGantryBlockEntity.java`, `gantry/GantryMode.java`, `gantry/GantryContents.java` (component), registration in `registry/ServusShuttlesBlocks.java`, `ServusShuttlesItems.java`, `ServusShuttlesBlockEntities.java`, `ServusShuttlesComponents.java`; `stats/ShuttlesStatsManager.java` and `src/main/resources/data/servus_shuttles/shuttles/gantry.json` (design.md D1, D7, D9). Placement on a straight rail, refusals with messages, remove all parts, drop with contents, place back. Temporary cube models until task 5. Run `gradlew build`.
- [x] 2.2 GameTests `gametest/FluidGantryGameTests.java`: `gantryPlacedAroundStraightRail`, `gantryRefusedOnCurve`, `gantryRefusedWithoutRoom`, `breakingAnyPartRemovesAll`, `droppedGantryKeepsTanksLocksModeUpgrades`. Run the Shuttles GameTests.

## 3. Tanks, locks, modes, buckets, upgrades

- [x] 3.1 `gantry/FluidGantryBlockEntity.java`: two `FluidSlot`s with capacity from `gantry.json` and `CapacityUpgradeSlots`, locks, mode; `ServusShuttlesEvents.java` block capability per mode with `invalidateCapabilities` on mode change; bucket use on every part (design.md D3, D4). Run `gradlew build`.
- [x] 3.2 GameTests: `storageGantryHasNoFluidHandler`, `importGantryFilledByPipeGivesNothingBack`, `exportGantryDrainedByPipeTakesNothing`, `bucketFillsStorageGantryByLock`, `bucketRefusedByLock`, `emptyBucketFillsFromGantry`, `capacityUpgradeGrowsBothTanks`, `upgradeRemovalRefusedWhenFull`, `lavaRefusedByWaterTank`. Run the Shuttles GameTests.

## 4. Warehouse node and fluid stock

- [x] 4.1 `network/WarehouseConnectionRule.java` (`NodeKind.GANTRY`), `network/WarehouseNetwork.java` (gantry map, `dockRails`, fluid index, `FluidChanged` handling, per tank views), `network/WarehouseEvent.java`, `network/StorageNodeBlockEntity.java` and `rail/RailBlock.java` (`bendsRail`, no arm toward the legs), `gantry/TankRef.java`; command `fluids` in the Shuttles command class (design.md D2, D5). Run `gradlew build`.
- [x] 4.2 GameTests: `gantryJoinsByItsRail`, `gantryTouchingControllerStaysOut`, `railShowsNoDockArmToLegs`, `storageGantriesAreFluidStock`, `importGantryIsNotStock`, `bucketChangeReachesIndexWithinASecond`, `fluidsCommandListsGantries`. Run the Shuttles GameTests.

## 5. Look

- [x] 5.1 Datagen part models and blockstates (design.md D6) in `datagen/ServusShuttlesBlockStateProvider.java` (or a new `datagen/GantryModels.java`), item model, mode and lamp textures and the Any plate by job script; `ONLINE` and `ACTIVITY` copied to the beam. List every overlapping box pair and the plane check in the task notes. Run `gradlew :shuttles:runData`, `gradlew build`.
  Flicker check (px, rail north to south). Leg, lower block: posts stand on the plinth (touching, opposite faces); the glass has no top or bottom face; glass faces (0.3, 15.7) run inside the posts and post faces (2, 14) inside the glass box; the seam band (inset 0.15, no top face) has its faces inside the posts and the glass faces inside it. Upper block: the same for band (no bottom face), posts and glass, which end at the cap (y 13); lock plates (x 5 to 11, y 3 to 9) have their inner faces at 15.725 and 0.275, 0.025 outside the glass, and touch neither posts nor band. Fluid (renderer): inset 1.5, 0.05 off plinth and cap, its faces run inside posts and band, no shared plane; the locked fluid on a plate sits 0.01 outside the plate face. Beam: flanges and web touch at y 10.5 and 14 (opposite faces); their end faces at x 0.05 and 15.95 meet only at edges, 0.05 short of the legs; signs sit 0.15 inside the flanges and 4 px off the web; feed pipes touch the bottom flange (y 9) and end on the sleeve faces (x 6 and 10); the sleeve touches the flange (y 9); the inner pipe (6.75 to 9.25) pierces the sleeve bottom with its top (4.4) inside the sleeve; the nozzle touches the inner pipe at y 3.5. No two faces share a plane where they overlap.
- [x] 5.2 `client/FluidGantryRenderer.java` (fluid columns, lock plate sprites, render bounds), update tag sync in the block entity, registration in `client/ServusShuttlesClient.java`; Foundation `client/FluidBox.java` shared with `FluidTankRenderer`. Run `gradlew build`.

## 6. GUI

- [x] 6.1 `menu/FluidGantryMenu.java`, `client/FluidGantryScreen.java`, `network/SetGantryLockPayload.java` (or the Shuttles payload package), registrations; lock picker list from source fluids with a bucket, Any, Lock to content; mode button; state line (design.md D8). Check every payload `TYPE` is registered. Run `gradlew :shuttles:runData`, `gradlew build`.
- [x] 6.2 GameTests on the menu: `menuLockFromList`, `menuLockToContent`, `menuCyclesMode`, `menuWarnsLockMismatch`. Run the Shuttles GameTests.

## 7. Recipe and lang

- [x] 7.1 Recipe, lang (block, item tooltip, GUI strings, messages), creative tab entry (design.md D9). Regenerate `docs/src/data/gamedata.json` with `gradlew :alchemy:runData` and run the docs tests. Run `gradlew build`.

## 8. Verify

- [x] 8.1 Full `gradlew build`, `gradlew test`, the Foundation, Alchemy and Shuttles GameTests green; the game launches with all three mods.
  Verified 2026-10-20: build and unit tests green; GameTests Foundation 159 and Shuttles 668 passed (field tests `harvesterTillsInsideStripOnly` and `harvestResumesBelowTheLimit` failed once each in separate runs and passed on rerun); Alchemy GameTests pass apart from the 9 known Foundation anchor tests that FTB Chunks breaks in Alchemy runs. The all mods GameTest server loads all three mods (its only failures are the anchor and site wake chunk ticket tests); the client launch is part of the play check.
- [ ] 8.2 Play check by the user: place a gantry on a rail, fill it by bucket, switch modes and pipe in and out, set locks, add upgrades, break and place it back, look for flicker on every part from several angles.
