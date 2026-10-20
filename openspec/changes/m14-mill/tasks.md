# Tasks

## 1. Shared helpers and drive logic

- [x] 1.1 Move the box rotation helpers: `machine/PixelBoxes` (pure: `rotated`, `rotatedPoint`) and `machine/PartShapes` (`quarters`, `box`, `shapeOf`) from `BeamPumpParts` and `BeamPumpBlock`; update the pump blocks, pump model datagen and tests; JUnit `PixelBoxesTest` takes over the rotation cases of `BeamPumpPartsTest` (D8). Verify: `gradlew :foundation:build` green.
- [x] 1.2 Pure drive logic in `foundation/drive`: `Drivable`, `WorkProgress`, `MotorRate`, `CrankTurns`, `SpinClock` (D1, D2); JUnit `DriveLogicTest` with nested `WorkProgressTest` (reaches the goal, drops the rest, keeps progress below the goal, reset), `MotorRateTest` (base rate, Speed Upgrade doubles both, FE per work constant, at least 1 work), `CrankTurnsTest` (four pushes make a turn, counting starts over), `SpinClockTest` (runs while running, stands while not). Verify: `gradlew :foundation:test` green.
- [x] 1.3 `drive/DriveStats` with `Partial` codec and `DriveStatsManager` registered in `MachineEvents`; `ServusFoundationCapabilities.DRIVE`; data `drive/hand_crank.json`, `drive/electric_motor.json` (D3); JUnit `DriveStatsTest` (partial over defaults, missing fields keep defaults). Verify: `gradlew :foundation:build` green.

## 2. Mill

- [x] 2.1 `machine/mill/MillingRecipe`, recipe type and serializer in `MachineContent`, client sync in `ServusFoundation` (D4); `MillParts`, `MillBlock`, `MillBlockEntity`, `MillMenu` (D5); registrations in `MachineContent` (block, item, block entity, menu, creative tab order); item and drive capabilities in `MachineEvents`; `MachineClient.MILL` screen. Verify: `gradlew :foundation:build` green.

## 3. Drivers

- [x] 3.1 `drive/HandCrankParts`, `HandCrankBlock`, `HandCrankBlockEntity` (D6); registrations in `MachineContent`. Verify: `gradlew :foundation:build` green.
- [x] 3.2 `drive/ElectricMotorParts`, `ElectricMotorBlock`, `ElectricMotorBlockEntity`, `ElectricMotorMenu` (D7); machine defaults in `FoundationMachines` and `machine/electric_motor.json`; energy capability in `MachineEvents`; `MachineClient.ELECTRIC_MOTOR` screen. Verify: `gradlew :foundation:build` green.

## 4. Look

- [x] 4.1 `tools/mill-textures/gen.py` writes the block and GUI textures (D9); run it, commit the PNGs. Verify: `python tools/mill-textures/gen.py` runs clean, `gradlew :foundation:build` green.
- [x] 4.2 Datagen models: `MillModels`, `HandCrankModels`, `ElectricMotorModels` (block models, block states, standalone turning parts, item models) wired into `MachineDataParts.blockStates` (D8); loot `dropSelf` and pickaxe tag through `MachineDataParts.selfDropping`; lang names. Overlap check from the design table against the final boxes. Verify: `:foundation:runData`, generated files committed, `gradlew :foundation:build` green.
- [x] 4.3 Renderers `MillRenderer`, `HandCrankRenderer`, `ElectricMotorRenderer`, standalone model keys, registration in `MachineClient`; motor sparks (D7, D8). Verify: `gradlew :foundation:build` green.

## 5. Recipes and JEI

- [x] 5.1 Crafting recipes for Mill, Hand Crank and Electric Motor and the milling recipe `milling/sand` in `MachineDataParts.recipes` (D4, D10); `:foundation:runData`. Verify: `gradlew :foundation:build` green.
- [x] 5.2 JEI `MillingRecipeCategory` and catalyst in `ServusFoundationJeiPlugin`, lang for the category (D11); `:foundation:runData`. Verify: `gradlew :foundation:build` green.

## 6. Behaviour tests

- [x] 6.1 GameTests in new `MillGameTests` (registered in `ServusFoundation`): `motorGrindsGravelToSand`, `millWithoutDriveDoesNothing`, `motorWithoutPowerDoesNothing`, `idleMotorKeepsItsEnergy`, `speedUpgradeDoublesWorkAndEnergy`, `motorTakesEnergyOnlyFromTheBack`, `driveOnlyOnBackAndSides`, `crankTurnsGrindAndTire`, `crankNeedsADrivenBlock`, `crankDropsWhenTheMillGoes`, `hopperAboveChestInFront`, `chuteRefusesInput`, `millRefusesOtherItems`, `millingRecipeShipped`. Verify: `:foundation:runGameTestServer` green.

## 7. Docs

- [x] 7.1 Docs: Mill, Hand Crank and Electric Motor on `docs/src/content/docs/mods/foundation.md`; the milling recipe in `reference/datapack/recipes.md`; the motor row and the drive files in `reference/datapack/machines.md`; `:alchemy:runData` refreshes `docs/src/data/gamedata.json`. Verify: no em dash in the diff, `gradlew build` green.

## 8. Continuous Hand Crank

- [x] 8.1 Pure `drive/Flywheel` replaces `SpinClock` (spin-up, constant speed, linear coast, interpolated angle); `CrankTurns` becomes the hold and tick counter (`click`, `driving`, `held`, `step` with the work share) (D2); JUnit in `DriveLogicTest`: `FlywheelTest` (spins up, constant speed, coasts to a stop within the coast ticks, interpolates between ticks, stands when stopped), `CrankTurnsTest` (shares add up to the turn's work, turn done every 16 steps, a click drives until the next is due and holds longer, held clicks drive every tick). Verify: `gradlew :foundation:test` green.
- [x] 8.2 `Drivable.turn()`; `HandCrankBlock.TURNING` and a server ticker; `HandCrankBlockEntity` drives per tick and sets `TURNING` on start and stop only; `MillBlockEntity` turning window 5 ticks; Mill, crank and motor block entities and renderers on `Flywheel` (D1, D5, D6, D8); `:foundation:runData` for the crank block state. Verify: `gradlew :foundation:build` green.
- [x] 8.3 GameTests in `MillGameTests`: `crankTurnsGrindAndTire` holds the crank by clicking every 4 ticks; new `crankStopsWhenLetGo` (TURNING on while held, off within 8 ticks after the last click, progress no longer grows) and `crankTurnsAnEmptyMill` (crank and Mill turn, no exhaustion). Verify: `:foundation:runGameTestServer` green.
- [x] 8.4 Docs: the Hand Crank paragraph on `docs/src/content/docs/mods/foundation.md` (continuous turn, coasting). Verify: no em dash in the diff, `gradlew build` green.

## 9. Verification

- [x] 9.1 Full `gradlew build test`, `:foundation:runGameTestServer` green.
- [ ] 9.2 User play check in game:
  - The Mill looks like the family steel mockup: heavy iron base, steel housing, iron grinding wheel with brass hub, steel hopper with a brass rim, steel chute; no flicker from any angle, also while the wheel turns. The item shows the whole mill.
  - The Hand Crank goes only onto the back or sides of a Mill; holding right click turns the arm and the Mill's wheel smoothly and continuously, Gravel becomes Sand in about six seconds, the hunger bar drops slowly; let go, arm and wheel slow down and stop in about a second and the progress arrow stops; breaking the Mill drops the crank.
  - The Electric Motor looks like the Electric Pump's motor; powered from the back it grinds Gravel to Sand in about two seconds, the coupling and fan turn and sparks show; it stops and spends nothing when the Mill is empty; a Speed Upgrade halves the time and doubles the FE per tick.
  - A hopper above and a chest in front work; the carrot cycles the Mill's sides and dismantles all three blocks; JEI shows the Milling category.
  - Look at the crank and the motor from several angles, also while they turn: no flicker.
