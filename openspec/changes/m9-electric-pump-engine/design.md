# Design

## Context

See proposal.md (Why). Current state on `main` (Minecraft 26.1.2, NeoForge 26.1.2.116), all in Foundation:

- `machine/pump/PumpBlock` (abstract, extends `MachineBlock`): stub properties per face, `opensFace` (every face but the front and bottom), `givesWater`, stub boxes, shapes through `bodyShape`.
- `machine/pump/BurnerPumpBlock` extends `PumpBlock` and adds the two halves: `HALF` (`DOUBLE_BLOCK_HALF`), `isUpper`, `lowerPos`, `opensFace` limited to the lower half's back and sides, placement only with room above, `setPlacedBy` puts the upper half, `updateShape` and `neighborChanged` keep the halves together, `playerWillDestroy` removes the lower half without drops in creative or without the tool, `animateTick` smoke, `useWithoutItem` and `onWrench` forward from the upper half. Only the lower half has a block entity.
- `machine/pump/BurnerPumpParts`: shared boxes in pixels facing north (base, firebox, drums, socket, cylinder, post, chimney, beam, pivot, rod, piston), `rotated` and `rotatedPoint`; `BurnerPumpPartsTest` covers them. `machine/pump/BeamStroke`: pure sine stroke (`angle(phase)`, `rodOffset(angle, arm)`).
- `client/BurnerPumpRenderer`: standalone models for beam, rod and piston (`ModelKeys`), the stroke phase advanced in the renderer while `LIT`, drawn rotated like the blockstate.
- `datagen/BurnerPumpModels`: lower, lower lit, upper, beam, rod, piston and item models from `BurnerPumpParts` with vanilla textures (`bricks`, `copper_block`, `iron_block`, `oxidized_copper`, `stone_bricks`, `stripped_dark_oak_log`).
- `machine/pump/ElectricPumpBlock` extends `PumpBlock` directly (one block); its model comes from `datagen/MachineDataParts.pump` (base, body with `electric_pump_front`, outlet); `MachineClient` registers only `SideMarkerRenderer` for it. `MachineEvents` registers its energy capability where `PumpBlock.givesWater` is true, so energy faces follow `opensFace`.
- `machine/pump/PumpItem` places both pumps on the water surface.

## Goals / Non-Goals

**Goals:**
- One shared two half block for both pumps; the electric pump gets the burner's behaviour by inheritance, not by copy.
- An electric engine model and renderer from shared part boxes, with a crank that drives the beam exactly in step with the stroke.
- Splash at the well for both pumps, sparks for the electric one.

**Non-Goals:**
- Rate, energy, recipe, GUI, sounds (proposal Non-goals).

## Decisions

### D1. `BeamPumpBlock` base for the two halves

Move everything about halves out of `BurnerPumpBlock` into a new abstract `machine/pump/BeamPumpBlock extends PumpBlock`: `HALF`, `isUpper`, `lowerPos`, `opensFace`, placement with room above, `setPlacedBy`, `upperOf`, `updateShape`, `neighborChanged`, `playerWillDestroy`, `useWithoutItem` and `onWrench` forwarding, `newBlockEntity` only for the lower half, and `bodyShape` from two abstract shape maps (lower, upper per facing). `BurnerPumpBlock` keeps its smoke `animateTick` and its shapes; `ElectricPumpBlock` extends `BeamPumpBlock` with its shapes and spark `animateTick`. Static callers of `BurnerPumpBlock.isUpper` and `lowerPos` move to `BeamPumpBlock`. `PumpItem` already places through the block's `getStateForPlacement`, so it needs no change beyond a test.

Alternative: copy the half logic into `ElectricPumpBlock` (two places to keep in step; rejected).

The half logic itself (placement with room above, putting and keeping the partner, creative and wrong tool breaking, forwarding use and carrot) goes into a static helper `machine/TwoHalves` that `BeamPumpBlock` calls, because `m9-coal-generator-chimney` needs the same behaviour on a `MachineBlock` that is not a pump. `BeamPumpBlock` keeps only what is pump specific (shapes, faces, `HALF` in its state definition). Whichever of the two changes is applied first extracts the helper; this change is meant to go first.

### D2. Parts: shared frame, own electric parts

Split `BurnerPumpParts` into the shared frame and per pump parts: the frame (base, socket, post, beam, pivot, pump rod, `ROD_X`, `PISTON_X`, `rotated`, `rotatedPoint`) moves to a new `BeamPumpParts`; `BurnerPumpParts` keeps firebox, drums, cylinder, piston, chimney, smoke point; a new `ElectricPumpParts` adds, facing north in pixels (starting values from the concept page, tuned in game):
- Lower: motor housing `{3, 2, 3, 13, 13, 13}`, three copper winding bands 1.2 px tall at y 4, 7 and 10 standing 0.4 px proud, a terminal box on the back of the housing (`{6, 8, 13, 10, 11, 14.5}`, red accent; on the concept page it sat on the upper half), a motor cap `{4, 13, 4, 12, 16, 12}`.
- Upper: cap `{4, 0, 4, 12, 2, 12}`, crank wheel disc centred on `CRANK_X` 12, `CRANK_Y` 5 (upper half frame), radius 3, 1 px thick, crank pin radius 2 from the centre, fan grille box `{3, 2, 9, 7, 8, 13}` with the fan behind its slats.
The crank sits under `PISTON_X` so the con rod hangs from the same beam point as the burner's piston.

`BurnerPumpPartsTest` moves its frame cases to `BeamPumpPartsTest`; new `ElectricPumpPartsTest` checks that every part lies inside its half (0..16), that the con rod reaches from the crank pin to the beam at `PISTON_X` at rest, and that rotated parts stay inside the block.

### D3. Pure crank and stroke helpers

New pure `machine/pump/CrankDrive` (no Minecraft imports): for a stroke phase it gives the crank angle (`360 * phase / period`), the crank pin height (`radius * sin`), and the beam angle, defined so the beam angle equals `BeamStroke.angle(phase)` and the beam end above the crank is highest when the pin is highest. `BeamStroke` gains `bottoms(previousPhase, phase)`: how many times the pump rod passed its lowest point between two phases (the sine minimum of the rod end), used for the splash. The fan angle is the crank angle times a fixed gear factor (example 3).

JUnit: `CrankDriveTest` (pin highest at a quarter period, beam angle matches `BeamStroke` at 8 sample phases, crank angle wraps at 360), `BeamStrokeTest.bottomsCountsOncePerPeriod` (one bottom per full period, none while the phase stands still, two across two periods).

### D4. Phase on the client block entity, shared by renderer and particles

The renderer today advances the phase itself. Splashes need to know when the rod passes its bottom, which a renderer should not decide (it may run several times per tick or not at all). Both pump block entities get a client tick (`BlockEntityTicker` returned for the client in `getTicker`) that advances a `strokePhase` field by one tick while `LIT` and spawns `ParticleTypes.SPLASH` (a few particles, `level.addParticle`) at the socket when `BeamStroke.bottoms(previous, now) > 0`. The renderers read `strokePhase` plus `partialTick` for smooth motion. The phase is client only, never saved or synced.

API: `BaseEntityBlock.getTicker(Level, BlockState, BlockEntityType)` with `level.isClientSide()`, `Level.addParticle`, `ParticleTypes.SPLASH`, `ParticleTypes.ELECTRIC_SPARK` (vanilla 26.1.2, checked in `build/moddev/artifacts/minecraft-patched-26.1.2.116-merged.jar` during apply before use).

### D5. Electric renderer and models

`client/ElectricPumpRenderer` like `BurnerPumpRenderer`: standalone models for the beam (shared with the burner's beam shape but in iron and dark steel), pump rod, con rod, crank wheel and fan, drawn with `ModelKeys`, turned like the blockstate (`quarters(facing)`); the crank wheel and fan rotate about their centres, the beam about the pivot, the con rod is placed from the crank pin to the beam end, the pump rod moves by `BeamStroke.rodOffset`. `MachineClient` registers it instead of `SideMarkerRenderer` (pumps have no side markers, so nothing is lost).

`datagen/ElectricPumpModels` like `BurnerPumpModels`: lower, upper, beam, pump rod, con rod, crank wheel, fan and item models from `BeamPumpParts` and `ElectricPumpParts`, blockstates by `HALF` and `FACING` (no lit variant: the electric look has no window), stub multipart parts as today. `MachineDataParts.pump` stops building the old electric model.

Textures: vanilla `iron_block` (beam, rods, post), `copper_block` (windings), `polished_blackstone` or `smooth_stone` (base, chosen in game), plus own `electric_pump_housing.png` (dark steel with vents), `electric_pump_grille.png` (slats), `electric_pump_fan.png` (blades) and `electric_pump_terminal.png` (red accent like the Power Cable). `electric_pump_front.png` and `electric_pump_front_lit.png` are deleted. Textures are drawn by a script in the job dir, not in the repo.

### D6. Sparks

`ElectricPumpBlock.animateTick` on the lower half while `LIT`: with a small chance per tick (example 1 in 6) one `ELECTRIC_SPARK` particle at a point on the motor brushes (the winding band nearest the crank side), rotated with the facing. Look only.

### D7. Shapes

Each half's outline and collision is the union of its part boxes, rotated per facing, plus the stubs, like the burner (`bodyShape`). The upper half of the electric pump includes the crank wheel, grille, post and beam at rest.

### D8. Wheel rim, axle, bearing and drive belt (play check 2026-10-13)

The play check found the crank wheel paper thin (only its two flat faces are drawn) and floating: nothing holds it, and the right half hangs past the 2 px cap. Changes, all in pixels in the upper half's north built frame unless noted:

- **Wheel.** 2 px thick (`z 6..8`). A rim band of 12 short segments, each a small box tangent to the circle of radius `CRANK_R`, about 1.6 px long, 0.6 px deep radially and as thick as the wheel. The renderer draws one standalone `electric_pump_rim` model 12 times, turned by 30 degrees each, inside the wheel's rotation, so the rim stays round while it turns. The flat faces keep the `electric_pump_wheel` texture.
- **Axle and bearing.** An iron axle about 1x1 px from the wheel centre back to `z` about 14.5, turning with the wheel. The cap widens to `x 4..14` as a mounting plate under the wheel. A bearing block (about `x 10.5..13.5`, `y 3.5..6.5`, `z 10..11.5`) on a short column down to the cap holds the axle. Cap, column and bearing are static: part of the upper model and of the upper shape.
- **Drive belt.** On the back, in the plane `z` about `13.5..14.5`, clear of the windings (`z 13.4`). A small motor pulley (radius about 1.2) on a shaft stub out of the housing back in the lower half, below and right of the back stub (`x 5..11`, `y 5..11`) so pipes and cables on the back never cut the belt, about centre `(13, 3.5)` lower half; an axle pulley (radius about 1.5) on the crank axle at `(CRANK_X, CRANK_Y)` upper half. Two straight belt strands tangent to both pulleys, spanning both halves. Positions are tuned in apply; `ElectricPumpPartsTest` checks the belt stays clear of the back stub and inside the block.
- **Belt motion.** Both pulleys are drawn with spokes and turn: the axle pulley with the crank angle, the motor pulley faster by the radius ratio. The strands are static boxes with a belt texture; one dark lacing mark per strand moves along the strand with the belt speed and wraps, so the belt reads as running. All from the client stroke phase (D4), so the belt stops with the engine.
- **Pure helpers.** The tangent points of the strands, the motor pulley angle and the lacing mark position come from a small pure `machine/pump/BeltDrive` with JUnit tests, next to `CrankDrive`.
- **New models and textures.** Standalone `electric_pump_rim`, `electric_pump_axle` (axle with axle pulley), `electric_pump_motor_pulley`, `electric_pump_belt_mark`; static belt strands in the renderer from one `electric_pump_belt` model stretched and tilted like the con rod. Textures `electric_pump_belt.png` (dark leather) and `electric_pump_pulley.png` (spokes), drawn by the job script.

PLAN.md open decisions settled: none.

## Decisions made during apply

Recorded on 2026-10-12 while applying, where the code differs from or adds to the decisions above:

- **`TwoHalves` API.** `machine/TwoHalves` holds `HALF`, `isUpper`, `lowerPos`, `canPlace(context)`, `placeUpper(level, pos, upper)`, `keepTogether(block, state, direction, neighbour)` (null when the direction does not point at the partner, air when the partner is gone, else the state unchanged so the block copies what the upper half mirrors), `breakPartner(block, level, pos, state, player)` and `forwardWrench(context, state, lowerWrench)`. `BeamPumpBlock` keeps `HALF` as an alias, `quarters`, `shapeOf(parts, facing)`, the faces, the shapes from two abstract methods `lowerShape(facing)` and `upperShape(facing)`, and the client ticker. The upper half mirrors only `FACING`, as before.
- **Shared stroke and client clock.** The stroke constants (12 degrees, 40 ticks) moved from `BurnerPumpRenderer` into a small pure `machine/pump/StrokeClock` that the block entities keep through a new interface `BeamPumpEngine` (`strokeClock()`); `tick(working)` returns the number of bottoms for the splash, `phase(partialTick, working)` gives the renderers a smooth phase. JUnit `StrokeClockTest` covers one splash per stroke and standing still while idle.
- **`bottoms` direction.** The pump rod hangs on the beam's short side (negative arm), so its lowest point is where the beam angle peaks, a quarter period into each stroke; `BeamStroke.bottoms` counts those. `bottomIsAtTheLargestAngle` pins it.
- **Splash and sparks.** Six `SPLASH` particles at the top of the socket per bottom; one `ELECTRIC_SPARK` in 6 client ticks at the top winding on the crank side. Both particle types checked with `javap`.
- **Parts.** The terminal box sits on the front of the housing (`{6, 8, 1.5, 10, 11.5, 3}`) instead of the back: the back carries a stub up to two lines wide (x 5..11, y 5..11) that would run through the box. The front never has a stub and faces the player. The shared `POST` starts at y 3 (on the burner's drum), so the electric upper half adds a `POST_FOOT` `{7, 2, 3, 9, 3, 5}` down to its 2 px cap. The fan is one thin plate `FAN` with the blades painted on (crossed bars read worse at this size) spinning about the x axis behind the grille slats on the west face; the grille is a frame box `GRILLE_FRAME` whose west face has the slat texture with open rows and a solid `GRILLE_BOX` behind it.
- **Con rod.** `CON_ROD` is modelled straight up from the pin at its top (`CRANK_Y + PIN_R`) to the level beam at `PISTON_X`; the renderer tilts and stretches it from the pin where it is to the beam end where it is, so the rod always connects although the beam keeps its sine stroke. `ElectricPumpPartsTest.conRodFromCrankPinToTheBeam` checks that rest pose; `CrankDriveTest` checks that the pin and the beam end above it peak together.
- **Extra texture.** The crank wheel uses its own `electric_pump_wheel.png` (rim, spokes and hub, clear outside the rim), drawn only on its two flat faces so it stays round while it turns. Materials: base and caps `polished_blackstone`, windings `copper_block`, post, beam, rods and pin `iron_block`, socket `oxidized_copper` like the burner. The upper half, the wheel, the fan and the item model use the cutout render type.
- **Pistons.** The Electric Pump now has `PushReaction.BLOCK` like the Burner Pump, and its top side is locked Off like the burner's.
- **GUI test.** A mock server player cannot receive NeoForge's open screen payload in a GameTest, so `electricTopOpensGui` uses a plain player that records the menu provider and position passed to `openMenu`, and checks it is the lower half's Electric Pump block entity whose menu is an `ElectricPumpMenu`.
- **Docs data.** `gradlew :manufacturing:runData` left `docs/src/data/gamedata.json` unchanged (no recipe or balance change).
- **Task 6.1.** Build, unit tests and the Foundation, Manufacturing and Storage GameTests are green; the client launch and the play check are left to the user from the main checkout.

## Risks / Trade-offs

- [Refactor of a working block] Moving the half logic could break the Burner Pump. → Its existing GameTests (placing, breaking, creative, piston, carrot, upper half forwarding) must stay green before the electric pump uses the base.
- [Existing electric pumps in dev worlds] The old one block pump has no `HALF`; it loads as a lower half without an upper half and `updateShape` would remove it. → Nothing is released; accepted, the docs note to replace it.
- [Moving parts readability] At 16 px the con rod and crank are small. → Tune sizes in the play check; parts are data in `ElectricPumpParts`.
- [Particles per tick] Splash and sparks run only on the client and only while lit, a few particles per stroke.
