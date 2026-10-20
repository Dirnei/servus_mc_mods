## Context

See proposal.md for the why. The explore of 2026-10-20 (`temp/delicacies-brief.md`) fixed the Teller: plated meals as a placed block with portions, each empty hand right click eats one, the whole meal gives a short buff; Roast Pork with two dumplings and red cabbage, Schnitzel with potato salad (no lemon), the Weisswurst breakfast with its Weissbier only when Brewery is installed. The look is the mockup's section "Teller" (`temp/delicacies-mockup-src/template.html`, `DISHES`, sprites `plate` and `ceramic` in `sprites.py`).

The parts come from `m14-meat-dishes` (Roast Pork, Gravy, Schnitzel, Weisswurst, Sweet Mustard), `m14-sides` (Bread and Potato Dumpling, Braised Red Cabbage, Potato Salad) and `m14-bakery` (Pretzel). Patterns followed: the placed Leberkaese loaf (`meat/LeberkaeseBlock`, `LoafSlices`, `MeatModels`), the synced food data map `servus_delicacies:food`, appended datagen blocks, `tools/delicacies-textures/gen.py`. Sources checked: `minecraft-patched-26.1.2.116-sources.jar`, `neoforge-26.1.2.116-sources.jar`.

## Goals / Non-Goals

**Goals:** four meals with a simple, automatable assembly, eating per portion with every number in data, a soft Brewery hook without a dependency, and a model that follows the mockup without z-fighting.

**Non-Goals:** see proposal.md.

## Decisions

### D1 Assembly by crafting
Crafting is the simplest assembly: one shapeless recipe per meal, JEI shows it with no code, and autocrafting can make meals. Assembly on a placed plate by hand is left out (one way only). Recipes (datagen, shapeless):
- `roast_pork_plate`: Plate, Roast Pork, two `#servus_delicacies:dumplings`, Braised Red Cabbage, Gravy.
- `schnitzel_plate`: Plate, Schnitzel, Potato Salad.
- `weisswurst_breakfast`: Plate, two Weisswurst, Pretzel, Sweet Mustard.
- `weisswurst_breakfast_with_weissbier`: the same plus `#servus_delicacies:breakfast_beers`, saved through `output.withConditions(new ModLoadedCondition("servus_brewery"))` (verified: `IRecipeOutputExtension#withConditions`, `ModLoadedCondition(String modid)` writes `neoforge:mod_loaded`).
- `plate`: three Clay Balls and one `#c:dyes/blue` (`Tags.Items.DYES_BLUE`) into two Plates.

Tags: `servus_delicacies:dumplings` holds Bread Dumpling and Potato Dumpling. `servus_delicacies:breakfast_beers` holds the optional entry `servus_brewery:weissbier` (`TagEntry.optionalElement`), so it loads empty without Brewery and other packs can add their wheat beer.

Bowls: Gravy and the bowl foods get `Item.Properties#craftRemainder(Items.BOWL)` (verified in 26.1: `craftRemainder(Item)` sets `craftingRemainingItem`, used by `CraftingRecipe#defaultCraftingReminder`), so crafting a plate leaves the Bowl in the grid. No other crafting recipe uses them.

### D2 Portions, pure logic (package `teller`)
`MealPortions` (no Minecraft imports, JUnit tested): `FULL = 4`, `isLast(left)`, `afterEating(left)` (left minus one, only when not the last), `shown(kind, left)`; out of range values throw. Four is a rule of the block (the block state `portions` runs 1 to 4 and the look has four food parts), not balance; what a portion gives is data (D4).

`MealKind` (enum, pure): `ROAST_PORK_PLATE`, `SCHNITZEL_PLATE`, `WEISSWURST_BREAKFAST`, `WEISSWURST_BREAKFAST_WITH_WEISSBIER`, each with its id, its base parts (stay until the end), its four portion parts (the first `left` are shown, so the last listed is eaten first) and its extra parts (the glass). `MealPart(float[] box, String top, String side)` names the texture keys. `MealParts` (pure) holds the plate's boxes and `boxes(kind, left)` for shapes and models. `MealPartsTest` checks the layout rules: every food part stands on the plate top (y 1) or on a base part and lies inside the block, and no two parts of one state share a face plane where they overlap.

### D3 Blocks
- `PlateBlock extends Block`: shape from `MealParts.PLATE`; placement only on a solid top (`Block.canSupportCenter`, as the loaf); no survive rule; `useWithoutItem` passes. Properties for plate and meals: `strength(0.3F)`, `SoundType.DECORATED_POT` (ceramic, verified in 26.1), `noOcclusion`, `PushReaction.DESTROY`.
- `MealBlock extends Block` with a `MealKind`, `FACING` (horizontal, toward the player) and `PORTIONS` (`IntegerProperty.create("portions", 1, 4)`), default full. Codec: `RecordCodecBuilder.mapCodec` with the kind as a string field and `propertiesCodec()`. Shapes per state from `PartShapes.shapeOf(MealParts.boxes(kind, left), facing)` (Foundation), built once in the constructor. Placement as the plate.
- `useWithoutItem`, modelled on vanilla `CakeBlock#useWithoutItem` (verified in 26.1): on both sides, when the meal has a data entry and `player.canEat(false)`, eat; else `PASS`. Eating on the server: `player.getFoodData().eat(portion)` (verified: `FoodData#eat(FoodProperties)`), `SoundEvents.GENERIC_EAT` (a `Holder.Reference`, verified), `GameEvent.EAT`, item particles of the meal; then either `setBlock(portions - 1)` or finish (D5). `useItemOn` returns `TRY_WITH_EMPTY_HAND`.

### D4 Meal data map
`Meal(FoodProperties portion, List<MobEffectInstance> effects)` with a codec of `FoodProperties.DIRECT_CODEC` (`portion`) and `MobEffectInstance.CODEC.listOf()` (`effects`, optional, default empty; verified: `id`, `duration`, `amplifier`, ...). `DelicaciesDataMaps.MEAL`, id `servus_delicacies:meal`, on `Registries.ITEM`, synced like `FOOD`, registered in the same event; looked up through the meal block's item. Examples (datagen):

| Meal | Portion | Four portions | Parts eaten alone | Effect |
|---|---|---|---|---|
| Roast Pork Plate | 8, 12.8 | 32 | about 29 | Properly Full 1200 ticks |
| Schnitzel Plate | 5, 8.0 | 20 | 16 | Properly Full 900 ticks |
| Weisswurst Breakfast (both) | 6, 9.6 | 24 | 20 | Properly Full 900 ticks |

Saturation is the nutrition times 1.6, as the meat dishes. The meal gives a little more than its parts and the buff.

### D5 Finishing and Properly Full
After the last portion the block becomes `PlateBlock`'s default state (`setBlock`, `GameEvent.BLOCK_CHANGE`), then every effect of the meal is added as a copy (`new MobEffectInstance(effect)`), and for the Weissbier breakfast the first item of `servus_delicacies:breakfast_beers` (`BuiltInRegistries.ITEM.getTagOrEmpty`) goes into the inventory (`placeItemBackInInventory`, drops when full). Handing the beer over keeps drinking and Rausch in Brewery's own item.

`ProperlyFullEffect extends MobEffect` (`MobEffectCategory.BENEFICIAL`, warm brown colour): `applyEffectTick` heals 1.0 when hurt; `shouldApplyEffectTickThisTick` every `100 >> amplifier` ticks, as `RegenerationMobEffect` does with 50 (verified). Registered in `registry/ServusDelicaciesEffects` (`DeferredRegister.create(Registries.MOB_EFFECT, ...)`), registered in the mod constructor. Icon `textures/mob_effect/properly_full.png`, 18 x 18 like vanilla (verified in the 26.1 assets).

### D6 Drops
Loot of each meal: one pool with the meal item when `portions = 4`, another with the Plate otherwise (`LootItemBlockStatePropertyCondition`, as the loaf). The Plate drops itself. A started Weissbier breakfast drops only the Plate too; its beer is lost with the meal.

### D7 Look (pixels, built facing north)
Plate (`MealParts.PLATE`), a stepped round plate one pixel high, top `block/plate`, sides `block/plate_ceramic`:
- `1,0,4` to `15,1,12`; `2,0,2` to `14,1,4`; `2,0,12` to `14,1,14`; `4,0,1` to `12,1,2`; `4,0,14` to `12,1,15`.
- The two middle boxes leave out the face toward the centre box, the two end boxes the face toward the middle boxes; the down faces cull against the block below.

Food parts stand on the plate top (y 1) and have no down faces. Textures `block/plate_<key>`:
- Roast Pork Plate: base gravy `3,1,6` to `10,1.2,13`; portions (first shown first) roast `3.5,1.2,7.5` to `9.5,3.4,12` (top `crackling`, side `roast`), dumpling `4.5,1,3` to `8.5,5,7`, dumpling `9.5,1,3` to `13.5,5,7`, red cabbage `10.05,1,9` to `14,3.5,12.5`.
- Schnitzel Plate: schnitzel `3,1,3` to `10,2,8`, potato salad `9.5,1,9` to `13.5,3,13`, schnitzel `3,1,8.5` to `9,2,13`, potato salad `10.5,1,3.5` to `13.5,2.5,7`.
- Weisswurst Breakfast: base mustard `10,1,10` to `13,1.3,13`; portions weisswurst `3,1,2.5` to `10,3,4.5`, pretzel `10.5,1,3` to `13.5,3,5`, weisswurst `3,1,5.5` to `10,3,7.5`, pretzel `10.5,1,5.5` to `13.5,3,7.5`. With Weissbier also the glass `3.5,1,9.5` to `6.5,9,12.5` (top `weissbier_top`, side `weissbier_side`).

The mockup's lemon is left out; its third Weisswurst becomes the place of the glass, the Pretzel is two pieces so the breakfast has four portions. One model per meal and portion count; block states turn it by `FACING` (`ModelRotations.rotationY`). The items use the full model (`simpleBlockItem`).

Overlapping box pairs and their planes (CLAUDE.md "Models and textures"):

| Pair | Shared plane? | How |
|---|---|---|
| plate boxes among each other | no | they only meet back to back at z 2, 4, 12, 14, and the inner face of each pair is left out |
| plate bottom / block below | no | back to back at y 0, culled against a solid block |
| food parts / plate top | no | food parts have no down faces |
| roast / gravy top (y 1.2) | no | the roast has no down face |
| dumplings / gravy | no | the gravy's faces inside the dumplings lie inside them; no face planes meet (dumpling faces at x 4.5, 8.5, 9.5, 13.5, z 3, 7; gravy at x 3, 10, z 6, 13) |
| red cabbage / gravy | no | the cabbage starts at x 10.05, 0.05 px past the gravy's east face at x 10 |
| schnitzel and salad parts | no | 0.5 px gaps between all four |
| weisswurst, pretzel, mustard, glass | no | 0.5 px gaps; the mustard and glass sit in their own corner |

### D8 Textures
`tools/delicacies-textures/gen.py` gets one appended block: the mockup's `plate_top` (filled to the octagon, blue ring at radius 5.9 to 7.0, a little wider than the mockup so it reads closed) and `ceramic`, `food_tex` and `crackling_tex` ported, the ramps `MEAT` and `WEISS` and `FOAM`, and the block textures `plate`, `plate_ceramic`, `plate_gravy`, `plate_crackling`, `plate_roast`, `plate_dumpling` (`skn`), `plate_red_cabbage` (`redk`), `plate_schnitzel`, `plate_potato_salad` (`salad`), `plate_weisswurst` (`wurst`), `plate_pretzel` (baked dough with salt specks), `plate_mustard` (`fat`), `plate_weissbier_side` (hazy wheat beer behind glass edges, a foam band on top) and `plate_weissbier_top` (foam), plus `mob_effect/properly_full` (a small plate with a brown heap, 18 x 18).

### D9 GameTests (`gametest/TellerGameTests`, registered in `DelicaciesGameTests`)
- `eatFourPortions`: a hungry player eats a placed Roast Pork Plate four times; hunger rises by the data value each time, portions fall 4, 3, 2, 1, no buff before the last; after the fourth an empty Plate stands there, the player has Properly Full for the data's duration, and the empty plate does nothing.
- `fullPlayerEatsNothing`: a full player, or a click with a stick, does not change the portions.
- `schnitzelPortionFromData`: one Schnitzel Plate portion restores its data value.
- `beerBreakfastFinishesWithoutBrewery`: the last portion of the Weissbier breakfast leaves the Plate and Properly Full; without Brewery nothing is handed over.
- `mealAssembly`: the shipped recipes craft all three meals and two Plates; the Roast Pork Plate and the Schnitzel Plate leave a Bowl; the Weissbier recipe exists exactly when Brewery is loaded.
- `mealDrops`: a full meal drops itself; a started one only a Plate; a Plate drops itself.
- `plateNeedsASolidTop`: a solid block carries a plate, air does not.
- `properlyFullHeals`: the effect heals on its interval, twice as often one level up, and one tick heals one health point.

JUnit: `MealPortionsTest` (last portion, after eating, out of range throws), `MealPartsTest` (shown parts per count, base kept, all parts inside the block and on the plate, no shared planes where parts overlap).

### Settles from PLAN.md
PLAN.md lists `m14-teller` with "plated meals with portions and a meal buff (Roast Pork with two dumplings and red cabbage, Schnitzel with potato salad, the Weisswurst breakfast; its Weissbier only when Brewery is installed)". All of it is built here. The mockup's open buff name is settled as "Properly Full". PLAN.md is groomed when archived (the brief asks agents not to edit it now).

## Assumptions

- Assembly is crafting only (shapeless); no hand assembly on a placed plate.
- Four portions per meal is a rule of the block (block state, look); the food per portion and the finish effects are data.
- Either dumpling counts for the Roast Pork Plate; the gravy comes from the Gravy bowl item, the red cabbage from Braised Red Cabbage, the salad from Potato Salad; their Bowls stay in the grid.
- A player only eats while hungry (as cake); a full player clicking does nothing.
- Breaking a started meal drops only the Plate, a full meal drops itself (so a placed meal can be picked up again).
- Weissbier: a separate meal block and item, crafted only with Brewery loaded, from `servus_delicacies:breakfast_beers` (optional entry `servus_brewery:weissbier`; `m15-beer` names its Weissbier item, the tag must match it). The glass stands on the plate and the beer is handed to the player with the last portion. The item is in the creative tab only when Brewery is loaded.
- Properly Full: heals half a heart every 5 s at amplifier 0; 1200 ticks after the Roast Pork Plate, 900 after the others.
- The Plate is crafted from three Clay Balls and a blue dye into two (day one, no kiln).
- "Slab height" is read as a flat block: a 1 px plate with food up to 5 px (the glass 8 px), as the mockup's boxes.

## Risks / Trade-offs

- [Brewery names its beer differently] The tag stays empty and the conditioned recipe cannot be crafted -> one optional tag entry to fix; the log stays clean.
- [Crafting remainder on Gravy and the bowl sides] Any later crafting recipe with them leaves a Bowl -> that is what a bowl food should do.
- [A meal on a block that is later removed] It floats, like the loaf -> food on a table, not a falling block.

## Migration Plan

None. Nothing is released.
