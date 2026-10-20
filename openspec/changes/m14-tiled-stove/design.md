## Context

See proposal.md for the why. Decided in the explore of 2026-10-20 (`temp/delicacies-brief.md`): the Tiled Stove is the only stove, a green tiled multiblock formed with the Configuration Carrot, smallest 2x1, cheap for day one, one firebox heats every cooking spot on top; Pot (family steel) and Pan (iron with a wooden handle) sit on top; a Cutting Board does the prep; every kitchen block works by hand and through the standard item and fluid capabilities; hunger and saturation live in datapack JSON; inside Delicacies a small firebox interface answers "am I hot", FE is the later shared interface of the Heating Element. Look: `temp/delicacies-mockup.html` option C/D (`kachel`, `kachel_door`, `herd_top` sprites in `temp/delicacies-mockup-src/sprites.py`, the `topf` and `pfanne` boxes in `template.html`).

`:delicacies` has the crops, a creative tab, datagen for models, loot, tags, data maps and lang, GameTests and JUnit tests. Sources checked: `minecraft-patched-26.1.2.116-sources.jar`, `neoforge-26.1.2.116-sources.jar`, and the family code named below.

## Goals / Non-Goals

**Goals:** a playable day one kitchen: tiles, a formed stove with a fuel fire, Pot, Pan, Cutting Board and Knife, data driven recipes and food values, automation through capabilities, JEI, and a firebox contract the Brewery and the Heating Element build on.

**Non-Goals:** see proposal.md.

## Decisions

### D1 Firebox contract and pure fire logic (package `firebox`)
No Minecraft imports in the pure part, JUnit tested:
- `Firebox`: what a cooking spot sees. `boolean isHot()` and `void demandHeat()` (the spot has something to cook this tick).
- `HeatSource`: the seam inside a firebox. `void tick(boolean demanded)`, `boolean hot()`, `float heat()` (0 to 1; a gauge later shows it), plus `save`/`load` of its ints through plain getters and a restore method. The stove's block entity holds one `HeatSource` and never knows which kind it is.
- `FuelFire implements HeatSource`: a wood fire. It takes a `FuelSupply` (`int burnNext()`: takes one fuel item and returns its burn ticks, 0 when there is none). `tick`: a burning fire burns one tick down; a fire that is out and demanded asks the supply for the next item and burns it for its full time. `hot()` while burn ticks remain; `heat()` is 1 while hot, else 0. So a wood fire is hot at once and cold as soon as the fuel ends, and an item once lit burns out even when the demand stops, like a furnace.

`m14-heating-element` adds a second `HeatSource` (FE in, a heat value that climbs while powered and sinks slowly without power, `hot()` above a threshold) and picks it when the element is installed. Cooking spots, the capability and the stove need no change for that.

Cooking spots find a firebox through a NeoForge block capability, `DelicaciesCapabilities.FIREBOX = BlockCapability.createVoid(id("firebox"), Firebox.class)` (verified: `BlockCapability#createVoid(Identifier, Class)`), asked on the block below the spot. A vessel keeps a `BlockCapabilityCache.create(FIREBOX, serverLevel, pos.below(), null)` (verified in `capabilities/BlockCapabilityCache`). The Brewery registers the same capability on its Brewhouse and reuses `FuelFire`; it depends on Delicacies, so it can.

### D2 Stove Tile and forming (package `stove`)
`StoveTileBlock extends BaseEntityBlock implements Wrenchable` with `FORMED`, `DOOR`, `LIT` (booleans) and `FACING` (horizontal, the stove's front). One block entity type for every tile, `StoveTileBlockEntity`.

Forming (`onWrench`, server side, not sneaking, tile not formed):
- Front: the clicked face when horizontal, otherwise `context.getHorizontalDirection().getOpposite()` (the side toward the player).
- Row direction: `front.getClockWise()`; the row axis is across the front.
- `StoveRow.find(IntPredicate looseTileAt, int maxLength)` (pure): walks from offset 0 both ways along the axis over loose tiles, at most `maxLength` steps each way, and returns `Formed(from, to)` or `TooShort` / `TooLong`. JUnit covers single tile, two tiles from either end, a gap, exactly max, max plus one, clicked in the middle.
- On success every tile gets `FORMED`, `FACING = front`, `DOOR` only on the clicked tile; the clicked tile is the master and saves the row (`along` direction, `from` offset, `length`); every other tile saves the offset to the master. Then `level.invalidateCapabilities` for each tile, a sound, and an overlay message (`message.servus_delicacies.stove.formed`, `.too_short`, `.too_long`).
- Sneaking carrot: pops the tile's item and removes the block, like the Crafting CPU.

Unforming: `StoveTileBlockEntity#preRemoveSideEffects` (verified in 26.1 `BlockEntity`, the CPU uses it) asks the master to unform: every other tile goes back to the default state and forgets its master, the fuel drops at the master, capabilities are invalidated. Setting `FORMED`/`LIT` keeps the block entity because the block stays the same.

Stats: `kitchen/KitchenStats(int stoveMaxLength, int potCapacity)` with an optional field `Partial`, loaded by `KitchenStatsManager` (`SimpleJsonResourceReloadListener` on `data/<ns>/kitchen/`, file id `servus_delicacies:stats`, registered with `AddServerReloadListenersEvent`), the same shape as Foundation's `DriveStatsManager`. Shipped `kitchen/stats.json`: `stove_max_length` 4, `pot_capacity` 4000. The minimum length 2 is a rule, not a balance number.

### D3 Firebox on the master tile
The master tile holds `ItemSlots(1)` for fuel (valid: `stack.getBurnTime(null, level.fuelValues()) > 0`, as Foundation's `MachineBlockEntity`), a `FuelFire` whose supply takes one item, leaves its crafting remainder (`getCraftingRemainder`, as `BurnerMachineBlockEntity`) and returns `getBurnTime(RecipeType.SMELTING, fuelValues)`, and a demand flag. Server ticker only on the master: `heat.tick(demanded)`, clear the flag, and when `hot()` changed set `LIT` on every tile of the row. Fuel is saved with the fire's burn ticks.

Capabilities on every formed tile (`RegisterCapabilitiesEvent`): `FIREBOX` returns the master's firebox; `Capabilities.Item.BLOCK` returns a view of the master's fuel slot that inserts only fuel and extracts only what is not fuel (an empty bucket). A loose tile returns null for both.

Hand use: a fuel item on a formed tile goes into the fuel slot; an empty hand shows `message.servus_delicacies.stove.status` (lit or cold, fuel count); the carrot passes to `onWrench`.

Look while lit: light level 13 on the door tile (`lightLevel` from `FORMED && DOOR && LIT`), the door's lit texture is an animated two frame flicker (`kachel_door_lit.png.mcmeta`), the plates show `herd_top_lit`, and `animateTick` on the lit door tile spawns a small flame and smoke at the door and plays the furnace crackle now and then.

### D4 Vessels (package `cooking`)
`VesselBlock` (abstract, `FACING`, `COOKING` boolean, shape from parts) with `PotBlock` and `PanBlock`; `VesselBlockEntity` (abstract: 4 input slots, 1 output slot, progress, cached recipe) with `PotBlockEntity` (adds a `FluidSlot` sized from `pot_capacity`) and `PanBlockEntity`.

Recipes: `VesselRecipe` (interface: `ingredients()` as `List<SizedIngredient>`, `fluid()` as `Optional<SizedFluidIngredient>`, `result()`, `time()`) with records `PotRecipe` (fields `ingredients`, optional `fluid`, `result`, `time`) and `PanRecipe` (no fluid field), both `Recipe<VesselInput>`, `isSpecial`, `PlacementInfo.NOT_PLACEABLE`, like Foundation's `MillingRecipe`. `VesselInput(List<ItemStack> items, FluidStack fluid) implements RecipeInput`. Codecs: `SizedIngredient.NESTED_CODEC.listOf()`, `SizedFluidIngredient.CODEC` (verified: `{"ingredient": "minecraft:water", "amount": 250}`), `ItemStackTemplate.CODEC`. Types `servus_delicacies:pot`, `servus_delicacies:pan` and `servus_delicacies:cutting`, synced to clients with `ServusRecipes.syncToClients` so JEI and the client side insertion checks agree.

Matching: `SlotMatcher.match(wants, slots, isEmpty)` (pure, generic over the slot content, JUnit tested): finds a distinct slot for every requirement by backtracking (at most four slots) and fails when a non empty slot is left over. `SizedIngredient#test` is count sensitive, so it is the requirement. Fluid: `SizedFluidIngredient#test` on the tank.

Tick (server): the cached recipe is looked up again after any content change. No runnable recipe (no match or the result does not fit): progress 0, not cooking. Otherwise demand heat from the firebox below; while it is hot, count up and on `time` take the counts from the matched slots, drain the fluid amount and insert the result. `COOKING` follows "has a recipe and is hot"; it is set only when it changes.

Hand use (`useItemOn`): a fluid container on the Pot goes through `FluidUtil.interactWithFluidHandler` (as Foundation's Fluid Tank, `Transfer.isFluidContainer`); an item some recipe of the vessel takes (ignoring count) goes into the inputs with `insertItemStacked`; anything else passes. Empty hand (`useWithoutItem`): the output, or the last filled input, into the player's inventory (`Inventory#placeItemBackInInventory`).

Automation: `kitchen/KitchenSlots`, a `LegacyItemHandler` view (Foundation) over an `ItemSlots` with an insert predicate per slot and the set of extractable slots; vessels insert into the inputs only items some recipe takes and extract only the output. The Pot's `FluidSlot` validator takes only fluids some pot recipe uses. Registered on every side and `null`.

Client: block entity update tag with slots, fluid and progress (`ValueIo.write`, `ClientboundBlockEntityDataPacket`), sent with `sendBlockUpdated` on content change only. `VesselRenderer` (a Foundation `BufferedBlockEntityRenderer`) lays the input stacks flat in a 2x2 grid and the result in the middle above them, and draws the Pot's fluid surface with `FluidBox` and `FluidSprites`, rising with the fill. Steam: `animateTick` while `COOKING`, `ParticleTypes.WHITE_SMOKE` (verified in 26.1 `ParticleTypes`) rising slowly from the vessel's top.

### D5 Cutting Board (package `board`)
`CuttingBoardBlock` (`FACING`, thin shape) and `CuttingBoardBlockEntity` with two slots: 0 the stack to cut, 1 the pieces. `CuttingRecipe(Ingredient ingredient, Ingredient tool, ItemStackTemplate result) implements Recipe<CuttingInput>` with `CuttingInput(ItemStack item, ItemStack tool)`. Datagen writes `tool` as `#c:tools/knife` (a common tag other kitchen mods use too). A knife click finds the recipe, checks the pieces fit, takes one item, adds the result, plays a chop sound with item particles of the cut item, and costs one durability (`ItemStack#hurtAndBreak(int, LivingEntity, InteractionHand)`, verified). Placing: an item some cutting recipe takes, when the cut slot is empty or holds the same item. Empty hand: pieces first, then the item to cut. Capabilities through `KitchenSlots`: insert into slot 0 only for cuttable items, extract only slot 1. `CuttingBoardRenderer` lays the item to cut flat in the middle and the pieces beside it.

`kitchen_knife` is a plain `Item` with `durability(250)` in the tag `c:tools/knife`.

### D6 Food values in a data map (package `food`)
Verified in 26.1: the item `FOOD` component (`FoodProperties`) and `CONSUMABLE` are default components set in code; datapacks cannot change them. So the cooked foods are `DelicaciesFoodItem`s with only the `CONSUMABLE` component (`Consumables.DEFAULT_FOOD`, the eat animation and sound) and their food values in a NeoForge data map, `DelicaciesDataMaps.FOOD = DataMapType.builder(id("food"), Registries.ITEM, FoodProperties.DIRECT_CODEC).synced(FoodProperties.DIRECT_CODEC, false).build()` (verified: `DataMapType.builder`, `synced`, `RegisterDataMapTypesEvent#register`; `FoodProperties.DIRECT_CODEC` reads `nutrition`, `saturation`, `can_always_eat`). The file is `data/servus_delicacies/data_maps/item/food.json`. `DelicaciesFoodItem#use` reads the entry (`stack.typeHolder().getData(FOOD)`): no entry passes, a player who cannot eat (`Player#canEat(canAlwaysEat)`) fails, otherwise it starts eating; `finishUsingItem` calls the entry's `FoodProperties#onConsume` (hunger, saturation, burp) before the vanilla consume. Example values: Boiled Potatoes 5 and 6.0, Fried Egg 4 and 4.8, Fried Onions 3 and 3.6, Fried Potatoes 8 and 12.8.

### D7 Models and look (built facing north, pixels)
Stove Tile models (datagen): `stove_tile` (cube all `kachel`), `stove_tile_formed` and `stove_tile_formed_lit` (sides and bottom `kachel`, top `herd_top` / `herd_top_lit`), `stove_tile_door` and `stove_tile_door_lit` (north `kachel_door` / `kachel_door_lit`, rotated by `FACING`). Full cubes, one element each, nothing overlaps.

Pot (`VesselParts.POT_*`), steel `servus_foundation:block/electric_pump_steel`, handles `servus_foundation:block/heavy_iron`:
- `POT_BOTTOM` 2,0,2 to 14,1,14; walls on it: `POT_NORTH` 2,1,2 to 14,10,3; `POT_SOUTH` 2,1,13 to 14,10,14; `POT_WEST` 2,1,3 to 3,10,13; `POT_EAST` 13,1,3 to 14,10,13.
- `POT_HANDLE_WEST` 0.5,7,7 to 2,8.5,9; `POT_HANDLE_EAST` 14,7,7 to 15.5,8.5,9.

Pan, `servus_foundation:block/heavy_iron`, handle `minecraft:block/oak_planks`:
- `PAN_BOTTOM` 1,0,2 to 12,1,14; rims on it: `PAN_NORTH` 1,1,2 to 12,2.5,3; `PAN_SOUTH` 1,1,13 to 12,2.5,14; `PAN_WEST` 1,1,3 to 2,2.5,13; `PAN_EAST` 11,1,3 to 12,2.5,13.
- `PAN_HANDLE` 12,1.5,7.25 to 16,2.5,8.75.

Cutting Board: `BOARD` 2,0,3 to 14,1,13, `minecraft:block/birch_planks`.

Renderer parts: the Pot's fluid surface is an up face only at x 3 to 13, z 3 to 13, from y 1.5 to y 9.5 by fill; items lie at y 1.05 plus the item's half thickness in the Pan, on the fluid surface plus 0.3 px in the Pot (at y 1.3 when empty), on the board at y 1.05.

Overlapping box pairs and their planes (CLAUDE.md "Models and textures"):

| Pair | Shared plane? | How |
|---|---|---|
| pot bottom / each wall | no | walls start on the bottom top (y 1), no down faces; back to back |
| pot north or south wall / west or east wall | no | the side walls end at the north and south walls' inner faces (z 3, z 13), back to back; tops abut along an edge only |
| pot handle / wall | no | handles end at the wall face (x 2, x 14), no inner face |
| fluid surface / walls, bottom | no | an up face only, inside the walls, at a height no wall top or bottom uses (1.5 to 9.5, never 1 or 10) |
| pan bottom / each rim | no | rims start on the bottom top (y 1), no down faces |
| pan rims among each other | no | west and east rims end at the north and south rims (z 3, z 13) |
| pan handle / east rim | no | handle starts at the rim's outer face (x 12), no west face |
| items in a vessel / bottom, fluid | no | items lie 0.05 px or more above the face below them |
| board / item on it | no | items lie 0.05 px above the board top |
| vessel or board / stove top | no | they start at y 0 of the block above, back to back with the stove's top face |

Item models: Pot, Pan and Cutting Board use their block model; Stove Tile its loose cube.

### D8 Textures
`tools/delicacies-textures/gen.py` gains the mockup's `tex_noise`, the `IRON`, `BRASS` and `TILE` ramps and the sprites `kachel`, `kachel_door` (cold, lit, and the second flicker frame), `herd_top` (cold and lit), written to `textures/block/` (`stove_tile`, `stove_tile_door`, `stove_tile_door_lit` as a 16x32 two frame strip with an `.mcmeta`, `stove_plate`, `stove_plate_lit`). New item sprites: Boiled Potatoes, Fried Egg, Fried Onions, Fried Potatoes, Chopped Onion, Shredded Cabbage, Kitchen Knife (steel blade, wooden handle). Pot, Pan and Board icons come from their block models.

### D9 Recipes and tags (datagen)
- Stove Tile x4: eight Clay Balls around `#minecraft:stone_crafting_materials` (`CCC`/`CSC`/`CCC`).
- Pot: `I I`/`III` Iron Ingots. Pan: `II`/`S ` (Iron Ingots, Stick). Kitchen Knife: `I`/`S`. Cutting Board: shapeless two `#minecraft:wooden_slabs` and a Stick.
- Cutting: `cutting/shredded_cabbage` (Cabbage to 2 Shredded Cabbage), `cutting/chopped_onion` (Onion to 2 Chopped Onion).
- Pot: `pot/boiled_potatoes` (2 Potatoes, 250 mB water, 200 ticks, 2 Boiled Potatoes).
- Pan: `pan/fried_egg` (1 `#c:eggs`, 100 ticks), `pan/fried_onions` (2 Chopped Onion, 120 ticks), `pan/fried_potatoes` (2 Boiled Potatoes and 1 Chopped Onion, 160 ticks, 2 Fried Potatoes).
- Tags: Kitchen Knife in `c:tools/knife` and `c:tools`; cooked foods in `c:foods`; Stove Tile mineable with a pickaxe; vessels mineable with a pickaxe; board with an axe. Loot: every block drops itself.

### D10 JEI
`delicacies/build.gradle` adds the JEI common and NeoForge API as `compileOnly`, like Shuttles. `ServusDelicaciesJeiPlugin` registers three categories, `PotRecipeCategory` (up to four item slots, the fluid, an arrow, the result, time), `PanRecipeCategory` (items, arrow, result, time) and `CuttingRecipeCategory` (item, knife, result), with the Pot, Pan and Cutting Board as catalysts and the Stove Tile as a further catalyst for pot and pan.

### D11 GameTests
New classes registered in `DelicaciesGameTests`: `StoveGameTests` (carrot forms a row of two with the door on the clicked face; a single tile is refused; a row longer than the maximum is refused; breaking a tile unforms the rest and drops the fuel; fuel through a hopper; a cold stove with fuel burns nothing while no vessel cooks), `CookingGameTests` (Pot cooks Boiled Potatoes on a fuelled stove and uses Coal; no cooking without fuel; no cooking on a loose tile; a stray item blocks the recipe; hopper above the Pot feeds it; a hopper under a Pan pulls only the result; water bucket fills the Pot; empty hand takes the result; Fried Potatoes are eaten with their data map values and refused by a full player), `CuttingBoardGameTests` (knife cuts one onion per click and wears the knife; no cut without a knife; refuses Cobblestone; hopper fills the board; recipes are shipped).

### Settles from PLAN.md
PLAN.md lists `m14-tiled-stove` with the stove, firebox, Pot, Pan, Cutting Board and the firebox interface; no open decision of section 5 is touched. The food value decision (data map instead of the item component) is new and is recorded here for `m14-bakery`, `m14-meat-dishes` and `m14-sides` to follow.

## Assumptions

- Stove Tile recipe: eight Clay Balls around one stone crafting material makes four tiles; no dye, the glaze is the look.
- The door goes on the clicked tile; forming clicks on the top face put the door toward the player. The row is the straight line across the front; tiles joined in other directions are ignored.
- Maximum length 4 (data); minimum 2 (rule).
- The fire lights only on demand and burns a lit item down even when cooking stops, like a furnace; burn times are the vanilla furnace ones.
- Fuel goes in through every face of every tile; the top faces are usually covered by vessels.
- Vessels hold four input stacks and one output stack; the Pot's tank holds 4000 mB (data). Vessels can stand on any block but cook only on a hot firebox.
- An empty hand takes the whole output stack; with no output it takes back the last filled input.
- The Cutting Board holds one stack to cut and one stack of pieces; one item per knife click; the knife has 250 durability (an item property, like vanilla tools).
- Foods are eaten only through `DelicaciesFoodItem`; mods that read the vanilla `FOOD` component for tooltips (AppleSkin and the like) do not see these values.
- No flue pipe on the stove (option C's pipe is left out).
- Only the minimal foods: Boiled Potatoes, Fried Egg, Fried Onions, Fried Potatoes; ingredients Chopped Onion and Shredded Cabbage.

## Risks / Trade-offs

- [Food values outside the `FOOD` component] Other mods' food tooltips miss them -> a later change can add our own tooltip line from the data map; the gameplay is correct.
- [Removal that skips block entity side effects (some world editors)] A stove could stay half formed -> the vessel and fuel code only trust a master that still names the tile; a carrot click on a stray formed tile without a master unforms it.
- [Recipe lookups every tick] Each vessel caches its recipe and looks it up only after a content change.

## Migration Plan

None. Nothing is released.
