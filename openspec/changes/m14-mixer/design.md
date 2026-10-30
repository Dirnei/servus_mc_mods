# Design

## Context

See proposal.md for the why. Decided with the user on 2026-10-30: the Mixer is a Foundation block, the Mill's sibling; recipe type `servus_foundation:mixing` (counted items shapeless, at most one fluid, one item result, work); driven exactly like the Mill through the drive link on back, left and right; one tank filled through a port on the back and by bucket; result out of a front chute; the look from the mockup (open steel tub on a heavy iron base, brass rim, steel bridge with a brass paddle hub, crossed heavy iron blades, contents layer, chute, brass port); shipped recipes Mud, Coarse Dirt and Concrete Powder; a JEI Mixing category.

Foundation already has everything the Mill uses (`MachineBlock`, `MachineBlockEntity`, `MachineMenu`, `MachineScreen` with `Layout`, `ServusRecipes`, the drive link `ServusFoundationCapabilities.DRIVE` with `Drivable`, `WorkProgress`, `Flywheel`, `PartShapes`, `PixelBoxes`, `ModelKeys`, `ModelPoses`) plus what a tank needs: `FluidSlot` (a `FluidStacksResourceHandler` with a validator), the Fluids side layer in `MachineBlockEntity` (`sideTanks`, `fluidModes`, `fluidSides`, `tankFluid`), `FluidSideBlock`, `FluidSideHandler`, `FluidSideMover`, the side panel's Fluids tab, `FluidGauge`, `FluidBox`, `FluidSprites`, and the menu data entries `DATA_FLUID_AMOUNT`, `DATA_FLUID_CAPACITY`, `DATA_FLUID_ID` (`DATA_COUNT_FLUID`). No Foundation machine has used the Fluids layer of `MachineBlockEntity` yet; the Mixer is the first. Delicacies' Pot and Brewery's Brew Kettle already do counted shapeless items plus a fluid (`VesselRecipe`, `BrewingRecipe`) with a pure `delicacies/cooking/SlotMatcher` and a one kind per slot rule in `VesselBlockEntity.canHoldInput` and `BrewKettle`.

## Goals / Non-Goals

**Goals:**
- A data driven mixer in Foundation that Alchemy and Delicacies fill with their own recipes.
- The Mill's drive, sides and look carried over, with a tank as the only new kind of part.
- The shapeless matching logic shared and pure, not copied a third time.

**Non-Goals:** see proposal.md. No change to the drive link, the Mill, the Hand Crank or the Electric Motor.

## Decisions

### D1 Recipe type
`machine/mixer/MixingRecipe(List<SizedIngredient> ingredients, Optional<SizedFluidIngredient> fluid, ItemStackTemplate resultTemplate, int work) implements Recipe<MixingInput>`, JSON:

```json
{"type": "servus_foundation:mixing",
 "ingredients": [{"ingredient": "minecraft:dirt", "count": 1}],
 "fluid": {"ingredient": "minecraft:water", "amount": 250},
 "result": {"id": "minecraft:mud"},
 "work": 100}
```

- Codec: `SizedIngredient.NESTED_CODEC.listOf(1, MixerBlockEntity.INPUTS)` (DFU 9.0.19 `Codec.listOf(int, int)`, checked in the jar), `SizedFluidIngredient.CODEC.optionalFieldOf("fluid")` (NeoForge 26.1.2.116 `net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient`, fields `ingredient` and `amount`), `ItemStackTemplate.CODEC`, `Codec.INT` work. Stream codec: `SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(INPUTS))`, `ByteBufCodecs.optional(SizedFluidIngredient.STREAM_CODEC)`, as `PotRecipe` does.
- `MixingInput(List<ItemStack> items, FluidStack fluid) implements RecipeInput`, like Delicacies' `VesselInput`, with `isEmpty` false while it holds fluid.
- `matches`: the fluid, if any, passes `SizedFluidIngredient.test` (fluid and amount), and `SlotMatcher.match` (D3) finds a slot for every ingredient with every non-empty slot used. `slotsFor(input)` returns the assignment for consuming. `accepts(ItemStack)` and `acceptsFluid(FluidStack)` ignore counts, for slot and tank acceptance.
- `isSpecial` true, `PlacementInfo.NOT_PLACEABLE`, `RecipeBookCategories.CRAFTING_MISC`, like `MillingRecipe`.
- Registered in `MachineContent` (`MIXING_ID`, `MIXING`, `MIXING_SERIALIZER`), synced with `ServusRecipes.syncToClients(MachineContent.MIXING)` in `ServusFoundation`.
- Four input slots (`INPUTS = 4`) and so at most four ingredients: Concrete Powder needs three, Alchemy's Dirt three, and a 2x2 grid fits beside the gauge in the 176 px screen.

**Choosing among matches.** The Mixer asks `ServusRecipes.all(level, MIXING)`, keeps the matching ones and takes the first with a fluid, else the first. Exact slot use already separates most recipes (Dirt alone with Water is Mud, Dirt with Gravel is Coarse Dirt); the fluid rule settles a dry and a wet recipe with the same items in favour of the one that uses what was poured in. Alternative: `ServusRecipes.first` as the Mill does; rejected because a dry recipe could then shadow a wet one by load order.

### D2 Shipped recipes (`datagen/MixingRecipes`, beside `MillingRecipes`)
`mix(output, name, ingredients, fluid, result, count, work)` and a table; ids `servus_foundation:mixing/<name>`; called from `MachineDataParts.recipes`. Items as vanilla's own recipes use them (`RecipeProvider.concretePowder` and the Coarse Dirt recipe in `VanillaRecipeProvider`, 26.1.2 sources: plain `Blocks.SAND`, `Blocks.GRAVEL`, `Blocks.DIRT` and the dye item, no tags).

| Recipe | Items | Fluid | Result | Work (example) |
|---|---|---|---|---|
| `mud` | 1 Dirt | 250 mB Water | 1 Mud | 100 |
| `coarse_dirt` | 2 Dirt, 2 Gravel | none | 4 Coarse Dirt | 100 |
| `<color>_concrete_powder` (16) | 4 Sand, 4 Gravel, 1 `<color>` Dye | none | 8 `<color>` Concrete Powder | 200 |

Mud takes 250 mB, the bottle a player pours on Dirt in vanilla. No two rows match the same contents: their item sets differ in kinds or count of slots, which a GameTest checks (D9).

### D3 Pure slot logic in `foundation/production`
- `SlotMatcher.match(wants, slots, isEmpty)`: copied unchanged from `delicacies/cooking/SlotMatcher` (backtracking, every want its own slot, every filled slot used), with its JUnit test moved alongside as `SlotMatcherTest`. Delicacies and Brewery switch to it and the Delicacies copy goes once `m14-tiled-stove` and `m15-brewhouse` are archived (proposal non-goal); until then two identical copies exist.
- `KindSlots.accepts(slots, slot, candidate, isEmpty, sameKind)`: the one kind per slot rule of `VesselBlockEntity.canHoldInput` as pure generic Java. A candidate fits a slot that holds the same kind, or an empty slot when no other slot holds that kind.
- No Minecraft imports; the Mixer passes `ItemStack::isEmpty` and `ItemStack::isSameItemSameComponents`.

### D4 Mixer block entity
`MixerBlockEntity extends MachineBlockEntity implements FluidSideBlock`, 5 slots (0 to 3 input, 4 output), a `FluidSlot tank` whose validator is "some mixing recipe's fluid accepts it" (recipes are synced, so client and server agree), a `WorkProgress`, a `Flywheel` for the paddle and the Mill's `Drivable` pattern:
- `wantsWork()`: `runnableRecipe()` present (a match whose result fits the output).
- `addWork(work)`: turns; when runnable, adds to the `WorkProgress` toward the recipe's work; on reaching it, `slotsFor` gives the slots, each loses its ingredient's count, the tank drains the fluid amount, the result goes into the output.
- `turn()`: remembers the game time; `tick()` returns "turned within the last 5 ticks" for `LIT`, resets the progress when nothing matches, sets the tank capacity from `stats().fluidCapacity()`, runs `FluidSideMover.tick(level, pos, this, 8)` for a Pull back, and sends the contents state (D7) when it changed.
- `acceptsInput(slot, stack)`: some recipe `accepts` it and `KindSlots.accepts` over the input slots. `MachineBlockEntity.isItemValid` routes both the menu slots and automation (`automationAccepts`, `pullInputs`) through it, so the rule holds for every inserter.
- Items layer: defaults `TOP`, `BACK`, `LEFT`, `RIGHT` Input, `BOTTOM` Auto, `FRONT` fixed Push, as the Mill.
- Fluids layer: `sideTanks()` = one `SideTank(TankRole.FILL_ONLY)`, `sideSlots()` = the tank, `fluidModes()` = `SideConfig.INBOUND_MODES` (Input, Pull, Off; Input first, so the default), `tankFluid(0)` the tank's fluid, `sideTankAccepts` the validator. `fluidSides()` overrides the base: after the base builds it (front locked), it also locks `TOP` and `BOTTOM` (`FluidSides.lock` sets Off on All), so the back, left and right are fluid sides (decided with the user 2026-10-30, so a driver on the back never blocks the water). With one tank, the side panel shows no tank chip (existing rule).
- Capabilities in `MachineEvents`: `Capabilities.Item.BLOCK` through `itemCapability(side)`, `Capabilities.Fluid.BLOCK` through `FluidSideHandler.of(mixer, side)` (null on locked and null sides; fill only from the tank role, so automation never drains), `ServusFoundationCapabilities.DRIVE` through `drive(side)` on `BACK`, `LEFT`, `RIGHT` and null.
- Stats: no `MachineDefaults` entry, like the Mill (the docs lab kinds do not fit). Code default `MachineStats.crafter(1, 0, 0, 1)` with fluid capacity 4000, through a new factory `MachineStats.fluidHolder(int fluidCapacity)`; data `data/servus_foundation/machine/mixer.json` `{"fluid_capacity": 4000}` read by `MachineStatsManager` as for every machine. A smaller capacity from data never deletes fluid: the tank keeps what it holds and takes nothing until it is below the new capacity.
- Menu data: `dataCount()` `DATA_COUNT_FLUID`; `DATA_PROGRESS` work collected, `DATA_WORK_TICKS` the recipe's work, `DATA_FLUID_AMOUNT`, `DATA_FLUID_CAPACITY`, `DATA_FLUID_ID` (`BuiltInRegistries.FLUID.getId`, as `PumpLogic.data`).
- Saved: `Work`, the tank as `Tank` (`FluidSlot.save/load`); the base saves items and sides. Breaking drops the items through the base; the fluid is lost (spec).

### D5 Mixer block, bucket and screen
`MixerBlock extends MachineBlock`, properties without light, `noOcclusion`, shape from `MixerParts.shapeParts()` through `PartShapes`. `useItemOn`: a fluid container (`Transfer.isFluidContainer`) calls `FluidUtil.interactWithFluidHandler(player, hand, pos, mixer.tank())` on the server (NeoForge 26.1.2.116 `net.neoforged.neoforge.transfer.fluid.FluidUtil`, the overload with a `ResourceHandler<FluidResource>`, as `FluidStationBlock`) and succeeds; anything else goes on to the machine's usual handling (carrot, open screen). The hand gets the tank itself, so a bucket fills (validator) and an empty bucket drains a full 1,000 mB; automation gets only the fill only side handler. A client ticker drives the paddle `Flywheel` from `LIT`, as `MillBlock`.

`MixerMenu`: inputs at (34,26), (52,26), (34,44), (52,44) as `MachineSlot`s, output `OutputSlot` at (116,35); client data `clientData(DATA_COUNT_FLUID)`. `MixerScreen extends MachineScreen` with `new Layout(false, true, false, false, true, false, gui("mixer"))` (arrow and side panel; not the base's pump gauge, whose status line speaks of water sources) and its own `FluidGauge(16, 52)` at (148,17) with the tooltip from `menu.fluid()` and `menu.fluidCapacity()`. The base screen enables the Fluids tab because `fluidSides()` is not null. Registered in `MachineClient`.

### D6 Look and models (built facing north, front at z 0, pixels)
`MixerParts`, pure float boxes, shared by shape, datagen and renderer:
- `BASE` heavy iron 0,0,0 to 16,4,16.
- `FLOOR` steel 2.5,4,2.5 to 13.5,5,13.5, the tub's floor.
- Tub walls steel, 1.5 thick, outer 1 to 15, y 4 to 13: `WALL_FRONT` 1,4,1 to 15,13,2.5; `WALL_BACK` 1,4,13.5 to 15,13,15; `WALL_LEFT` 1,4,2.5 to 2.5,13,13.5; `WALL_RIGHT` 13.5,4,2.5 to 15,13,13.5. Inner faces painted steel too.
- Rim brass, y 13 to 14, overhanging 0.5 outside: `RIM_FRONT` 0.5,13,0.5 to 15.5,14,2.5; `RIM_BACK` 0.5,13,13.5 to 15.5,14,15.5; `RIM_LEFT` 0.5,13,2.5 to 2.5,14,13.5; `RIM_RIGHT` 13.5,13,2.5 to 15.5,14,13.5.
- `BRIDGE` steel 1.5,14,7 to 14.5,15,9, resting on the rim across the tub.
- Turning about the vertical axis through (8, 8), standalone model `block/mixer_paddle`: `HUB` brass 6.5,15,6.5 to 9.5,16,9.5 on the bridge; `SHAFT` steel 7.25,6.5,7.25 to 8.75,14.9,8.75, its top hidden inside the bridge; `BLADE_A` heavy iron 3.5,6,7.5 to 12.5,10,8.5; `BLADE_B` heavy iron 7.5,6.005,3.5 to 8.5,9.995,12.5, a hair lower at the top and higher at the bottom. A blade corner reaches 4.53 from the axis, inside the tub's 5.5, so the paddle never touches a wall at any angle.
- `CHUTE` steel 6,0.5,-2 to 10,3.5,0 with `mill_chute` on its front face, as the Mill. It sits one pixel lower than the Mill's, so its front face maps the mouth's pixels (u 6 to 10, v 11.5 to 14.5) explicitly.
- `PORT` brass 11.5,5.5,15 to 14,8.5,16.5, a stub off centre on the back wall, with `mixer_port` on its back face, mapped to the mouth's pixels (u 6.75 to 9.25, v 6.5 to 9.5). Off centre so a Hand Crank flange (x 5 to 11) or a motor coupling (x 6 to 10) on the back never meets it.
- Contents layer (renderer only): one up face 2.5,9,2.5 to 13.5,9,13.5.
- Crank flange and motor coupling reach one pixel into the driven block; the walls are inset by one pixel, so they meet the wall's outer face as they meet the Mill's housing.

Overlapping box pairs and their planes (CLAUDE.md "Models and textures"):

| Pair | Shared plane? | How |
|---|---|---|
| base / floor, base / walls | no | they start on the base top (y 4), faces back to back |
| floor / walls | no | floor ends at the walls' inner faces (2.5, 13.5), back to back |
| wall / wall | no | side walls end at the front and back walls' inner faces (z 2.5, 13.5) |
| walls / rim | no | rim starts on the wall tops (y 13), back to back; rim inner edges meet wall inner faces in a line only |
| rim / rim | no | side rims end at the front and back rims (z 2.5, 13.5) |
| rim / bridge | no | bridge starts on the rim top (y 14) |
| bridge / hub | no | hub starts on the bridge top (y 15), back to back at any angle |
| bridge / shaft | no | shaft faces at 7.25 and 8.75, bridge faces at z 7 and 9, y 14 and 15; shaft top 14.9 inside |
| shaft / blades | no | shaft faces at 7.25 and 8.75, blade faces at 7.5 and 8.5; shaft bottom 6.5 inside the blades |
| blade A / blade B | no | B's top 0.005 lower and bottom 0.005 higher; side faces at different planes |
| blades / floor, walls | no | blades start at y 6 above the floor top (5) and stay 1 px inside the walls |
| contents layer / anything | no | nothing else has a face at y 9 |
| chute / base | no | chute ends at the base front (z 0) |
| port / back wall | no | port starts at the wall's outer face (z 15), back to back |
| port / crank flange, motor coupling | no | port at x 11.5 to 14, flange to x 11, coupling to x 10 |

### D7 Contents layer and its sync
`getUpdateTag` adds `MixFluid` (the tank's fluid id, absent when empty) and `MixItems` (any input slot not empty) to the base's tag; `loadAdditional` reads them into client fields. The server compares the pair after each tick with the last sent one and calls `level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS)` only when it changes, not on every amount change. `MixerRenderer` (a render state renderer like `MillRenderer`, so it also draws the side markers) draws the paddle model turned by the `Flywheel` angle (`ModelPoses.rotateAbout`, `ModelKeys.submit`; the paddle and the layer are symmetric about the axis, so no facing turn), and the layer through `submitCustomGeometry`: with a fluid, a `FluidBox` up face with `FluidSprites.still` and `FluidSprites.tint(level, pos, fluid)` on the translucent block sheet; with items only, the same face with the block atlas sprite `servus_foundation:block/mixer_mix` on the cutout block sheet. The fluid level is not shown by height; the gauge tells the amount.

### D8 Textures, crafting recipe, JEI
- `tools/mill-textures/gen.py` gains `block/mixer_mix.png` (grainy grey brown), `block/mixer_port.png` (brass ring, dark mouth) and `gui/mixer.png` (from `gui/mill.png`: input slot replaced by a 2x2 grid at x 33 to 69, y 25 to 61, and a 18x54 gauge frame at 147,16). Reused: `heavy_iron`, `electric_pump_steel`, `brass`, `mill_chute`.
- Crafting (example): ` S ` / `ICI` / `III` with a Stick, a Cauldron and Iron Ingots.
- JEI `integration/jei/MixingRecipeCategory`: up to four item slots in a row, a fluid slot when the recipe has one (`addFluidStack(fluid, amount)` for each of `ingredient().fluids()`, as Delicacies' `VesselRecipeCategory`), the animated arrow, the output slot, "%s work" below; registered with `ServusRecipes.all(level, MIXING)` and the Mixer as catalyst in `ServusFoundationJeiPlugin`.

### D9 Tests
- JUnit, pure: `SlotMatcherTest` (the Delicacies cases), `KindSlotsTest` (same kind slot, empty slot when no other holds it, refused when another slot holds it, refused into a slot of another kind).
- GameTests `machine/mixer/MixerGameTests`: `motorMixesMud`, `mixerWithoutDriveDoesNothing`, `noWaterNoMud`, `coarseDirtLeavesTheWater`, `concretePowderPerColour`, `leftoverItemBlocksTheMix`, `outputFullWantsNoWork`, `oneKindPerSlot`, `refusesUnusedItems`, `fluidOnlyThroughTheBack`, `automationNeverDrains`, `bucketPoursInAndOut`, `tankRefusesLava`, `pullBackFillsFromTank`, `driveOnlyOnBackAndSides`, `crankTurnsAnEmptyMixer`, `hopperAboveChestInFront`, `chuteRefusesInput`, `mixingRecipesShipped`, `shippedMixingRecipesDoNotCompete` (for each shipped recipe, its least contents in an empty Mixer match only that recipe).

**PLAN.md decisions settled:** none beyond the item itself; slot count 4, tank 4,000 mB from data, the matching rule and the fluid sides (back, left, right) are recorded here.

Pure Java: `SlotMatcher`, `KindSlots`, `MixerParts` (box numbers). Minecraft-touching: recipe, block, block entity, menu, screen, renderer, datagen, JEI; GameTests and the play check.

## Risks / Trade-offs

- [A driver on the back covers the brass port stub] → Fluid also enters from the left and right, so a driver on any side leaves a fluid side free.
- [Two copies of `SlotMatcher` until the Delicacies and Brewery changes are archived] → Identical code and tests; the switch is a two-import follow-up recorded in proposal non-goals.
- [Dry recipe sharing items with a wet one] → Wet wins (D1); packs can override by id.
- [The fluid is lost when the Mixer is broken] → Same as dropping a filled machine elsewhere in the family; an empty bucket can take it out first.
- [Shared plane between the rim's inner edge and the wall's inner face] → They meet edge to edge (rim above y 13, wall below), no overlapping area, so nothing flickers.
- [Contents sync on change only] → A tank that drains to empty or the last item leaving changes the pair and sends one update.

## Assumptions

- Work examples: Mud and Coarse Dirt 100 (four crank turns, about 3 s by hand, 1 s with a motor), Concrete Powder 200.
- The tank capacity is a machine stats field, so packs set it in `machine/mixer.json`.
- Docs pages follow with the next Foundation release, not in this change.
