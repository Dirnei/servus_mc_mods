# Design

## Context

See proposal.md for the why. After `m9-calciner-multiblock` the Calciner keeps Salt, Sulfur and Mercury in `PrincipleTanks` and offers Salt as a drain only fluid view on the Ash Pan (`CalcinerPorts.fluid`), pushed into touching handlers by `CalcinerPorts.push`. The Coagulator, the Athanor and the Gnome Hollow take liquid Salt and stay as they are. Calcination recipes (`CalcinationRecipe`) give `Principles` in mB.

## Goals / Non-Goals

**Goals:**
- Ash as the Calciner's Salt output with no change to the calcination data.
- One number decides how much Salt an Ash is worth, and it lives in datapack JSON.
- A Purifier that fits the Fluid Pipe rules without configuration: water in, Salt out.

**Non-Goals:**
- A shared machine base for GUI less blocks; the Purifier is small enough to stand alone.

## Decisions

### Ash is counted Salt
The Calciner keeps its Salt tank in mB. The Ash Pan offers `salt / saltPerAsh` Ash and taking n Ash drains `n * saltPerAsh` mB; a remainder stays for the next Ash. `saltPerAsh` is read from the purification recipes: the Salt of the first recipe whose ingredient takes Ash, divided by its item count, falling back to 100 when no recipe takes Ash. Alternatives: a new MachineStats field (a Foundation record grows for one Manufacturing number) or a separate JSON with its own reload listener (more code for one number); reading the recipe also guarantees that the Ash Pan and the Purifier agree.

The Ash Pan port (`CalcinerPorts.AshPort`) is a one slot `IItemHandler` that shows the Ash as a stack of at most 64 and only extracts. The Salt fluid view and the Salt push leave the Ash Pan; an empty hand on the Ash Pan hands out all whole Ash. Calcination still waits when the Salt tank has no room, so a full Ash Pan (example 40 Ash) stops the Calciner until it is emptied. The BER draws a heap with the Ash texture instead of the Salt fluid.

### Purifier
- `alchemy/purifier/PurifierBlock` is a plain `BaseEntityBlock` with a server ticker, not a `MachineBlock`: it has no facing, no lit state, no menu and no side configuration, and the carrot leaves it alone (it does not implement `Wrenchable`, has no facing property, so the carrot passes).
- `PurifierBlockEntity` holds a one slot `ItemStackHandler`, a water `FluidTank` and a Salt `FluidTank` (capacity from `MachineDefaults.stats("servus_manufacturing:purifier").fluidCapacity()`, `data/servus_manufacturing/machine/purifier.json`), progress, and a `TeamOwnership` for the journal. Cycle: find the purification recipe for the slot, check water and room, count progress to the recipe time, then take and add. Every 10 ticks it pushes Salt into touching fluid handlers.
- `PurificationRecipe(SizedIngredient input, int water, int salt, int time)`, type and serializer in `ServusManufacturingRecipes`, written by datagen (`purification/ash.json`: 1 Ash, 100 mB water, 100 mB Salt, 40 ticks).
- Capabilities by block entity: item handler insert only for matching items; fluid handler with two tanks that fills only water and drains only Salt. By the Fluid Pipe rules a pump's water side sees the Purifier as a sink and the Salt side as a source, with no endpoint setting.
- Hand use: Ash in, water bucket in (`FluidUtil.interactWithFluidHandler` against the handler: it fills only water and drains only Salt, so one call handles both buckets), sneak with an empty hand returns the slot.
- Look: a vat model of oak planks with iron bands from vanilla textures; `PurifierRenderer` draws the brine (Salt if any, else water) inside at the height of what it holds, syncing when the drawn level changes by a sixteenth, like the Calciner.
- Journal: `purify` event with the Salt made, through `Journal.recordEvent`; Villach gets `{"id": "purify", "event": "purify", "count": 1}`.
- Jade: the Calciner plugin becomes `ServusManufacturingJadePlugin` with a second provider for the Purifier.
- JEI: `PurificationRecipeCategory` like the calcination one, with the Purifier as catalyst.

### Purifier as a build (play check, 2026-10-10)
The user asked for a small build and a waste product. Layout (`alchemy/purifier/PurifierLayout`, pure): Leaching Vat (the Purifier block, controller) at (0,0) with planks of any wood at (-1,0) and (1,0), the Catch Basin (`CatchBasinBlock`) at (0,-1) with logs of any wood at (-1,-1) and (1,-1). It reads the same from front and back, so forming tries the X and Z axes and stores the axis on the Vat (`HORIZONTAL_AXIS`); the Basin's controller is simply the block above it. Planks and logs stay vanilla and are checked from the Vat and the Basin in `neighborChanged` (both touch them), matched by the block tags `minecraft:planks` and `minecraft:logs`. In the schematic the Basin, the bottom middle, is the `0` anchor.

The waste is the item Caput Mortuum, the alchemical name for the residue left after the useful part is drawn out; an item rather than a slurry fluid because it stacks, hoppers and counts for the Crafting CPU with nothing new. `PurificationRecipe` gains an optional `residue` item stack. The shipped recipe washes 4 Ash with 400 mB water into 400 mB Salt and 1 Caput Mortuum in 160 ticks, so the Salt per Ash stays 100. Its later use (carbon, diamond) is a Rubedo proposal in PROGRESSION.md.

Ports by block: the Vat's item handler (slot 0 ingredient insert only, slot 1 residue extract only) and fluid handler (water fill only) are on the Vat's block entity; the Salt drain only view is registered by block on the Basin and pushes from the Basin into touching handlers outside the build. Renderers: the Vat draws the ash layer and the water; the Salt is drawn in the Basin below.

### Split outputs and forming looks (play check, 2026-10-10)
The user wanted Salt and Caput Mortuum in separate containers for easier pipe access, and a visible change on forming. The layout becomes `P V P` over `B L R`: planks beside the Vat, a log of any wood under the Vat, the Catch Basin under one plank and the new `ResidueCrockBlock` under the other. The Basin and the Crock may swap sides, so `PurifierLayout` lists the cells for one side and its mirror, and forming tries both axes and both mirrors. The Vat keeps `HORIZONTAL_AXIS`; the Basin and the Crock get `HORIZONTAL_FACING` toward the log, set by `PurifierStructure.form`, and find their controller at `pos.above().relative(facing)`. That facing also turns their formed models. In the schematic the log, the bottom middle, is the `0` anchor.

Ports: the residue slot stays in `PurifierBlockEntity`; the Vat's item handler takes only the ingredient and gives nothing; the Crock registers an extract only item view of the residue slot by block, like the Basin's Salt view. An empty hand on the Crock takes the residue; the Vat keeps sneak to take the Ash back. `CatchBasinBlock` and `ResidueCrockBlock` route `onRemove` and `neighborChanged` into `PurifierStructure`, which now checks planks and the log from the Vat and the side blocks. The Crock is crafted from bricks around a barrel; `PurifierRenderer` draws a residue heap in the Crock.

Formed models, from vanilla textures in `CalcinerModels`: `purifier_formed` (the vat with a dark oak spout over each plank, sloping outward), `catch_basin_formed` and `residue_crock_formed` (a rim and a bracket toward the log). Blockstates: `formed=false` picks the plain model for every axis or facing, `formed=true` picks the formed model rotated by `y` from the axis or facing. Overhanging pieces stay smaller than a face and keep 1 px from the plank and log faces, so nothing z fights; the Vat's inside stays clear for the brine the renderer draws.

Forming effect: `alchemy/structure/FormingEffect.play(ServerLevel, BlockPos controller, Collection<BlockPos> cells)` plays `SoundEvents.ENCHANTMENT_TABLE_USE` at the controller with `Level.playSound(null, ...)` and sends a few `ParticleTypes.ENCHANT` per cell with `ServerLevel.sendParticles`, so every nearby client sees it. It is called from the Journal path of each build (`CalcinerStructure`, `PurifierStructure`, `CoagulatorStructure`, the Athanor) only after a successful form, not from `form` itself, so GameTest helpers and unform stay silent. It lives in this change because the Journal forms every build; the Coagulator and Athanor changes are archived later and need no spec edit.

### Ash texture
A 16x16 grey speckled PNG written with the session's `png.py` helper, used by the item model and the Ash Pan heap.

### Pure vs Minecraft
Pure and JUnit tested: `AshCount` (whole Ash from mB and the mB n Ash take). GameTests: Ash from the Ash Pan by hopper and hand, no fluid at the Ash Pan, Purifier cycles, water and Salt through its handler, buckets, the `purify` journal event, both mirrors forming, residue by hopper under the Crock, formed states set and cleared. Manual: models, formed looks, the forming effect, brine level, Jade, JEI, journal pages.

### PLAN.md
M9 item 6b2 `m9-salt-leaching`; the Tria Prima paragraph says Salt leaves the Calciner as Ash and the Purifier leaches it into liquid Salt; the Nigredo table gets the Purifier.

### Archive order
`m9-calciner-multiblock` creates `manufacturing/calciner-structure`; this change modifies it. Archive the Calciner change first.

## Risks / Trade-offs

- [Existing worlds hold liquid Salt in Calciners] → Nothing is released; the Salt tank stays and simply becomes Ash.
- [A full Ash Pan stops the Calciner] → Intended, like full tanks before; the journal says to empty the Ash Pan or put a hopper under it.
- [Recipe lookup on every Ash Pan access] → Cached per recipe manager reload in the Calciner block entity.
