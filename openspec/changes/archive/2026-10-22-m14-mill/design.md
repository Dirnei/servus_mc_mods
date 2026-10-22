# Design

## Context

See proposal.md for the why. Decided in the explore of 2026-10-20 (`temp/delicacies-brief.md`, "Foundation parts"): the Mill, the Hand Crank and the Electric Motor live in Foundation; the mill looks like option B of `temp/mill-mockup.html` (box sizes from `steelMill` in `temp/delicacies-mockup-src/mill_template.html`); the recipe type is `servus_foundation:milling` and Foundation ships gravel to sand; the mill grinds only while driven; a crank or motor drives only the block it touches (no Create style shaft network); the motor takes the Speed Upgrade.

Foundation already has: the machine base (`MachineBlock`, `MachineBlockEntity`, `PoweredMachineBlockEntity`, `MachineMenu`, `MachineScreen` with `Layout`), side configuration with fixed sides, machine stats from `data/<ns>/machine/<name>.json` (`MachineStatsManager`, `MachineDefaults`), upgrades (`UpgradeSlots`, `Upgrades`, `UpgradeMath`), recipe sync to clients (`ServusRecipes.syncToClients`), the Electric Pump's motor look and its renderer pattern (standalone models turned by a block entity renderer, `ModelKeys`, `ModelPoses`, a client side clock advanced by a client ticker), and a JEI plugin. The Alchemy Prelum (`servus_alchemy:pressing`) is the model for a data driven single input recipe and its JEI category.

## Goals / Non-Goals

**Goals:**
- A data driven grinder in Foundation that Delicacies and Brewery fill with their own recipes.
- A drive link small enough for one driver per block, open to other mods.
- A day one hand drive and a powered drive with the Speed Upgrade.
- The family steel look, with moving parts that turn only while working.

**Non-Goals:** see proposal.md.

## Decisions

### D1 Drive capability
`ServusFoundationCapabilities.DRIVE = sided("drive", Drivable.class)`, a `BlockCapability<Drivable, Direction>` (NeoForge `BlockCapability.createSided`, as the other Servus capabilities). The context is the side of the driven block the driver touches. `drive/Drivable` is a plain Java interface:

```java
public interface Drivable {
    boolean wantsWork();
    void addWork(int work);
    default void turn() {}
}
```

`turn()` is called every tick a driver turns the block, with or without work, so the block can show that it turns (the Mill's wheel turns with a crank even when the Mill is empty). `addWork` counts as a turn too.

Drivers query it each time they act (`level.getCapability`, the motor through a `BlockCapabilityCache` created on first server tick, NeoForge `net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(capability, ServerLevel, pos, context)`). Alternatives considered: a Java interface on the block entity (only Foundation blocks could take drive) and a shaft network (rejected in the explore).

### D2 Pure drive logic
Package `foundation/drive`, no Minecraft imports, JUnit tested:
- `WorkProgress`: work collected toward a goal. `add(work, goal)` adds and reports whether the goal was reached; reaching it starts over at 0 and drops the rest, so a strong driver never banks work for the next item. `reset()`, `value()`, `set(int)` for saving.
- `MotorRate(int workPerTick, int energyPerTick)`, `MotorRate.of(baseWork, baseEnergy, multiplier)`: both multiplied by the upgrade multiplier and rounded, work at least 1, so FE per unit of work stays the same.
- `CrankTurns(ticksPerTurn)`: the crank's hold and turn counting. `click(now)` records a use click at game time `now`; `driving(now)` is true until the next click is due (4 ticks, the use key's repeat interval, counted inclusive so held clicks drive every tick whether a click is handled before or after the crank's tick), `held(now)` for 8 ticks, so one late click packet does not stop and restart the crank. `step(workPerTurn)` counts one driving tick and returns this tick's share of the turn's work (shares add up to `workPerTurn` over `TICKS_PER_TURN` = 16 ticks) and whether the turn was completed.
- `Flywheel(degreesPerTick, spinUpTicks, coastTicks)`: replaces the first `SpinClock`. The client side angle of a turning part with momentum: `tick(driven)` once per client tick moves the speed (0 to 1) up by `1/spinUpTicks` while driven and down by `1/coastTicks` while not, then advances the angle by speed times `degreesPerTick`; `angle(partialTick)` interpolates between the last two tick angles, so a frame never steps. Defaults 4 ticks spin-up and 20 ticks coast: linear slowdown, about a second, about half a turn at crank speed.

### D3 Drive balance data
`drive/DriveStats(int workPerTick, int workPerTurn, float exhaustionPerTurn)` with a pure `Partial` of optional fields (its codec lives in the manager, since unit tests run without the game libraries) `work_per_tick`, `work_per_turn`, `exhaustion_per_turn`, merged over code defaults like `MachineStats.Partial`. `DriveStatsManager` (a `SimpleJsonResourceReloadListener` on `data/<ns>/drive/`, registered in `MachineEvents.onAddReloadListeners` with `AddServerReloadListenersEvent`) holds the files. Shipped files: `drive/hand_crank.json` (`work_per_turn` 25, `exhaustion_per_turn` 0.1) and `drive/electric_motor.json` (`work_per_tick` 5). The motor's FE lives in its machine stats, as for every powered machine: `MachineDefaults` entry `servus_foundation:electric_motor`, kind `powered`, no recipe types, `MachineStats.powered(1, 20, 8000)`, file `machine/electric_motor.json`. The Mill gets no machine stats entry: its only number is the recipe's work (the docs lab knows only the kinds burner, powered, crafter and generator, and the Mill is none of them).

### D4 Milling recipe
`machine/mill/MillingRecipe(SizedIngredient input, ItemStackTemplate resultTemplate, int work) implements Recipe<SingleRecipeInput>`, JSON `{"type": "servus_foundation:milling", "input": {"ingredient": "minecraft:gravel", "count": 1}, "result": {"id": "minecraft:sand"}, "work": 200}`; `matches` checks ingredient and count, `isSpecial` true, never in the recipe book, `PlacementInfo.NOT_PLACEABLE`, like `AlchemyRecipe`. Registered in `MachineContent` (`RECIPE_TYPES`, `RECIPE_SERIALIZERS`, `MILLING`) and synced with `ServusRecipes.syncToClients(MachineContent.MILLING)` in the Foundation constructor. Datagen writes `recipe/milling/sand.json` from `MachineDataParts.recipes`.

### D5 Mill block entity
`MillBlockEntity extends MachineBlockEntity` (2 slots: 0 input, 1 output) with a `Drivable`:
- `wantsWork()`: a recipe matches the input (`ServusRecipes.first`) and the output takes its result.
- `addWork(work)`: when it wants work, adds to its `WorkProgress` toward the recipe's work and remembers the game time; on reaching the goal it takes the recipe's count and inserts the result.
- `turn()`, and `addWork`, remember the game time.
- `tick()`: resets progress when no recipe matches; returns "turned within the last 5 ticks", which sets `LIT` and so drives the wheel's `Flywheel` on the client. Crank and motor turn it every tick, so the window only bridges a missing tick; it ends together with the crank's 8 tick hold.
- `getData(DATA_WORK_TICKS)` reports the recipe's work, `progress` is the work collected, so the screen arrow shows work done.
- `acceptsInput` asks every milling recipe (synced, so client slot and server agree).
- Sides: Items layer defaults `TOP`, `BACK`, `LEFT`, `RIGHT` Input and `BOTTOM` Auto; `FRONT` fixed to Push (the chute). `MachineBlockEntity` fixes the front Off in its constructor; the mill fixes it again to Push (`SideConfig.lock` replaces a lock).
- Drive capability on `BACK`, `LEFT`, `RIGHT` (and `null`), registered in `MachineEvents`, with `Capabilities.Item.BLOCK` through `itemCapability(side)`.

`MillBlock extends MachineBlock`: own properties without light (a lit Mill does not glow), `noOcclusion`, shape from its parts. Screen: `MachineClient.MILL = new Layout(false, true, false, gui("mill"))` (arrow and side panel); `MillMenu` input at 56,35 and output at 116,35 like the Prelum.

### D6 Hand Crank
`HandCrankBlock extends BaseEntityBlock` with `FACING` (horizontal, pointing from the driven block to the crank) and `TURNING` (boolean, the server's spin state). `getStateForPlacement` needs a horizontal clicked face and a `DRIVE` capability on the clicked block for that face, else null. `canSurvive` asks the same of the block behind it (only when the reader is a `Level`), and `updateShape` turns a crank that cannot survive into air, so it drops by its loot table like a torch. `useItemOn` passes for the Configuration Carrot and otherwise goes to `useWithoutItem`; on the server `HandCrankBlockEntity.click(player)` records the click and the player (`CrankTurns.click`). Holding the use key repeats the click every 4 ticks (`Minecraft.rightClickDelay = 4`, checked in the 26.1.2 sources). The crank now ticks on the server: while `driving`, it calls `turn()` on the driven block and, if that wants work, `step(work_per_turn)` hands it the tick's share of work; a completed turn calls `player.causeFoodExhaustion(exhaustion_per_turn)` (vanilla skips exhaustion for invulnerable players). While not driving it gives nothing, so coasting gives no work. When `held` changes the server sets `TURNING` with `Block.UPDATE_CLIENTS`: the only sync, on start and stop, and players who load the chunk later get it with the block state. The client ticker runs the arm's `Flywheel` from `TURNING`, at 360 / 16 degrees per tick, one turn per 16 ticks as the work. This replaces the first build's quarter turn block events, whose arm and wheel stepped per click. No GUI, no item capability.

### D7 Electric Motor
`ElectricMotorBlock extends MachineBlock`, `FACING` is the shaft direction. Placement: when the clicked face is horizontal and the clicked block takes drive on it, the shaft faces that block; otherwise it faces the player's look direction. `ElectricMotorBlockEntity extends PoweredMachineBlockEntity` with no item slots and an `UpgradeSlots(1)`:
- `canWork()`: the driven block's `Drivable` exists and wants work.
- `energyPerTickNow()`: `MotorRate.of(work_per_tick, energy_per_tick, upgrades.multiplier()).energyPerTick()`.
- `finishWork()` (every tick, `work_ticks` 1): `addWork(rate.workPerTick())`.
- Energy capability only on the back (`side == facing.getOpposite()`) and `null`.
- Every item side fixed Off: the motor has no items, so pipes never connect; the carrot still dismantles it.
- The upgrades are saved with the block entity and drop when it is broken or dismantled (`dropContents`).
- `ElectricMotorMenu`: the upgrade slot at 80,35 (`ResourceHandlerSlot` on the `UpgradeSlots`, as the Ender Anchor does); screen `MachineClient.ELECTRIC_MOTOR = new Layout(false, false, true, false, false, gui("electric_motor"))`, energy bar, no side panel.
- Sparks: `animateTick` while `LIT`, one `ParticleTypes.ELECTRIC_SPARK` at the shaft now and then, like the Electric Pump.

### D8 Look and models (built facing north, front at z 0, pixels)
All parts in `*Parts` classes (pure Java float boxes); shapes from the same boxes through a shared `machine/PartShapes` helper. The pumps' `BeamPumpBlock.quarters/box/shapeOf` and `BeamPumpParts.rotated/rotatedPoint` move there (`PartShapes` for the shape and facing helpers, `PixelBoxes` for the pure box rotation), so the new blocks do not copy them.

**Mill** (`MillParts`), static model plus a turning wheel drawn by `MillRenderer` about the vertical axis through (8, 8):
- `BASE` heavy iron 0,0,0 to 16,5,16; `HOUSING` steel 1,5,1 to 15,10,15.
- Wheel, turning: `RIM_SEGMENT` iron, 12 copies, radial 5.2 to 6.6 from the axis, 3.6 wide, y 10 to 11.5, copy i lowered at the top by i x 0.005; `SPOKE` iron, 6 copies, 1.5 to 5.4 from the axis, 0.8 wide, y 10 to 11; `HUB` brass 6.2,10,6.2 to 9.8,12,9.8 and `HUB_TURNED`, the same turned 45 degrees and 0.05 lower at the top. Wheel parts have no down faces: they sit on the housing.
- `HOPPER` steel 4.5,12,4.5 to 11.5,15,11.5; `HOPPER_RIM` brass 4,15,4 to 12,16,12, its top painted as the hopper's mouth.
- `CHUTE` steel 6,1.5,-2 to 10,4.5,0, sticking out of the front like the mockup's chute; its north face painted with the dark mouth.

**Hand Crank** (`HandCrankParts`), facing north means the driven block is south (z 16), the handle toward z 0:
- Static: `FLANGE` heavy iron 5,5,15 to 11,11,17, reaching one pixel into the driven block so it meets the Mill's housing, which is inset by one pixel; `BEARING` heavy iron 6.5,6.5,13 to 9.5,9.5,15.
- Turning about the z axis through (8, 8): `SHAFT` steel 7.25,7.25,11 to 8.75,8.75,13; `ARM` heavy iron 7,7,10 to 9,14,11; `HANDLE` oak 7.5,12.5,6 to 8.5,13.5,10.

**Electric Motor** (`ElectricMotorParts`), shaft at z 0, back at z 16:
- Static: `FEET` heavy iron 4,0,3 to 12,3,13; `BODY` steel 3,3,2 to 13,13,14.5; `WINDINGS` copper, three bands 2.6,2.6,z to 13.4,13.4,z+1.2 at z 4, 7.4 and 10.8; `TERMINAL` the pump's terminal box 6,13,8.8 to 10,14.5,10.6 between two bands; `FRONT_PLATE` heavy iron 5,5,1.5 to 11,11,2; `GRILLE` 4,4,14.5 to 12,12,16 with the round grille on its back face (cutout gaps) and steel elsewhere.
- Turning about the z axis through (8, 8): `SHAFT` steel 7.25,7.25,0 to 8.75,8.75,1.5 and `COUPLING` 6,6,-1 to 10,10,0 with the pump's pulley face on its front, reaching one pixel into the driven block so it meets the Mill's housing; `FAN` 5.2,5.2,15.2 to 10.8,10.8,15.3 inside the grille, painted both sides.

Overlapping box pairs and their planes (CLAUDE.md "Models and textures"):

| Pair | Shared plane? | How |
|---|---|---|
| Mill base / housing | no | housing ends on the base top (y 5), faces touch back to back |
| housing / wheel parts | no | wheel parts start on the housing top and have no down faces |
| rim segment / rim segment | no | each copy's top 0.005 lower |
| spoke / rim segment | no | spoke ends inside the rim, its top (11) below the rim top (11.5) |
| spoke / spoke | no | inner ends at 1.5 from the axis, they meet only within 0.7 |
| spoke / hub | no | inner ends inside the hub, top below the hub top |
| hub / hub turned | no | turned copy 0.05 lower at the top, sides at 45 degrees |
| hub / hopper | no | hub ends at the hopper bottom (y 12), back to back |
| hopper / hopper rim | no | hopper ends at the rim bottom (y 15) |
| chute / base | no | chute ends at the base front (z 0) |
| crank flange / bearing / shaft / arm / handle | no | each ends at its neighbour's face (z 15, 13, 11, 10) |
| motor feet / body | no | body starts on the feet top (y 3); feet narrower |
| body / windings | no | bands stand 0.4 proud on every side |
| windings / feet | no | band bottom (2.6) inside the feet, no common face |
| terminal / body, windings | no | terminal starts on the body top (y 13), between bands |
| front plate / body, shaft / plate, coupling / shaft | no | each ends at its neighbour (z 2, 1.5, 0) |
| crank flange, motor coupling / Mill housing | no | they end at the housing side, back to back |
| grille / body | no | grille starts at the body back (z 14.5) |
| fan / grille | no | fan inside the grille, 0.1 thick at z 15.2 to 15.3 |

Renderers: `MillRenderer` turns the wheel models, `HandCrankRenderer` turns the shaft, arm and handle, `ElectricMotorRenderer` turns the coupling with shaft and the fan, each from its block entity's `Flywheel` (crank from `TURNING`, Mill and motor from `LIT`) with the frame's partial tick; all through `ModelKeys` standalone models and `ModelPoses.faceFromNorth`, registered in `MachineClient`. Item models hold every part at rest (spokes and rim segments turned by element rotations, which take any angle).

### D9 Textures
`tools/mill-textures/gen.py`, pure Python like the other generators, writes into `foundation/src/main/resources/assets/servus_foundation/textures/`: `block/heavy_iron.png` (copied from Shuttles' `heavy_rack_iron.png`, which Foundation cannot reference), `block/brass.png`, `block/mill_hopper_top.png`, `block/mill_chute.png`, `block/electric_motor_grille.png` (round grille, cutout gaps), and `gui/mill.png` (from `burner_machine.png`: input slot moved to y 35, fuel and flame cleared) and `gui/electric_motor.png` (from `coal_generator.png`: slot moved to 79,34, flame cleared). Reused: `electric_pump_steel` (steel parts), vanilla `copper_block` (windings, as on the pump), `electric_pump_terminal`, `electric_pump_pulley`, `electric_pump_fan`, vanilla `oak_planks` (crank handle).

### D10 Recipes
- Mill: ` H ` / `IGI` / `III`: Hopper, Grindstone, Iron Ingots.
- Hand Crank: `IS` / `I `: Iron Ingots and a Stick.
- Electric Motor: `ICI` / `CRP` / `ICI`: Iron and Copper Ingots, Redstone and a Foundation Power Cable.

### D11 JEI
`integration/jei/MillingRecipeCategory` (input slot, animated arrow, output slot, "200 work" below), registered with `ServusRecipes.all(level, MILLING)` and the Mill as catalyst in `ServusFoundationJeiPlugin`.

## Risks / Trade-offs

- [Holding use on a block depends on the client's repeat delay] The 4 tick repeat is vanilla behaviour; a mod that changes it changes the crank's speed, which is harmless.
- [The chute sticks 2 px out of the block] A block in front hides the chute's tip; it reads as the chute reaching into the bin.
- [Capability lookups in `canSurvive`] Only answered when the reader is a `Level`; during world generation the crank is never placed.

## Assumptions

- Exhaustion is charged only for turns that give work, so cranking an idle mill is free.
- A full turn is 16 held ticks, the time four clicks took in the first build, so work per second (25 work per 0.8 s) and exhaustion per turn stay as they were; the work comes spread over the turn instead of at its end.
- Coasting gives no work: the flywheel is only the look. The server holds only whether the crank is turning (target speed), not the coasting speed.
- One click drives until the next is due (letting go may give one tick of work more than the first build); the crank keeps turning 4 ticks more before it stops, so a late packet does not stutter it. The Mill's 5 tick window ends at the same tick, so crank and wheel start coasting together.
- The crank sits only on horizontal sides; the Mill takes drive on back, left and right, not on the top (hopper), bottom or front (chute).
- The Mill's bottom defaults to Auto (pushes into a chest below); its sides and top to Input.
- Numbers: gravel to sand 200 work; crank 25 work and 0.1 exhaustion per turn (about 6.4 s per Sand by hand); motor 5 work for 20 FE per tick (2 s and 800 FE per Sand), buffer 8000 FE.
- Upgrades drop when the motor is dismantled; no keep-on-item like the Ender Anchor.
- The Mill is not a producer on the data line yet (proposal non-goal).
