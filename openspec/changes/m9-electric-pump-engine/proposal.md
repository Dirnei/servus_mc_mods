# Proposal

## Why

The Electric Pump is a plain grey box with a blue front: no motion and no character, standing next to the Burner Pump, which became an animated two block beam engine in `m9-burner-pump-steam-engine`. The electric pump is the upgrade of the burner pump (it is crafted from one), so it should look like its advanced sibling: the same family, moving the same way, without steam.

## What Changes

This change implements a follow-up to PLAN.md milestone **M9 Alchemy**, item **beam engine Burner Pump** (section 2.1 Pumps, line "Pumps are the first fluid source"), as a new item `m9-electric-pump-engine`. It follows the decisions taken in `/opsx:explore` on 2026-10-12 from an animated concept page (memory note "electric-pump-look-decisions", concept A).

- **Two block electric beam engine.** The Electric Pump becomes one block wide and two blocks tall like the Burner Pump, with the same base plate, post, beam, pump rod, socket and stroke. The lower half is a motor housing with copper windings and a terminal box with a red power accent instead of the firebox and boiler; the upper half carries a crank wheel that drives the beam through a con rod instead of the steam cylinder, and a fan grille with a fan where the chimney was. The compact one block body goes.
- **Places, breaks and dismantles as one** like the Burner Pump: one item places both halves, breaking either half removes both and drops one Electric Pump, the carrot dismantles the whole pump, pistons move neither half, and the upper half forwards its right click to the engine and offers nothing to pipes.
- **Faces like the Burner Pump.** Water leaves, and FE comes in, on the back and the two sides of the lower half only; the front, the bottom and the whole upper half are closed. The outlet on top goes.
- **Moves without steam.** While working, the crank wheel turns, the con rod drives the beam, the beam rocks, the pump rod rises and falls, the fan spins, and now and then a small spark jumps at the motor. Idle, everything stands still.
- **Splash at the well, both pumps.** On each stroke, when the pump rod reaches its lowest point, a small water splash rises at the socket, for the Electric Pump and the Burner Pump.
- **Materials.** Vanilla block textures where possible (iron, copper, a dark stone base), own textures only for the motor housing with vents, the fan grille and the terminal box.
- `PLAN.md` names the electric beam engine in the Pumps line and the M9 row; the docs pumps bullet describes it.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- `foundation/pumps`: the Electric Pump becomes a two block engine (shape, placing and breaking as one, upper half closed, faces on the lower half), its working motion and sparks, and the splash at the well for both pumps; the compact body requirement is removed.

## Non-goals

- Pump rate, energy use, recipe and GUI (unchanged; balance stays in datapack JSON and the open M9 balance pass).
- Sounds, such as a motor hum (not chosen).
- A pulsing or lit window on the motor (decided against).
- A faster stroke for the electric pump; the stroke is look only like the burner's.
- Changing the Burner Pump beyond the splash.

## Impact

- Code, Foundation: `machine/pump/` (shared two half logic out of `BurnerPumpBlock` into a common base for both pumps, `ElectricPumpBlock`, `ElectricPumpBlockEntity` faces, new `ElectricPumpParts`, pure crank and stroke helpers next to `BeamStroke`), `client/` (new `ElectricPumpRenderer`, splash in both pumps' client tick or renderer state), `datagen/` (electric pump models like `BurnerPumpModels`, blockstates with `HALF`, item model, loot), `PumpItem` placement for two blocks, GameTests in `machine/pump/PumpGameTests`, JUnit tests next to `BurnerPumpPartsTest`.
- Assets: new textures for the motor housing, fan grille, fan and terminal box; the old `electric_pump_front*.png` go.
- Docs: the pumps bullet in `docs/src/content/docs/mods/foundation.md`; `PLAN.md` (the Pumps line and the M9 row of the milestone table).
