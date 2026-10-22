# Proposal

## Why

Every burner machine counts its burn down on every tick, also on ticks it has no work, and its lit state follows the work of the single tick. A Coal Generator with a small load (for example 5 FE/t on its 16,000 FE buffer at 40 FE/t) flips between full and not full on every tick: the lamp, the fire window and the smoke flicker, the block state changes every tick, and one coal burns its whole time while the generator produces on only about one tick in eight. A Burner Pump with a full tank wastes its burn the same way, and a Calciner without input burns its fuel to nothing.

This is not an item of `PLAN.md`; it came up while play testing M9 (the Coal Generator and the pumps). It belongs to M9 because the burner machines and pumps were built there.

## What Changes

- **Banked fire.** A burner machine (Coal Generator, Burner Pump, Calciner) keeps its burn while it has no work: the remaining burn time does not count down, so no fuel is wasted. A banked burn shows as not lit: no lamp, no fire window, no smoke, no flame in the GUI. When work comes back the banked burn goes on before a new fuel item is taken. The eternal fire of the Calciner stays as it is.
- **Restart threshold.** The Coal Generator, the Burner Pump and the Electric Pump stop when their buffer is full (generator: FE buffer full; pumps: the tank cannot take another cycle) and stay off until the buffer drops below a fraction of its capacity, then run until it is full again. The fraction is the new machine stats field `restart_below`, `0.5` in the shipped `coal_generator.json`, `burner_pump.json` and `electric_pump.json`. Machines without the field restart as soon as there is room (`1.0`).
- The lit state of these machines then changes only when they start or stop, not on every tick.
- **Why it is dark.** The Coal Generator, Burner Pump and Electric Pump screens get a status line: "Waiting: restarts below N FE" (or mB) while the threshold holds them off, "Out of fuel" or "No power" when they have room but cannot run, and the pumps' existing water source line otherwise. The energy bar (generator) and the fluid gauge (pumps) show a small mark at the restart level.
- The run or stop decision is a small Minecraft free class with JUnit tests.
- The game data export carries `restart_below`, and the datapack reference page documents it.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/machines`: burner machines bank their fire while idle; a banked fire is not lit; machine stats gain `restart_below`.
- `foundation/coal-generator`: a full buffer pauses the burn instead of letting it count down; the generator restarts only below its restart threshold; its GUI gets the status line and the restart mark.
- `foundation/pumps`: a pump with a full buffer pauses its burn (Burner Pump) and restarts only below its restart threshold (both pumps); the smoke and beam scenario for a full buffer stops right away; the pump GUI status line and the restart mark.
- `alchemy/calciner`: a Calciner that cannot work keeps its burn instead of burning it down.

## Non-goals

- The Delicacies and Brewery firebox (`MasterFirebox`) has its own fuel logic and is not changed here.
- No redstone control or GUI setting for the threshold; it is datapack data only.
- Powered machines other than the Electric Pump (Ender Anchor, Electric Motor) get no threshold.
- No new look for a banked fire (for example glowing embers); it simply shows as not lit.

## Impact

- Code: `foundation/.../machine/BurnerMachineBlockEntity`, `MachineStats`, `generator/CoalGeneratorBlockEntity`, `pump/PumpLogic`, two new pure classes in `foundation/.../machine` (`RestartGate`, `MachineStatus`), `MachineBlockEntity` and `PoweredMachineBlockEntity` (status and menu data), `menu/MachineMenu`, `client/MachineScreen` and `MachineClient`, the Foundation lang datagen, the alchemy game data export (`GameDataExportProvider`, `GameDataJson`).
- Data: `coal_generator.json`, `burner_pump.json`, `electric_pump.json` in Foundation; `docs/src/data/gamedata.json` regenerated.
- Docs: `docs/src/content/docs/reference/datapack/machines.md`.
- Tests: JUnit for the threshold class and the stats default; GameTests for the generator and the Burner Pump.
- Saves: a banked burn and the waiting state are saved; old worlds load with no wait.
