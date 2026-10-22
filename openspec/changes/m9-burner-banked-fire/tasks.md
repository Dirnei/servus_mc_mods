# Tasks

## 1. Restart threshold logic and data

- [x] 1.1 Add the pure `RestartGate` in `foundation/src/main/java/io/github/dirnei/mc/servus/foundation/machine/RestartGate.java` and `RestartGateTest` in `foundation/src/test/java/.../machine/` with tests: full stops and starts waiting; waits while at or above the threshold; restarts below it and runs until full; `restartBelow` 1.0 runs whenever not full; waiting state set and read back. Verify with `gradlew :foundation:test`.
- [x] 1.2 Add `restartBelow` (double) to `MachineStats` and `restart_below` to `MachineStats.Partial.CODEC` (clamped to 0..1); factories give 1.0, except `generator`, `burnerPump` and `poweredPump` with 0.5; a 13 argument constructor without the threshold keeps `AlchemyMachineDefaults` unchanged; add JUnit cases in `FoundationMachinesTest` for the default and the clamp (`MachineStats.restartFraction`). Add `"restart_below": 0.5` to `coal_generator.json`, `burner_pump.json`, `electric_pump.json`. Verify with `gradlew :foundation:test :alchemy:compileJava`.

## 2. Banked fire and gated machines

- [x] 2.1 Bank the fire in `BurnerMachineBlockEntity.tick()`: no count down without work, a `lit` flag behind `isBurning()`, `DATA_BURN_TIME` 0 to the menu while banked, plus a `burnTime()` accessor for tests. Verify with `gradlew build`.
- [x] 2.2 Gate the Coal Generator (`generator/CoalGeneratorBlockEntity`): `canWork()` through a `RestartGate`, saved as `RestartWaiting`. Verify with `gradlew build`.
- [x] 2.3 Gate both pumps in `pump/PumpLogic.canWork` with one `RestartGate`, saved in `PumpLogic.save/load`. Verify with `gradlew build`.

## 3. GameTests

- [x] 3.1 In `generator/CoalGeneratorGameTests`: a generator with a full buffer keeps its burn time and is unlit; a generator that filled up stays off at 60 percent and runs again below 50 percent. In `pump/PumpGameTests`: a Burner Pump with a full tank keeps its burn time and is unlit. Adjust any test that assumed the old count down. Verify with `gradlew :foundation:runGameTestServer` and `gradlew :alchemy:runGameTestServer`.

## 4. Export and docs

- [x] 4.1 Export `restart_below` for every machine: `GameDataJson.Machine.stats` as `Map<String, Number>`, `GameDataExportProvider.machines`. Run `gradlew :alchemy:runData` and keep the regenerated `docs/src/data/gamedata.json`. Run the docs tests (`npx vitest run` in `docs/`).
- [x] 4.2 Document `restart_below` and the banked fire in `docs/src/content/docs/reference/datapack/machines.md` and the Coal Generator and pump lines of `docs/src/content/docs/mods/foundation.md`. Verify the page by reading it back.

## 5. Why a dark machine is dark

- [x] 5.1 Add the pure `MachineStatus` enum (`foundation/.../machine/MachineStatus.java`) with `MachineStatusTest` (working runs, waiting beats missing fuel, work without working is what is missing, no work is idle, ordinal round trip), and `RestartGate.level` with tests for its rounding and that the gate restarts exactly below it. Verify with `gradlew :foundation:test`.
- [x] 5.2 Set the status at the end of the tick in `BurnerMachineBlockEntity` and `PoweredMachineBlockEntity` (`restartWaiting()` hook in `MachineBlockEntity`, overridden by `CoalGeneratorBlockEntity`, `BurnerPumpBlockEntity`, `ElectricPumpBlockEntity` through `PumpLogic`); sync it at `DATA_STATUS` (index 6) and the restart level at the new `DATA_RESTART_LEVEL`, with the pump fluid entries moved up behind it; `MachineMenu.machineStatus()` and `restartLevel()`; `CoalGeneratorMenu` syncs `DATA_COUNT_RESTART`. Verify with `gradlew build`.
- [x] 5.3 Status line and restart mark in `client/MachineScreen` (new `Layout.statusLine`, on for the generator in `MachineClient`, kept equal to `fluid` for the 6 argument constructor), lang keys in `datagen/MachineDataParts.lang`; run `gradlew :foundation:runData` and `gradlew :alchemy:runData` (no change to `docs/src/data/gamedata.json`). Verify with `gradlew build`.
- [x] 5.4 GameTests: `CoalGeneratorGameTests.restartsOnlyBelowHalf` checks `WAITING` and the restart level in the menu data, then `RUNNING`; new `CoalGeneratorGameTests.reportsOutOfFuel`; `PumpGameTests.fullTankKeepsTheBurn` checks `WAITING` and level 2000. Verify with `gradlew :foundation:runGameTestServer` and `gradlew :alchemy:runGameTestServer`.

## 6. Verification

- [x] 6.1 Full `gradlew build` and `gradlew test` green, `:foundation:runGameTestServer` and `:alchemy:runGameTestServer` green (or failures shown to be pre-existing on main), `openspec validate m9-burner-banked-fire` passes.
- [ ] 6.2 User play check from main: game launches with all mods; a Coal Generator feeding a small load (for example an Electric Pump with a full tank, or a single Power Cable consumer) no longer flickers its lamp, fire window or smoke, stays dark while its buffer is above half and runs lit until full; a Burner Pump with a full tank goes dark at once and keeps its burn; a Calciner whose input runs out goes dark and keeps its burn. In the GUIs: the generator reads "Running", "Waiting: restarts below 8000 FE" while it waits and "Out of fuel" without coal; the pumps read "Waiting: restarts below 2000 mB" while they wait, "Out of fuel" (Burner Pump) or "No power" (Electric Pump), and the source lines as before; the energy bar and the fluid gauge show a small, subtle mark at half height.

## Workflow follow-up

- Archive after the play check (`/opsx:archive`).
