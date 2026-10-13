# Tasks

Paths are relative to `foundation/src/main/java/io/github/dirnei/mc/servus/foundation/` and `foundation/src/main/resources/assets/servus_foundation/` unless written in full. Apply after `m9-electric-pump-engine` when both are open (design.md D1). GameTests run with `gradlew :foundation:runGameTestServer`. No pure logic is added, so there are no JUnit tests.

## 1. Shared two half helper

- [x] 1.1 `machine/TwoHalves.java` (new helper, design.md D1), unless `m9-electric-pump-engine` already added it; `machine/pump/BurnerPumpBlock.java` (or `BeamPumpBlock.java`) calls the helper instead of its own copies. Run `gradlew build` and the Foundation GameTests: every Burner Pump test stays green.

## 2. Generator halves

- [x] 2.1 `machine/generator/CoalGeneratorBlock.java`: `HALF`, placement and partner handling through `TwoHalves`, block entity only on the lower half, `LIT` mirrored on the upper half, shapes, carrot forwarding, smoke `animateTick` on the upper half (design.md D2, D5). Run `gradlew build`.
- [x] 2.2 `machine/MachineEvents.java` and `machine/EnergyNeighbors.java` (no energy toward `UP` for the generator), `machine/generator/CoalGeneratorBlockEntity.java` (top side locked Off) (design.md D3). Run `gradlew build`.

## 3. Look

- [x] 3.1 Texture script `C:\Users\Dirnei\AppData\Local\Temp\servus-helpers\jobs\coal-generator\gen_coal_generator.py` (not in the repo) drawing `textures/block/coal_generator_front.png`, `coal_generator_front_lit.png`, `coal_generator_side.png`, `coal_generator_top.png`, `coal_generator_stack.png` from the G2 sketch (`C:\Users\Dirnei\AppData\Local\Temp\claude\C--games-minecraft-servus-mc-mods\e63664e3-2e9f-4956-97ee-f355bc14ab46\scratchpad\genloader.py`, functions `g2_*`); check 16x16 RGBA. Run `gradlew build`.
- [x] 3.2 `datagen/MachineDataParts.java`: lower half orientable models with the new textures, chimney element model, blockstates by `HALF`, `FACING`, `LIT`, item model with both halves, loot from the lower half only (design.md D4). Run `gradlew :foundation:runData` and `gradlew build`.

## 4. GameTests

- [x] 4.1 `machine/generator/CoalGeneratorGameTests.java` (new, registered with the Foundation GameTests): `placesTwoHalves`, `noRoomAbove`, `breakChimneyDropsOneAndFuel`, `creativeDropsNothing`, `carrotOnChimneyDismantles`, `pistonMovesNothing`, `chimneyOpensGui`, `chimneyHasNoCapabilities`, `noEnergyUpward`, `pushesIntoNeighbourBeside`; update existing tests that place a generator with a block above it (`MachineBaseGameTests`, Manufacturing and Storage GameTests that build generators, found with `grep -rn COAL_GENERATOR`). Run the Foundation, Manufacturing and Storage GameTests.

## 5. Docs and plan

- [x] 5.1 `PLAN.md` section 2.1 (the Coal Generator is a dark steel generator two blocks tall with a chimney) and the machines list in `docs/src/content/docs/mods/foundation.md`; regenerate `docs/src/data/gamedata.json` with `gradlew :manufacturing:runData`. Run `cd docs && npm test`.

## 6. Verification

- [x] 6.1 Full `gradlew build`, `gradlew test`, `gradlew :foundation:runGameTestServer`, `gradlew :manufacturing:runGameTestServer` and `gradlew :storage:runGameTestServer` green, the game launches with all three mods. Then the play check by the user from the main checkout: a Coal Generator placed in the open shows the dark steel cube with copper behind the vents and the centred square chimney; burning coal lights the fire window and the red lamp and smoke rises from the cap; a Power Cable beside the cube takes FE, one on the chimney does not; breaking or dismantling either half drops one generator; it cannot be placed under a block. Apply status 2026-10-12: build, test (896 JUnit), Foundation GameTests (118), Manufacturing GameTests (194) and Storage GameTests (575) green in the worktree; game launch and play check pending. Play checked by the user on 2026-10-13 from the main checkout.
