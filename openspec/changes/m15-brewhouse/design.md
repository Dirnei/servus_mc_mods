## Context

See proposal.md for the why. Decided in the explore of 2026-10-20 (`temp/delicacies-brief.md`): one bricked firebox multiblock 3x2 with the 2x2 copper Brew Kettle (dome, vent pipe) on the left and the Malt Kiln (grate tray) on the same firebox, which makes light or dark malt; the firebox is the Delicacies one; FE through the later Heating Element is the shared interface, no heat API. Look: `temp/delicacies-mockup.html` option D, function `sudhaus` in `temp/delicacies-mockup-src/template.html` (boxes in pixels, front on +z there).

`m15-brewery-project`, `m15-crops` and `m15-malting` (applied, not archived) left `:brewery` with barley, hops, the Malting Floor, the malt and grist items, `tools/brewery-textures/gen.py`, datagen and GameTests. `m14-tiled-stove` (applied, not archived) left the firebox types in `io.github.dirnei.mc.servus.delicacies.firebox` public: `Firebox`, `HeatSource`, `FuelFire`, `FuelSupply`, `DelicaciesCapabilities.FIREBOX`, plus `kitchen/KitchenSlots` and `cooking/SlotMatcher`. Brewery depends on Delicacies, so it uses them as they are; this change edits no Delicacies source. Sources checked: `minecraft-patched-26.1.2.116-sources.jar` and the family code named below.

## Goals / Non-Goals

**Goals:** a formed Brewhouse that burns fuel through the Delicacies fire logic, a kettle that brews three worts from data recipes, a kiln with a pale and a dark mode, hand use and capabilities for everything, JEI, the look of option D without z-fighting, JUnit tests for the pure parts and GameTests for the behaviour.

**Non-Goals:** see proposal.md, Non-goals.

## Decisions

### D1 Parts and layout (package `brewhouse`)
Two blocks: `BrewhouseBricksBlock` (id `brewhouse_bricks`) and `BrewKettleBlock` (id `brew_kettle`), both `BaseEntityBlock implements Wrenchable` with one shared block entity type `BrewhouseBlockEntity` (a `BlockEntityType` valid for both blocks).

Ten cells, given as column (0 to 2, left to right seen from the front), row (0 front, 1 back) and layer (0 bricks, 1 kettle). The pure `BrewhouseLayout` (no Minecraft imports) has `enum Cell` with the six brick cells `KETTLE_FRONT_LEFT (0,0,0)`, `KETTLE_FRONT_RIGHT (1,0,0)`, `KETTLE_BACK_LEFT (0,1,0)`, `KETTLE_BACK_RIGHT (1,1,0)`, `KILN_FRONT (2,0,0)`, `KILN_BACK (2,1,0)` and the four kettle cells `COPPER_FRONT_LEFT (0,0,1)`, `COPPER_FRONT_RIGHT (1,0,1)`, `COPPER_BACK_LEFT (0,1,1)`, `COPPER_BACK_RIGHT (1,1,1)`, each with its `Kind` (`BRICK`, `KETTLE`). `find(Kind clicked, Probe probe)` tries every cell of the clicked kind, front row first, and returns the first cell for which `probe.looseAt(dc, dr, dl, kind)` holds for all ten cells, offsets taken from the clicked block. JUnit covers every clicked cell, a missing kettle block, a kettle block on the wrong side, a missing brick, and a brick layer of three by three (the front row wins).

In the world: `front` is the clicked face when horizontal, else `context.getHorizontalDirection().getOpposite()` (the stove's rule); `right = front.getCounterClockWise()`, `back = front.getOpposite()`; a cell lies at `clicked + right * dc + back * dr + up * dl`. The master is `KETTLE_FRONT_LEFT`.

### D2 Block states and forming
`BrewhouseBricksBlock`: `FORMED`, `PART` (`EnumProperty<BrewhousePart>` limited to the brick cells with `EnumProperty.create(name, class, predicate)`, verified in 26.1 `EnumProperty`), `FACING` (horizontal, the front), `LIT`, `TRAY` (`TrayLook`: `EMPTY`, `GREEN`, `PALE`, `DARK`, only used on kiln cells). `BrewKettleBlock`: `FORMED`, `PART` (the kettle cells), `FACING`, `BREWING`. `BrewhousePart implements StringRepresentable` is the Minecraft side twin of `BrewhouseLayout.Cell` (`of(Cell)`, `cell()`).

Forming (`onWrench`, server, not sneaking, block loose): `BrewhouseForming.tryForm(level, pos, front)` runs `BrewhouseLayout.find` with a probe that accepts loaded, unformed blocks of the right kind; on success every cell gets `FORMED`, its `PART`, `FACING = front`, `LIT`/`BREWING` false, `TRAY = EMPTY`; the master block entity calls `becomeMaster()`, every other one `joinMaster(masterPos)` (saved as an offset); then `level.invalidateCapabilities` for each cell, `SoundEvents.STONE_PLACE`, and an overlay message (`message.servus_brewery.brewhouse.formed` or `.layout`).

Carrot on a formed part: a kiln cell toggles that tray's mode (D6); any other cell sends its status (D5); a formed cell without a valid master unforms itself (the stove's stray rule). Sneaking carrot: pops the block's item and removes it (unforms, as the stove).

Unforming: `BrewhouseBlockEntity#preRemoveSideEffects` (verified in 26.1 `BlockEntity`, the stove uses it) asks the master to unform: fuel, kettle inputs and both trays drop at the master, every other cell goes back to its block's default state and forgets the master, capabilities are invalidated. The fluids are lost. Formed parts return `PushReaction.BLOCK`.

### D3 Firebox reuse
The master holds `ItemSlots(1)` fuel (valid when `stack.getBurnTime(null, level.fuelValues()) > 0`), a `FuelFire` whose `FuelSupply` takes one item, leaves its crafting remainder and returns `getBurnTime(RecipeType.SMELTING, fuelValues)` (the stove's rule), a field `HeatSource heat = fire` (the seam the Heating Element replaces), a demand flag and a `Firebox` view (`isHot` from `heat.hot()`, `demandHeat` sets the flag). Only the master ticks (`getTicker` when `FORMED` and `PART == KETTLE_FRONT_LEFT`): kettle tick, tray ticks, then `heat.tick(demanded)`, clear the flag, and on a change of `hot()` set `LIT` on all six bricks. A `KitchenSlots` view over the fuel inserts only fuel and extracts only what is not fuel.

`DelicaciesCapabilities.FIREBOX` is registered on the block entity type and returns the master's firebox on every formed brick and null elsewhere, so a later `HeatSource` on FE and any mod that asks see the same firebox.

### D4 Brew Kettle
`BrewKettle` (not a block entity, owned by the master): `ItemSlots(4)` inputs with the vessel rule (one item kind per stack, only items some brewing recipe takes), a water `FluidSlot` (capacity `kettle_capacity`, valid for fluids some brewing recipe uses) and a wort `FluidSlot` (capacity `wort_capacity`), progress, a cached recipe looked up again after any content change.

Recipe: `BrewingRecipe(List<SizedIngredient> ingredients, SizedFluidIngredient fluid, FluidStackTemplate result, int time) implements Recipe<BrewingInput>`, `BrewingInput(List<ItemStack> items, FluidStack water)`. Codecs `SizedIngredient.NESTED_CODEC.listOf()`, `SizedFluidIngredient.CODEC`, `FluidStackTemplate.CODEC` (`{"id": ..., "amount": ...}`), stream codecs as `PotRecipe`. The result is NeoForge's `FluidStackTemplate` (verified in `neoforge-26.1.2.116-sources.jar`), the fluid twin of `ItemStackTemplate`: a `FluidStack` cannot be made before data components are bound, so datagen could not write it. `matches`: the fluid test on the water and `SlotMatcher.match` (Delicacies) over the four stacks. `isSpecial`, `PlacementInfo.NOT_PLACEABLE`, type `servus_brewery:brewing`, synced to clients with `ServusRecipes.syncToClients`.

Tick: no recipe or the wort tank cannot take `result` (`wort.fill(result, true) < amount`): progress 0, not brewing. Otherwise demand heat; while hot count up; at `time` extract the matched counts, drain the fluid amount from the water tank, fill the result. `BREWING` on the four kettle cells follows "has a runnable recipe and is hot", set only on change.

Capabilities on every kettle cell: `Capabilities.Item.BLOCK` is a `KitchenSlots` that inserts by the input rule and extracts nothing; `Capabilities.Fluid.BLOCK` is `KettleFluids extends LegacyFluidHandler` (Foundation): two tanks reported, the wort as tank 0 (the index `LegacyFluidHandler` fills and drains through, so pipes that drain by index find the wort) and the water as tank 1; `fill` goes to the water tank and every tank accepts what the water tank takes; both `drain`s come from the wort tank.

Hand use on a formed kettle cell (`useItemOn`): `Transfer.isFluidContainer` items go through `FluidUtil.interactWithFluidHandler(player, hand, pos, kettleFluids, null)` (as the Pot), so a water bucket fills the water tank and an empty bucket takes wort; an item the kettle accepts goes into the inputs; anything else passes. `useWithoutItem`: sneaking takes back the last filled input stack (`placeItemBackInInventory`), otherwise an overlay status (water, wort and its name, brewing or not).

### D5 Status
Bricks under the kettle, empty hand or carrot: `message.servus_brewery.brewhouse.lit` / `.cold` with fuel count and name, `.no_fuel` variants, `.loose` on a loose part. Kettle: `message.servus_brewery.kettle.status` (water mB, wort mB and name, `.brewing` or `.idle`).

### D6 Malt Kiln
`KilnTray` (owned by the master, two of them, index 0 `KILN_FRONT`, 1 `KILN_BACK`): `ItemSlots(1)` valid for items a kilning recipe takes (the NeoForge stacks handler already refuses a different item in an occupied slot), `extract` overridden to give nothing while the stack is still a kilning input (as `MaltingFloorBlockEntity.FloorSlot`), progress, `KilnMode mode` and the look of the last dried batch.

Recipe: `KilningRecipe(Ingredient input, ItemStackTemplate resultTemplate, int time, Optional<Dark> dark) implements Recipe<SingleRecipeInput>` with `Dark(ItemStackTemplate resultTemplate, int time)`:

```json
{"type": "servus_brewery:kilning", "input": "servus_brewery:green_malt",
 "result": {"id": "servus_brewery:pale_malt"}, "time": 600,
 "dark": {"result": {"id": "servus_brewery:dark_malt"}, "time": 600}}
```

Pure `KilnClock` (no Minecraft imports): `target(time, darkTime, mode)` (dark only when the recipe has a dark result), `step(progress, hot)` (counts only while hot), `done(progress, target)`; `KilnMode` (`PALE`, `DARK`, `next()`). JUnit covers both modes, a recipe without a dark result in dark mode, the pause while cold and the toggle.

Tick per tray with an input: demand heat; while hot step the progress; when done set the look (`DARK` when the dark result was made, else `PALE`) and replace the stack by the result with the same count. `onContentsChanged`: empty sets `EMPTY`; a kilning input restarts the progress and sets `GREEN`; anything else keeps the dried look. The look goes into `TRAY` on that kiln cell, set only on change.

Hand use on a formed kiln cell: an item a kilning recipe takes is spread (`stack.consume`), fuel goes to the firebox, an empty hand takes the whole stack. Carrot toggles the mode and tells `message.servus_brewery.kiln.mode.pale` or `.dark`. `Capabilities.Item.BLOCK` on each kiln cell is its tray's slot.

The mode is the player's choice (decision for the brief's open point): one carrot click per tray, kept with the tray, so hand play and pipes behave the same. A tray in dark mode does not give pale malt halfway.

### D7 Stats
`BrewhouseStats(int kettleCapacity, int wortCapacity)` with a `Partial` of optionals, loaded by `BrewhouseStatsManager` (`SimpleJsonResourceReloadListener` on `data/<ns>/brewhouse/`, file `servus_brewery:stats`, registered with `AddServerReloadListenersEvent`), the shape of `KitchenStatsManager`. Shipped `brewhouse/stats.json`: `kettle_capacity` 4000, `wort_capacity` 4000. Tank sizes are read when the block entity is made.

### D8 Wort fluids
`BreweryFluids` registers per wort (`pale_wort`, `dark_wort`, `wheat_wort`) a `FluidType` (`descriptionId fluid_type.servus_brewery.<id>`, `canConvertToSource(false)`), a `BaseFlowingFluid` source and flowing form, a `LiquidBlock` (copy of water) and a `BucketItem` (`craftRemainder(BUCKET)`, stacks to 1), exactly as Alchemy's `PrincipleFluids`. Client: `RegisterFluidModelsEvent` registers `FluidModel.Unbaked` with the vanilla water still and flow textures and a `FluidTintSource` per wort colour (as `AlchemyClient`): Pale Wort `#C8902A`, Dark Wort `#4A2410`, Wheat Wort `#D8A848`. Buckets use the NeoForge `neoforge:fluid_container` item model (as Alchemy). The liquid blocks get a particle only model.

### D9 Look (structure pixels and model slicing)
The whole Brewhouse is one model in structure pixels: `u` from the left 0 to 48, `v` from the front 0 to 32, `y` up. Boxes after `sudhaus`, turned so the front is `v = 0`:

| Part | u | v | y | Faces |
|---|---|---|---|---|
| `BRICKS` | 0..48 | 0..32 | 0..12 | `minecraft:block/bricks` on all faces |
| `KETTLE_DOOR` | 8..24 | -0.5..0 | 1..11 | front `#door` stretched over the whole door, other faces `heavy_iron`, no back face |
| `KILN_DOOR` | 34..46 | -0.5..0 | 2..9 | the same |
| `KETTLE` | 2..30 | 2..30 | 12..30 | `brew_kettle_copper`, no down face |
| `DOME` | 6..26 | 6..26 | 30..34 | copper, no down face |
| `PIPE` | 14..18 | 14..18 | 34..48 | copper, no down face |
| `TRAY` | 33..47 | 2..30 | 12..15 | sides `heavy_iron`, top `#tray`, no down face |
| `GRATE` | 33..47 | 2..30 | 15..15.5 | `kiln_grate` on top and sides, no down face |

The pipe ends at y 48, not at the mockup's 52: a block model element stops at 32 px above its block (vanilla element bounds -16 to 32), and the pipe belongs to the kettle cells of layer 1.

Pure `ModelSlicer` (no Minecraft imports) cuts each part at the inner cell borders (`u` 16 and 32, `v` 16, `y` 16) and maps a piece into its cell's model space, built facing north: `x = 16 - (u - 16 col)`, `z = v - 16 row`, `y - 16 layer`. Faces on a cut are dropped, so pieces of one part never draw faces against each other; outer borders are not cut, so the doors stick 0.5 px out of the front cells and the pipe rises into the space above the kettle. Sides map to directions: front north, back south, left east, right west. UVs follow the vanilla position rule, shifted by whole blocks into 0 to 16 (so textures line up across cells); a stretched front maps the whole part onto the texture. `clipped()` gives a piece cut to its block, for the block shapes. JUnit covers a part across two columns and two rows (cut faces dropped, outer faces kept), the mirrored `u` mapping, a part over two layers, the door's stretched UVs, UV shifting above 16 px, a part outside the layout (refused) and clipping.

Datagen builds every model from the slicer: per brick cell the parts that reach it, with `#door` cold or lit (on the door cells) and `#tray` per look (on kiln cells: `heavy_iron`, `kiln_green_malt`, `malt_floor`, `kiln_dark_malt`); per kettle cell its copper pieces; rotated by `ModelRotations.rotationY(FACING)`. Loose: Brewhouse Bricks are `minecraft:block/bricks` on the sides with `servus_foundation:block/heavy_iron` top and bottom; the Brew Kettle is a full cube of `brew_kettle_copper`. Shapes: the union of the clipped pieces turned with `PartShapes.shapeOf`; loose parts are full cubes.

Overlapping faces (CLAUDE.md "Models and textures"):

| Pair | Shared plane? | How |
|---|---|---|
| bricks top (y 12) / kettle, tray | no | kettle and tray draw no down face |
| kettle top (y 30) / dome | no | the dome draws no down face |
| dome top (y 34) / pipe | no | the pipe draws no down face |
| door back (v 0) / bricks front (v 0) | no | the door draws no back face |
| tray top (y 15) / grate (y 15.5) | no | 0.5 px apart, the grate draws no down face |
| tray sides / grate sides (u 33, 47; v 2, 30) | no overlap | same planes, but y 12..15 and 15..15.5 only touch along an edge |
| pieces of one part in neighbouring cells | no | faces on cuts are dropped |
| kettle sides / bricks sides | no | kettle sides at u 2, 30 and v 2, 30, bricks at u 0, 48 and v 0, 32 |
| door sides / bricks front | no | perpendicular |

The kettle, dome and pipe are copper on every face; the tray and the door edges use the family `heavy_iron`.

### D10 Particles and light
`LIT` gives light 13 on `KETTLE_FRONT_LEFT`, `KETTLE_FRONT_RIGHT` and `KILN_FRONT`. `animateTick`: lit door cells spawn `SMALL_FLAME` and `SMOKE` at their part of the door and now and then `FURNACE_FIRE_CRACKLE` (the stove's code); `COPPER_FRONT_LEFT` with `BREWING` spawns `ParticleTypes.CLOUD` rising from the pipe top (structure point u 16, v 16, y 48); a lit kiln cell with `TRAY = GREEN` spawns a faint `WHITE_SMOKE` over the grate now and then. Structure points go to the world through the slicer's mapping and `PixelBoxes.rotatedPoint`.

### D11 Textures
`tools/brewery-textures/gen.py` gains the mockup's `IRON`, `BRASS`, `COPPER` ramps and:
- `brew_kettle_copper`: copper sheet (noise over the copper ramp) with the mockup's riveted bands at rows 2 and 13 and seam dots. The mockup draws it over vanilla `copper_block.png`; the generator does not read vanilla files, so the base is noise.
- `brewhouse_door` (port of `herd_front(False)`), `brewhouse_door_lit` (16x32 strip of `herd_front(True)` and `herd_front_lit2`) with the flicker `.png.mcmeta` of the Tiled Stove door.
- `kiln_grate` (port of `grate`: an iron lattice, clear between).
- `kiln_green_malt` and `kiln_dark_malt` (`grain_floor` in the green and dark malt ramps); pale uses the existing `malt_floor`.

### D12 Recipes, tags, loot, lang (datagen)
- Brewhouse Bricks x8: eight `minecraft:bricks` around one Iron Ingot. Brew Kettle: `C C` / `CCC` Copper Ingots.
- Brewing: `brewing/pale_wort` (4 Pale Malt Grist, 1 Hops, 1000 mB water, 1000 mB Pale Wort, 400 ticks), `brewing/dark_wort` (4 Dark Malt Grist, 1 Hops), `brewing/wheat_wort` (2 Wheat Malt Grist, 2 Pale Malt Grist, 1 Hops).
- Kilning: `kilning/pale_malt` (Green Malt, 600 ticks, dark Dark Malt plus 600), `kilning/wheat_malt` (Green Wheat Malt, 600 ticks).
- Tags: both blocks `minecraft:mineable/pickaxe`. Loot: both drop themselves; liquid blocks none.
- Lang: blocks, buckets, fluid types, messages, JEI titles.

### D13 JEI
`brewery/build.gradle` adds the JEI common and NeoForge API as `compileOnly`, like Delicacies. `integration/jei/ServusBreweryJeiPlugin` registers `BrewingRecipeCategory` (item slots, the water, an arrow, the wort, time and "needs a burning Brewhouse") and `KilningRecipeCategory` (input, arrow, result, time, and the dark result with its extra time when present), catalysts Brew Kettle (brewing) and Brewhouse Bricks (both).

### D14 GameTests
`gametest/BrewhouseGameTests` (listed in `BreweryGameTests`), built with a helper that places the ten parts facing north at a fixed spot:
- `carrotFormsTheBrewhouse`, `clickOnAKettleBlockForms`, `missingKettleIsRefused`, `breakingAPartUnformsAndDrops`, `fuelThroughTheBricks`, `noWorkNoFire`.
- `kilnDriesGreenMaltToPale`, `kilnDarkModeMakesDarkMalt` (the tray's progress is loaded near the target through its saved data, as `MaltingGameTests` does), `kilnDriesWheatMaltInDarkMode`, `kilnPipesTakeOnlyDriedMalt`, `emptyHandTakesTheTray`.
- `kettleBrewsOnlyWhileHot` (cold for a while with a full recipe: no wort and progress 0; then coal: Pale Wort within the recipe time), `kettlePipesInAndOut` (items and water through the capabilities, wort out, water never), `kettleBucketsByHand`, `strayItemBlocksTheBrew`, `recipesAreShipped`.

### Settles from PLAN.md
PLAN.md lists `m15-brewhouse` with the firebox multiblock, Brew Kettle and Malt Kiln; no open decision of section 5 is touched. PLAN.md is not edited in this change; the decisions below are for `m15-beer`: three wort fluids (Pale, Dark, Wheat Wort) and the kiln mode by carrot.

## Assumptions

- The Brewhouse is ten blocks: six Brewhouse Bricks and four Brew Kettle blocks placed by the player; the Malt Kiln is the right column of bricks with no own block.
- The kettle is always on the left seen from the front; no mirrored build.
- The firebox stays a full block when loose and draws 12 px tall when formed, as in the mockup; the vent pipe is 14 px instead of 18 (element bounds).
- Recipes: eight Bricks blocks around one Iron Ingot give eight Brewhouse Bricks; five Copper Ingots give one Brew Kettle block.
- Kettle and wort tanks 4000 mB each (data); 1000 mB water to 1000 mB wort in 400 ticks per brew (data).
- Wheat Wort takes half wheat and half pale grist; every wort takes one Hops.
- Kiln: one stack per tray, two trays; the whole stack dries in one time (600 ticks pale, 600 more for dark, data); drying pauses while the fire is out; adding to a tray restarts it.
- Kiln mode per tray, toggled with the carrot, starts pale.
- Fuel goes in by hand on any brick and through the four bricks under the kettle; the kiln bricks' capability is the tray.
- Worts are tinted water, no own fluid textures.
- Fluids are lost when the Brewhouse is broken (as tanks of a broken machine are).

## Risks / Trade-offs

- [384 brick block states] Each is one model reference; the models themselves are about twenty. Acceptable for a ten block multiblock.
- [The vent pipe rises into the space above the kettle] A block placed there overlaps the pipe; it is 4 px wide in the middle and players see it.
- [Removal that skips block entity side effects] A part could stay formed alone -> the stray rule of the stove: the carrot unforms a formed part without a master, and capabilities trust only a master that still lists the part.
- [Recipe lookups] The kettle caches its recipe and looks again only after a content change; trays look up their recipe per tick only while they hold an input, by a single ingredient test.

## Migration Plan

None. Nothing is released.
