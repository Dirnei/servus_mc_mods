# Proposal

## Why

The Coal Generator is a grey placeholder cube: the shared machine casing on every face and a front with a red bar and a window that turns yellow while burning. It is the first FE source of every Servus pack and stands next to the Burner Pump and the new electric pump engine, which both have a real look. It burns fuel, so it should look like it: a dark steel generator with a chimney that smokes.

## What Changes

This change implements a follow-up to PLAN.md milestone **M9 Alchemy** for the Foundation machines (section 2.1, "The Coal Generator moves to Foundation"), as a new item `m9-coal-generator-chimney`. It follows the decisions taken in `/opsx:explore` on 2026-10-12 (memory note "generator-anchor-look-decisions": look "G2" with chimney "C1" centred).

- **Dark steel generator.** The lower block stays a full cube in the dark steel frame: the front shows copper coils behind vents and a fire window low down that glows while burning, with a small red lamp; the sides are vent slats with copper behind them; the top is a vent plate.
- **A chimney makes it two blocks tall.** A straight square dark steel stack with a copper band and an overhanging cap stands centred on the top as the upper half. Smoke rises from the cap while the generator burns.
- **Two halves like the Burner Pump.** One item places both halves (placing needs room above), breaking either half removes both and drops one generator plus its fuel, the carrot dismantles the whole generator, pistons move neither half, right clicking the stack opens the generator GUI, and the stack has no capabilities, so cables and pipes never connect to it. The two half behaviour is the one the Burner Pump already has, moved into a shared helper for machines.
- **Energy and fuel stay on the cube.** The generator offers and pushes FE, and takes fuel from automation, through the faces of the lower half as today, except its top, which is the stack.
- `PLAN.md` and the docs describe the generator with its chimney.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- `foundation/coal-generator`: energy faces on the lower half only; new requirements for the two block look, placing and breaking as one, the stack forwarding to the generator, and smoke while burning.

## Non-goals

- FE rate, fuel use, recipe and GUI (unchanged; balance stays in datapack JSON).
- A moving part on the generator (decided: smoke only).
- Sounds.
- The Burner Pump and Electric Pump (their own changes); this change only shares the two half helper with them.

## Impact

- Code, Foundation: a shared two half helper in `machine/` extracted from `machine/pump/BurnerPumpBlock` (or reused when `m9-electric-pump-engine` already extracted it), `machine/generator/CoalGeneratorBlock` (halves, shapes, smoke), `machine/MachineEvents` (energy and item capability on the lower half only), datagen (`MachineDataParts`: blockstates by `HALF`, lower cube and stack models, loot from the lower half, item model), GameTests in `machine/MachineGameTests` or a new `machine/generator/CoalGeneratorGameTests`.
- Assets: new `coal_generator_front.png`, `coal_generator_front_lit.png`, `coal_generator_side.png`, `coal_generator_top.png`, `coal_generator_stack.png`.
- Order: apply after `m9-electric-pump-engine` when both are open, so the helper is extracted once.
- Docs: `docs/src/content/docs/mods/foundation.md` machines list; `PLAN.md` section 2.1.
