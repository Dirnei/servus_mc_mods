# Design

## Context

See proposal.md (Why). Current state on `main` (Minecraft 26.1.2, NeoForge 26.1.2.116), Foundation:

- `machine/generator/CoalGeneratorBlock extends MachineBlock` (`FACING`, `LIT`, side config through the carrot, `Wrenchable`), with `CoalGeneratorBlockEntity` (a `BurnerMachineBlockEntity`). `MachineBlockEntity` sets `LIT` while working.
- `machine/MachineEvents` registers for the generator `Capabilities.Item.BLOCK` (`sideHandler(side)`), `Capabilities.Energy.BLOCK` (`energyStorage()` on every face) and `UPKEEP`.
- `datagen/MachineDataParts.machine(...)` builds `orientable` models from `<name>_front`, `<name>_front_lit`, `machine_side`, `machine_top`.
- The two half behaviour lives in `machine/pump/BurnerPumpBlock` (`HALF`, placement with room above, `setPlacedBy` puts the upper half, `updateShape` and `neighborChanged` keep the halves together, `playerWillDestroy` for creative and wrong tool, `useWithoutItem` and `onWrench` forwarding, block entity only on the lower half) and is covered by the Burner Pump GameTests in `machine/pump/PumpGameTests`. `m9-electric-pump-engine` (open) plans to move it into a `BeamPumpBlock` base for both pumps.
- Smoke: `BurnerPumpBlock.animateTick` adds `ParticleTypes.CAMPFIRE_COSY_SMOKE` at the chimney top while lit.

## Goals / Non-Goals

**Goals:**
- The generator gets the Burner Pump's two half behaviour without a third copy of it.
- New textures and a chimney model; smoke while lit.

**Non-Goals:**
- Balance, GUI, recipe, moving parts, sounds.

## Decisions

### D1. A shared two half helper for machines, not a base class

The Burner Pump, the Electric Pump and the Coal Generator extend different classes (`PumpBlock` for the pumps, `MachineBlock` for the generator), so one abstract base cannot serve all three. The two half logic becomes a helper `machine/TwoHalves` (static methods plus the `HALF` property constant) that a block calls from its own overrides: `canPlace(context)`, `placeUpper(level, pos, state, upperOf)`, `keepTogether(state, direction, neighbour)` for `updateShape`, `breakPartner(level, pos, state, player)` for `playerWillDestroy`, `lowerPos(state, pos)`, `isUpper(state)`, and `forwardUse` for `useWithoutItem` and `onWrench`. `BurnerPumpBlock` switches to it first (its GameTests must stay green), then the generator uses it.

Coordination with `m9-electric-pump-engine`: whichever change is applied first extracts `TwoHalves`; that change's `BeamPumpBlock` then only holds pump specific parts (shapes, faces) and calls the helper. When this change is applied second, it reuses the helper as it is. The pump change's design.md D1 is aligned with this in the same commit as this proposal.

Alternative: copy the logic into `CoalGeneratorBlock` (three copies to keep in step; rejected).

### D2. Generator halves

`CoalGeneratorBlock` adds `HALF` to its state, places through `TwoHalves`, creates a block entity only for the lower half, keeps `FACING` and `LIT` on both halves (the upper half mirrors `LIT` through `updateShape` from the lower half, so smoke and the model can read it), and shapes: the lower half a full cube, the upper half the stack `{5, 0, 5, 11, 14, 11}` plus the cap `{4, 14, 4, 12, 16, 12}` (pixels, centred). The carrot's side cycling stays on the lower half; on the upper half the carrot only dismantles (sneaking) and does nothing else.

### D3. Capabilities on the lower half only

`MachineEvents` keeps the registrations for the generator's block entity, which exists only on the lower half, so the chimney has none. The energy provider returns null for `Direction.UP` (the face toward the chimney), and `EnergyNeighbors` pushing skips `UP`. The item side handler is unchanged (side config), except that the top side is locked Off through `SideConfig.lock`, like machine fronts.

### D4. Models and textures

Datagen in `MachineDataParts`: the lower half as an `orientable` model with own textures `coal_generator_front`, `coal_generator_front_lit`, `coal_generator_side`, `coal_generator_top` (no longer the shared `machine_side` and `machine_top`), the upper half as an element model of the stack and cap with `coal_generator_stack` (dark steel with a copper band row) and the cap in the frame's dark colour; blockstates by `HALF`, `FACING` and `LIT`; the item model shows both halves (like `burner_pump_item`). Loot: one Coal Generator from the lower half only (the upper half drops nothing), as the Burner Pump does. Textures follow the "G2" sketch from explore (dark steel frame `#3e3d41` with light corner pixels, copper `#c06a3e`, red `#b8382e`, fire `#ffd640` to `#e65a14`), drawn by a script in the job dir, not in the repo.

### D5. Smoke

`CoalGeneratorBlock.animateTick` on the upper half while `LIT`: `CAMPFIRE_COSY_SMOKE` at the cap centre, the same rate as the Burner Pump.

### D6. Tests

Pure logic: none new (the helper is Minecraft bound); no JUnit tests. GameTests in a new `machine/generator/CoalGeneratorGameTests` (registered like the other Foundation GameTest classes): `placesTwoHalves`, `noRoomAbove`, `breakChimneyDropsOneAndFuel`, `creativeDropsNothing`, `carrotOnChimneyDismantles`, `pistonMovesNothing`, `chimneyOpensGui`, `chimneyHasNoCapabilities`, `noEnergyUpward`, `pushesIntoNeighbourBeside` (existing behaviour on a side face). The Burner Pump GameTests stay green after the helper is extracted.

PLAN.md open decisions settled: none.

## Decisions made during apply

Recorded on 2026-10-12 while applying, where the code differs from or adds to the decisions above:

- **Helper already there.** `m9-electric-pump-engine` was applied first and extracted `machine/TwoHalves`; this change reuses it unchanged (its API is listed in that change's design.md). Task 1.1 needed no code.
- **Lit chimney.** The cube sets `LIT` with `Block.UPDATE_CLIENTS`; the neighbour shape update still reaches the chimney, whose `updateShape` copies `FACING` and `LIT` from the cube. `pushesIntoNeighbourBeside` also checks that the chimney is lit while the cube burns. Smoke comes from the chimney's own `animateTick`, at the cap centre, at the Burner Pump's rate.
- **Carrot on the chimney.** A plain carrot click on the chimney returns success and changes nothing; sneaking forwards to the cube through `TwoHalves.forwardWrench`, so it dismantles like a sneak click on the cube.
- **Block properties.** The generator now has `noOcclusion()` (the chimney is not a full block; the cube's model still culls its own faces) and `PushReaction.BLOCK`. Shapes: the cube a full block, the chimney the stack `{5, 0, 5, 11, 14, 11}` plus the cap `{4, 14, 4, 12, 16, 12}` (`CoalGeneratorBlock.STACK`, `CAP`).
- **Energy push.** `EnergyNeighbors` got a constructor with a skipped face; the generator builds it with `Direction.UP`, so it never even caches the chimney. The energy capability returns null for `UP`; every other face of the cube, the front included, still offers the buffer as before.
- **Models.** A new `datagen/CoalGeneratorModels` (next to `BurnerPumpModels` and `ElectricPumpModels`) instead of more code in `MachineDataParts`: the cube as `orientable` with own front, side and top; the chimney as stack and cap elements whose cap top takes the flue painted in the middle of `coal_generator_top` (hidden under the stack on the cube, uv 4..12), so no sixth texture is needed. The copper band is painted on `coal_generator_stack` (rows 8..10, y 6..8 of the stack), not a separate element. Blockstates are variants by `HALF`, `FACING` and `LIT`. Loot from the lower half only for every machine block with `HALF` (`MachineBlockLoot` now checks the property instead of the pump class).
- **Tests.** `CoalGeneratorGameTests.place(helper, pos, facing)` builds both halves and is used by every test that builds a generator (Foundation machine and pump tests, Storage `ControllerWirelessGameTests`). `chimneyOpensGui` uses a plain player that records the menu provider, as a mock server player cannot receive the open screen payload. `carrotOnChimneyDismantles` also checks that a plain carrot click on the chimney changes no side; `chimneyHasNoCapabilities` checks upkeep as well. `gamedata.json` did not change.
- **Task 6.1.** Build, unit tests and the Foundation, Manufacturing and Storage GameTests are green; the client launch and the play check are left to the user from the main checkout.

## Risks / Trade-offs

- [Existing generators in dev worlds] One block generators have no `HALF`; they load as lower halves without a chimney and `updateShape` would remove them. → Nothing is released; the helper treats a lower half without an upper half as broken only on a neighbour update, so old generators vanish when touched. Accepted; docs note to replace them.
- [Coordination] Two open changes touch the two half logic. → D1: one helper, extracted once, apply order pump change first.
- [Wall of machines] A two block generator no longer stacks under other blocks. → Intended by the user (a chimney needs air).
