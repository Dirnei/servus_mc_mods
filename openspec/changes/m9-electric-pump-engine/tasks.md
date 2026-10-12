# Tasks

Paths are relative to `foundation/src/main/java/io/github/dirnei/mc/servus/foundation/`, `foundation/src/test/java/io/github/dirnei/mc/servus/foundation/` and `foundation/src/main/resources/assets/servus_foundation/` unless written in full. GameTests run with `gradlew :foundation:runGameTestServer`.

## 1. Pure parts and motion

- [x] 1.1 `machine/pump/BeamPumpParts.java` (new, the shared frame moved out of `BurnerPumpParts`), `machine/pump/BurnerPumpParts.java` (burner parts only), `machine/pump/ElectricPumpParts.java` (new, design.md D2); update all users. JUnit: move the frame cases of `machine/pump/BurnerPumpPartsTest.java` to `BeamPumpPartsTest.java`; add `ElectricPumpPartsTest.java` (parts inside their half, con rod from crank pin to the beam at `PISTON_X` at rest, rotated parts inside the block). Run `gradlew :foundation:test`.
- [ ] 1.2 `machine/pump/CrankDrive.java` (new) and `machine/pump/BeamStroke.java` (`bottoms(previous, phase)`) (design.md D3). JUnit: `CrankDriveTest.java` (pin highest at a quarter period, beam angle equals `BeamStroke.angle` at 8 phases, crank angle wraps), `BeamStrokeTest.java` (`bottomsCountsOncePerPeriod`, none while still, two across two periods). Run `gradlew :foundation:test`.

## 2. Shared two half block

- [ ] 2.1 `machine/TwoHalves.java` (new static helper with the half logic from `BurnerPumpBlock`) and `machine/pump/BeamPumpBlock.java` (new abstract base for the pump specific half parts, calling the helper) (design.md D1); `machine/pump/BurnerPumpBlock.java` extends it and keeps smoke and its shapes; move static `isUpper` and `lowerPos` callers. Run `gradlew build` and the Foundation GameTests: every existing Burner Pump test stays green.
- [ ] 2.2 Client stroke phase on both pump block entities (`machine/pump/BurnerPumpBlockEntity.java`, `ElectricPumpBlockEntity.java`, client tickers from the blocks' `getTicker`), splash particles at the socket when `BeamStroke.bottoms` is above zero; `client/BurnerPumpRenderer.java` reads the phase instead of advancing it (design.md D4). Run `gradlew build`.

## 3. Electric engine

- [ ] 3.1 `machine/pump/ElectricPumpBlock.java` extends `BeamPumpBlock` with its shapes from `ElectricPumpParts` and the spark `animateTick` (design.md D6, D7); verify `ParticleTypes.ELECTRIC_SPARK` and `SPLASH` with `javap` on `build/moddev/artifacts/minecraft-patched-26.1.2.116-merged.jar`. Run `gradlew build`.
- [ ] 3.2 Textures by script in `C:\Users\Dirnei\AppData\Local\Temp\servus-helpers\jobs\electric-pump\gen_electric_pump.py` (not in the repo): `textures/block/electric_pump_housing.png`, `electric_pump_grille.png`, `electric_pump_fan.png`, `electric_pump_terminal.png`; delete `electric_pump_front.png` and `electric_pump_front_lit.png` (design.md D5). Check 16x16 RGBA. Run `gradlew build`.
- [ ] 3.3 `datagen/ElectricPumpModels.java` (new, lower, upper, beam, pump rod, con rod, crank wheel, fan, item; blockstates by `HALF` and `FACING` with the stub parts) and `datagen/MachineDataParts.java` (no old electric model; loot like the Burner Pump, one item from the lower half). Run `gradlew :foundation:runData` and `gradlew build`.
- [ ] 3.4 `client/ElectricPumpRenderer.java` (new; crank wheel and fan rotate, con rod from pin to beam, beam rocks, pump rod follows, design.md D5) registered in `client/MachineClient.java` instead of `SideMarkerRenderer`; standalone model keys registered like the burner's. Run `gradlew build`.

## 4. GameTests

- [ ] 4.1 `machine/pump/PumpGameTests.java`: `electricPlacesTwoHalves`, `electricNoRoomAbove`, `electricBreakTopDropsOne`, `electricCreativeDropsNothing`, `electricCarrotDismantles`, `electricPistonMovesNothing`, `electricTopOpensGui` (menu type from the upper half via `useWithoutItem`), `electricTopHasNoCapabilities` (fluid and energy lookups on every face of the upper half are null), `electricPowerOnlyLowerSides` (energy capability on back and sides of the lower half, not front, bottom or top), `electricWaterOnlyLowerSides`, `electricPlacesOnWaterSurface` (via `PumpItem`); update tests that relied on the one block electric pump (outlet on top, every face but front and bottom). Run the Foundation GameTests.

## 5. Docs and plan

- [ ] 5.1 `PLAN.md` (Pumps line in section 2.1: the Electric Pump is an electric beam engine two blocks tall, water and power on the lower half's back and sides; M9 row of the milestone table: electric beam engine pump) and the pumps bullet in `docs/src/content/docs/mods/foundation.md` (electric engine, both pumps splash at the well, faces of both pumps); regenerate `docs/src/data/gamedata.json` with `gradlew :manufacturing:runData`. Run `cd docs && npm test`.

## 6. Verification

- [ ] 6.1 Full `gradlew build`, `gradlew test`, `gradlew :foundation:runGameTestServer`, `gradlew :manufacturing:runGameTestServer` and `gradlew :storage:runGameTestServer` green, the game launches with all three mods. Then the play check by the user from the main checkout: an Electric Pump placed on a pond next to a Burner Pump shows the motor with copper windings, terminal box, crank wheel, con rod, beam and fan grille; powered, the crank turns, the con rod drives the beam in step, the rod pumps, the fan spins, sparks show now and then and both pumps splash at the socket once per stroke; unpowered everything stops; pipes and power connect on the back and sides of the lower half only; breaking or dismantling either half drops one pump.
