# Design

## Context

See proposal.md, Why. Today `BurnerMachineBlockEntity.tick()` decrements `burnTime` before it asks for work, so an idle tick still burns. `MachineBlockEntity.serverTick` writes the `LIT` property from the boolean `tick()` returns, so a machine whose `canWork()` flips each tick changes its block state each tick. The Coal Generator (`canWork = !energy.isFull()`), both pumps (`PumpLogic.canWork`: valid source and room for one `fluid_per_cycle`) and the Calciner extend these classes. The chimney of the generator copies `LIT` from the lower half; the smoke of the generator and the Burner Pump, the beam stroke of both pumps and the generator's fire window all read `LIT`. The GUI flame reads `DATA_BURN_TIME > 0`.

Machine stats come from `MachineStats.Partial.CODEC` (a `RecordCodecBuilder` with 13 optional fields, room for 16) merged over the code defaults registered in `FoundationMachines` and `AlchemyMachineDefaults`. The game data export (`GameDataExportProvider.machines`) writes every stats field into `docs/src/data/gamedata.json`.

## Goals / Non-Goals

**Goals:**
- One change in the burner base class covers every burner machine.
- The run or stop decision with hysteresis is pure Java and unit tested.

**Non-Goals:**
- No change to the shared firebox of Delicacies and Brewery, which has its own fuel code (`MasterFirebox`, `FuelFire`).
- No threshold for the Ender Anchor or the Electric Motor.

## Decisions

### Banked fire in the base class
`BurnerMachineBlockEntity.tick()` asks `canWork()` first. Without work it resets progress and returns false; `burnTime` is left alone. With work it lights a new item only when `burnTime` is 0, then decrements `burnTime` and steps progress. A burn of N ticks still gives N working ticks, as before. The eternal fire path is unchanged.

The block entity keeps a `lit` flag set from the tick's result. `isBurning()` now means "lit right now" (burning and working), and `DATA_BURN_TIME` goes to the menu as 0 while the burn is banked, so the GUI flame hides with the lamp. The banked time is kept in the `BurnTime` save field as before. Alternative considered: show the banked burn as a still flame in the GUI. Rejected because the decision was that a banked burn looks not lit everywhere, and a flame that does not move reads as a bug.

### `restart_below` as a stats field
A new `double restartBelow` in `MachineStats` and an optional `restart_below` in `Partial.CODEC` (`Codec.DOUBLE`, clamped to 0..1 in `over` through `MachineStats.restartFraction`). A second constructor without the threshold keeps the existing 13 argument calls, such as `AlchemyMachineDefaults`, as they are. The factory methods give 1.0 (restart as soon as there is room) to every machine, except `generator`, `burnerPump` and `poweredPump`, which take 0.5. The shipped `coal_generator.json`, `burner_pump.json` and `electric_pump.json` state `"restart_below": 0.5`. Alternatives: an int percent (`restart_below_percent: 50`) would keep the export map all integers, but the fraction was the agreed name and reads better in a datapack; the export map becomes `Map<String, Number>`.

### Pure `RestartGate`
`io.github.dirnei.mc.servus.foundation.machine.RestartGate`, no Minecraft imports:
`boolean allows(boolean full, long stored, long capacity, double restartBelow)` returns false and starts waiting when `full`; while waiting it returns false until `stored < capacity * restartBelow`, then stops waiting and returns true. `waiting()` and `setWaiting(boolean)` let the owner save the state. With `restartBelow` 1.0 it returns true whenever the buffer is not full, which is today's behaviour.

- Coal Generator: `canWork()` is `gate.allows(energy.isFull(), stored, capacity, stats().restartBelow())`, saved as `RestartWaiting`.
- Pumps: `PumpLogic` owns the gate, so the Burner Pump and the Electric Pump share it at no extra cost. `full` is `tank.getSpace() < fluidPerCycle`; `stored` and `capacity` are the tank amount and `fluid_capacity`. The gate is asked every tick, also while the source is invalid, so its state follows the tank alone. `PumpLogic.save/load` keeps `RestartWaiting`. The Electric Pump spends energy only while it works, so the threshold also stops its lit state, beam and sparks from flickering with a slow drain; it saves no fuel there, only block updates.

### Lit changes only at start and stop
With the gate the machine's `tick()` result is stable across ticks, so `updateLit` writes `LIT` only when the machine starts or stops. No change to `MachineBlockEntity.updateLit` is needed.

### Export and docs
`GameDataJson.Machine.stats` becomes `Map<String, Number>`; the int map writer is generalised to numbers. `restart_below` is exported for every machine. The docs schema (`docs/src/lib/gamedata.ts`) is a non strict zod object and ignores the new key, so the docs tests need no change. `docs/src/content/docs/reference/datapack/machines.md` documents the field.

### NeoForge and Minecraft APIs
None new. `Codec.DOUBLE` and `optionalFieldOf` from DataFixerUpper, `ValueOutput.putBoolean` / `ValueInput.getBooleanOr` (already used by `PumpLogic.save/load`).

### Pure and Minecraft parts
- Pure, JUnit: `RestartGate` (full stops, waits above threshold, restarts below, 1.0 means no hysteresis, 0 never restarts before empty, state round trip), `MachineStats.Partial.over` clamps and defaults `restart_below`.
- Minecraft, GameTest: generator with a full buffer keeps its burn time and is unlit; generator restarts only below 50 percent; Burner Pump with a full tank keeps its burn time.
- Manual play check: lamp, fire window, smoke and beam no longer flicker with a small load.

### PLAN.md
This change settles no open decision of `PLAN.md`; it fixes behaviour found in play. Nothing in `PLAN.md` describes the old count down, so it needs no edit.

## Risks / Trade-offs

- [A machine can now hold a banked burn forever, so the fuel slot shows no change while the machine waits] → the GUI flame and lamp are off, which is the agreed look. Breaking the machine loses a banked burn, as it loses a running burn today.
- [Hysteresis lowers the generator's buffer on average] → only by up to half; neighbours still see the stored energy and the generator still pushes while waiting.
- [Changing the export map type could break consumers] → only the docs site reads it, with a non strict schema.

## Migration Plan

Unreleased, no migration. Old saves have no `RestartWaiting` and load as not waiting; a saved `BurnTime` continues as a banked burn.
