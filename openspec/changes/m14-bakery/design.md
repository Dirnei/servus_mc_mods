## Context

See proposal.md for the why. The explore of 2026-10-20 (`temp/delicacies-brief.md`) left the bakery details to this change: where dough is made, how bread bakes on the Tiled Stove, and where lye and salt come from. The Pretzel exists as a plain item with the approved sprite (`m14-delicacies-project`). `m14-tiled-stove` set the patterns this change follows: the `FIREBOX` block capability a cooking spot asks on the block below, vessels as `VesselBlock` / `VesselBlockEntity` with data driven recipe types, food values in the synced data map `servus_delicacies:food` read by `DelicaciesFoodItem`, JEI categories, and sprites in `tools/delicacies-textures/gen.py`. The Foundation Mill (`m14-mill`) reads `servus_foundation:milling` recipes from any namespace.

A parallel change, `m14-heating-element`, adds FE heating to the stove's firebox, so this change reads `stove/` and `firebox/` but does not edit them. Sources checked: `minecraft-patched-26.1.2.116-sources.jar`, `neoforge-26.1.2.116-sources.jar`, and the Delicacies and Foundation code named below.

## Goals / Non-Goals

**Goals:** wheat to Pretzel and Bread Roll in a day one kitchen, with every number in data, without touching the stove code, and with the items the later dish changes need (Salt, Bread Crumbs, Bread Roll).

**Non-Goals:** see proposal.md.

## Decisions

### D1 Baking: a Baking Oven vessel, not an oven inside the stove
Options: (a) a baking spot built into the stove tiles (needs `stove/` changes, conflicts with `m14-heating-element`), (b) a separate oven block with its own fire (a second heat source next to the one stove PLAN.md wants), (c) a Baking Oven vessel that stands on a stove spot like Pot and Pan. Chosen: (c). It reuses the firebox capability through `VesselBlockEntity`, so it works on every stove tile, demands heat like the other vessels (the fire lights only while something bakes) and will bake on FE once the Heating Element exists, with no code of its own for that.

Code (package `cooking`):
- `BakingRecipe(List<SizedIngredient> ingredients, ItemStackTemplate resultTemplate, int time) implements VesselRecipe`, the same fields, codecs (`SizedIngredient.NESTED_CODEC.listOf()`, `ItemStackTemplate.CODEC`, `Codec.INT`) and stream codec as `PanRecipe`, with `fluid()` empty. Type and serializer `servus_delicacies:baking` in `ServusDelicaciesRecipes`, synced with `ServusRecipes.syncToClients` like pot and pan.
- `BakingOvenBlock extends VesselBlock` with the oven parts, and `BakingOvenBlockEntity extends VesselBlockEntity<BakingRecipe>` that only names its recipe type. The `COOKING` state of `VesselBlock` drives the lit window model and the light; the block's properties use `lightLevel(state -> state.getValue(VesselBlock.COOKING) ? 7 : 0)` (`BlockBehaviour.Properties#lightLevel(ToIntFunction<BlockState>)`, as the Stove Tile uses it).
- Hand use, automation and the steam come from `VesselBlock` / `VesselBlockEntity` unchanged; `ServusDelicaciesEvents` registers `Capabilities.Item.BLOCK` for the oven like the Pan. No block entity renderer: the oven is closed, the window shows the glow.

### D2 Flour and dough
- Flour: `output.accept(key("milling/flour"), new MillingRecipe(SizedIngredient.of(tag c:crops/wheat, 1), Flour, 100), null)` in `ServusDelicaciesRecipeProvider`. `MillingRecipe` is public Foundation API; the file lands in `data/servus_delicacies/recipe/milling/flour.json` with `"type": "servus_foundation:milling"`. Work 100 (Gravel is 200): wheat grinds faster than stone.
- Dough: a shapeless crafting recipe, three `#c:flours/wheat` and one `minecraft:water_bucket` into three Dough. Verified in 26.1 `Items`: the Water Bucket has `craftRemainder(BUCKET)`, so the crafting grid keeps the empty bucket. The vanilla bucket item is named, not `c:buckets/water`, because modded water buckets may lack the remainder. Crafting keeps the Pot free and is the simplest way for a day one player.
- Tags (`KitchenTags`): `c:flours/wheat` (and its parent `c:flours`) for Flour, so other mods' wheat flour kneads too; `c:foods/dough/wheat` and `c:foods/dough` (NeoForge `Tags.Items.FOODS_DOUGH`) for Dough.

### D3 Salt from the Pot
A pot recipe `pot/salt` with no item ingredients, 1000 mB of water, 400 ticks, one Salt. `SlotMatcher.match` with no requirements matches exactly when every input slot is empty (its filled count must equal the requirement count), so water alone boils to salt and any ingredient in the pot picks its own recipe instead. A new JUnit case pins that down. Vanilla's `RecipeMap#getRecipesFor` returns nothing for an input whose `isEmpty()` is true (verified in 26.1), and `RecipeInput#isEmpty` only looks at items, so `VesselInput` overrides `isEmpty()` to count its fluid as content. The tag `c:dusts/salt` (added to `c:dusts`) holds Salt; recipes ask for the tag. Alchemy's Salt Crystal is an alchemical principle, not table salt, and is not added.

### D4 Wood ash and lye without stove changes
- Wood Ash: `SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(#minecraft:logs_that_burn), MISC, Wood Ash, 0.1, 600)` and `SimpleCookingRecipeBuilder.smelting(Ingredient.of(Charcoal), MISC, CookingBookCategory.MISC, Wood Ash, 0.1, 200)` (verified in 26.1 `SimpleCookingRecipeBuilder`). Charcoal has no vanilla smelting recipe, logs have no vanilla campfire recipe, so neither clashes. The furnace route can be automated, the campfire route works on day one.
- Tag `c:dusts/ash` (added to `c:dusts`): Wood Ash plus `TagEntry.optionalElement(servus_alchemy:ash)` through NeoForge's `ITagAppenderExtension#add(TagEntry)` (verified), so the Calciner's Ash counts when Alchemy is installed and the tag still loads without it.
- Lye: pot recipe `pot/lye`, one `#c:dusts/ash` and 250 mB of water, 100 ticks, one Lye. Lye is an item (a small stoneware jar of lye) used up by the baking recipe.

### D5 Baked goods and food values
- `baking/bread_roll`: one Dough, 200 ticks, one Bread Roll.
- `baking/pretzel`: two Dough, one Lye, one `#c:dusts/salt`, 240 ticks, two Pretzels. The shaping and the lye dip are part of the recipe; there is no raw pretzel item.
- `cutting/bread_crumbs`: one Bread Roll, `#c:tools/knife`, two Bread Crumbs.
- `PRETZEL` becomes a `DelicaciesFoodItem` (registered through the existing `food(name)` helper) and joins `FOODS`, together with `BREAD_ROLL`. Data map entries: Bread Roll 4 and 4.8, Pretzel 6 and 7.2 (vanilla Bread is 5 and 6.0). Both in `c:foods/bread` and, through `FOODS`, `c:foods`.
- New plain items: Flour, Dough, Salt, Wood Ash, Lye, Bread Crumbs. Creative tab order: Pretzel first (the tab icon), then the kitchen blocks with the Baking Oven after the Pan, then ingredients and foods.

### D6 Oven model (built facing north, pixels)
Body `servus_foundation:block/electric_pump_steel` on every face, door `servus_delicacies:block/baking_oven_door` (cold) or `baking_oven_door_lit` (baking), handle `servus_foundation:block/brass`. Parts in `VesselParts`:
- `OVEN_BODY` 2,0,3 to 14,9,14.
- `OVEN_DOOR` 3,1,2 to 13,8,3, in front of the body, no south face.
- `OVEN_HANDLE` 5,6,1.2 to 11,6.8,2, in front of the door, no south face.
Shape: the three boxes. Block state: `FACING` turns the model (`ModelRotations.rotationY`), `COOKING` picks `baking_oven` or `baking_oven_lit`; the item shows `baking_oven`.

Overlapping box pairs and their planes (CLAUDE.md "Models and textures"):

| Pair | Shared plane? | How |
|---|---|---|
| body / door | no | the door ends at z 3 where the body starts; the door has no south face, so only the body's north face is in that plane |
| door / handle | no | the handle ends at z 2 where the door starts; the handle has no south face |
| body / stove top | no | the body starts at y 0 of the block above, back to back with the stove's top face |
| body top / steam | no | particles only |

The door texture is drawn so the window sits in the part of the 16x16 texture the door's north face maps to by position (u 3 to 13, v 8 to 15): an iron frame with rivets, a dark glass window (cold) or a warm orange glow with a dark loaf shape (lit).

### D7 Textures
`tools/delicacies-textures/gen.py` gains the mockup's `semmel` (Bread Roll) and `mehl` (Flour, with the `SACK` ramp and the blue band), and new sprites in the same painted style: Dough (a pale kneaded ball with a fold), Salt (a small heap of white crystals in a wooden scoop), Wood Ash (a grey heap with darker flecks), Lye (a small brown stoneware jar with a cork), Bread Crumbs (a golden heap of crumbs), and the oven door cold and lit (block textures). The approved Pretzel grid is unchanged. The preview option writes an 8x preview of every sprite (it only wrote the animations before).

### D8 JEI
`ServusDelicaciesJeiPlugin` adds `BAKING` with a `VesselRecipeCategory` (no fluid slot, titled "Baking Oven", the hot stove note), registers the baking recipes, and adds the Baking Oven and the Stove Tile as catalysts. Salt and lye appear in the Pot category; a pot recipe without items shows only its fluid slot. Flour appears in Foundation's milling category with no Delicacies code.

### D9 GameTests (`gametest/BakeryGameTests`, registered in `DelicaciesGameTests`)
- `millGrindsWheatToFlour`: a Foundation Mill with one Wheat and an Electric Motor with energy (as `MillGameTests` does) ends with one Flour.
- `ovenBakesBreadRollOnHotStove`, `ovenBakesPretzelsOnHotStove`: on a fuelled stove the oven makes one Bread Roll from a Dough, and two Pretzels from two Dough, Lye and Salt; `COOKING` is set while it bakes.
- `noBakingWithoutFire`: the same pretzel inputs on an unfuelled stove give nothing.
- `ovenByHandAndHopper`: Dough by right click and from a hopper above; Dirt refused; a hopper below pulls only the Bread Roll.
- `potBoilsSalt`, `potMakesLye`: water alone gives Salt; Wood Ash and water give Lye.
- `pretzelIsEdibleWithDataValues`: a Pretzel raises hunger by its data map value and is used; Dough is not edible.
- `bakeryRecipesShipped`: the dough crafting recipe gives three Dough and leaves a Bucket, Oak Log on a campfire and Charcoal in a furnace give Wood Ash, a Bread Roll cuts into two Bread Crumbs.

JUnit: `SlotMatcherTest` gains `noRequirementsMatchOnlyEmptySlots`.

### Settles from PLAN.md
PLAN.md's `m14-bakery` line says "Pretzel (with lye from wood ash, decided there)". Decided: the ash comes from a vanilla Campfire or Furnace, not from the stove's firebox, so the stove code stays untouched; PLAN.md needs no edit because the line only defers the decision here.

## Assumptions

- Dough is crafted (three Flour and a Water Bucket into three Dough); no salt in bread dough.
- One Wheat mills into one Flour for 100 work.
- Bread Roll: one Dough, 200 ticks; Pretzel: two Dough, one Lye, one Salt, 240 ticks, two Pretzels; Bread Crumbs: two per Bread Roll.
- Food values: Bread Roll 4 hunger 4.8 saturation, Pretzel 6 and 7.2. Bread Crumbs, Dough, Flour, Salt, Lye and Wood Ash are not food.
- Salt: 1000 mB of water alone, 400 ticks, one Salt. A Pot left on a lit stove with only water boils it to salt; that is the intended way to make salt.
- Wood Ash: campfire 600 ticks per log, furnace 200 ticks per Charcoal. Lye: one ash and 250 mB water, 100 ticks; the jar is used up.
- Alchemy's Ash is an optional member of `c:dusts/ash`; Alchemy's Salt Crystal is not salt.
- No farmhouse loaf; vanilla Bread stays the large bread.
- Oven recipe: `III` / `IGI` with Iron Ingots and a Glass Pane. Oven light level 7 while baking.
- The oven shows no items inside; the lit window tells that it bakes.
- Flour tag `c:flours/wheat` follows the convention other mods use for wheat flour; NeoForge has no constant for it.

## Risks / Trade-offs

- [Water alone turns to salt] A player who fills a Pot and waits before adding potatoes loses progress only, but one who leaves water on a lit stove gets salt -> the fire lights only on demand anyway, the salt recipe is slow (400 ticks), and JEI shows it.
- [Food values outside the `FOOD` component] Same as the cooked foods: tooltips of other mods miss them; gameplay is correct.
- [A recipe with no item ingredients in JEI] The category shows only the fluid slot -> fine for salt.

## Migration Plan

None. Nothing is released; the `pretzel` item keeps its id.
