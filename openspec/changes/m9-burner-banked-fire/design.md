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

- Coal Generator: `canWork()` is `gate.allows(full, stored, capacity, stats().restartBelow())` with `full` meaning the buffer has less room than `energy_per_tick`, saved as `RestartWaiting`. Exactly full would never be seen under any load: the push after the cycle drains the buffer before the next check, so the gate would never close, and the FE above the cap would be lost every tick.
- Pumps: `PumpLogic` owns the gate, so the Burner Pump and the Electric Pump share it at no extra cost. `full` is `tank.getSpace() < fluidPerCycle`; `stored` and `capacity` are the tank amount and `fluid_capacity`. The gate is asked every tick, also while the source is invalid, so its state follows the tank alone. `PumpLogic.save/load` keeps `RestartWaiting`. The Electric Pump spends energy only while it works, so the threshold also stops its lit state, beam and sparks from flickering with a slow drain; it saves no fuel there, only block updates.

### Lit changes only at start and stop
With the gate the machine's `tick()` result is stable across ticks, so `updateLit` writes `LIT` only when the machine starts or stops. No change to `MachineBlockEntity.updateLit` is needed.

### Status line and restart mark
A dark machine has to say why it is dark. A pure enum `MachineStatus` (`IDLE`, `RUNNING`, `WAITING`, `OUT_OF_FUEL`, `NO_POWER`) with `of(working, waiting, hasWork, starved)` decides it after each tick: working is `RUNNING`, a held off machine is `WAITING`, work without working is what the machine lacks (`OUT_OF_FUEL` for burners, `NO_POWER` for powered machines), else `IDLE`. `MachineBlockEntity` keeps the status and a `restartWaiting()` hook (false by default; the generator and the pumps return their gate's state); `BurnerMachineBlockEntity` and `PoweredMachineBlockEntity` set the status at the end of their tick.

Sync goes through the existing `ContainerData`:
- The status ordinal sits at index 6 (`DATA_STATUS`), which was free for machine specific data; the Ender Anchor already keeps its own status there and overrides it.
- The restart level (`RestartGate.level`: capacity times `restart_below`, rounded up, the same number the gate compares against) is a new entry `DATA_RESTART_LEVEL` right after the common entries. The generator syncs `DATA_COUNT_RESTART` entries; the pump's fluid entries move up by one behind it. The level is sent instead of the fraction because stats live on the server only.

`MachineScreen.Layout` gains `statusLine`. The 6 argument constructor keeps today's rule (status line exactly when there is a fluid gauge), so other screens are unchanged; the generator layout turns it on. The line takes the place of the inventory label, like the pumps' source line today, and the pumps' messages fold into it: a missing source first (red), then "Waiting: restarts below N mB" (grey, like the title), "Out of fuel" or "No power" (red), else the existing source ok line (green). The generator shows "Running" (green), "Waiting: restarts below N FE" or "Out of fuel"; nothing while idle. N is printed as a plain number like the bar tooltips.

The mark is a 1 pixel line, 3 pixels long, on both inner edges of the energy bar (generator) or fluid gauge (pumps) at the restart level, in a light grey, drawn only while 0 < level < capacity, which is the case exactly when `restart_below` is between 0 and 1. The Electric Pump's energy bar gets no mark; its threshold is on water.

Lang goes through `MachineDataParts.lang` (datagen): `gui.servus_foundation.machine.running`, `.waiting.energy`, `.waiting.fluid`, `.out_of_fuel`, `.no_power`.

### Export and docs
`GameDataJson.Machine.stats` becomes `Map<String, Number>`; the int map writer is generalised to numbers. `restart_below` is exported for every machine. The docs schema (`docs/src/lib/gamedata.ts`) is a non strict zod object and ignores the new key, so the docs tests need no change. `docs/src/content/docs/reference/datapack/machines.md` documents the field.

### NeoForge and Minecraft APIs
None new. `Codec.DOUBLE` and `optionalFieldOf` from DataFixerUpper, `ValueOutput.putBoolean` / `ValueInput.getBooleanOr` (already used by `PumpLogic.save/load`).

### Pure and Minecraft parts
- Pure, JUnit: `RestartGate` (full stops, waits above threshold, restarts below, 1.0 means no hysteresis, state round trip, `level` rounding), `MachineStats.restartFraction` clamps and the defaults of `restart_below`, `MachineStatus.of` and `byOrdinal`.
- Minecraft, GameTest: generator with a full buffer keeps its burn time and is unlit; a generator drained by a small load every tick still goes to `WAITING`; generator restarts only below 50 percent and reports `WAITING` with the restart level, then `RUNNING`, through its menu data; a generator without fuel reports `OUT_OF_FUEL`; Burner Pump with a full tank keeps its burn time and reports `WAITING` with level 2000.
- Manual play check: lamp, fire window, smoke and beam no longer flicker with a small load; the status lines and the marks read right.

### PLAN.md
This change settles no open decision of `PLAN.md`; it fixes behaviour found in play. Nothing in `PLAN.md` describes the old count down, so it needs no edit.

## Risks / Trade-offs

- [A machine can now hold a banked burn forever, so the fuel slot shows no change while the machine waits] → the GUI flame and lamp are off, which is the agreed look. Breaking the machine loses a banked burn, as it loses a running burn today.
- [Hysteresis lowers the generator's buffer on average] → only by up to half; neighbours still see the stored energy and the generator still pushes while waiting.
- [Changing the export map type could break consumers] → only the docs site reads it, with a non strict schema.

## Migration Plan

Unreleased, no migration. Old saves have no `RestartWaiting` and load as not waiting; a saved `BurnTime` continues as a banked burn.
