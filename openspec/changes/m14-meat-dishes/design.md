## Context

See proposal.md for the why. The explore of 2026-10-20 (`temp/delicacies-brief.md`) fixed the dish list (Weisswurst with Sweet Mustard, Leberkaese as a loaf you slice, Schnitzel from pork, flour, egg and bread crumbs, Cordon Bleu with ham and cheese, Roast Pork with crackling and gravy) and left the chains to this change. `m14-tiled-stove` and `m14-bakery` set the patterns followed here: vessels (Pot, Pan, Baking Oven) with data driven `servus_delicacies:pot`, `pan` and `baking` recipes, the Cutting Board with `servus_delicacies:cutting` recipes and `#c:tools/knife`, food values in the synced data map `servus_delicacies:food` read by `DelicaciesFoodItem`, JEI categories per vessel, sprites in `tools/delicacies-textures/gen.py`. The Foundation Mill reads `servus_foundation:milling` recipes from any namespace.

`m14-sides` runs in parallel and adds the Fermenting Crock and Cheese. This change only names cheese through the tag `c:foods/cheese`. Sources checked: `minecraft-patched-26.1.2.116-sources.jar`, `neoforge-26.1.2.116-sources.jar`, `jei-26.1.2-common-api-29.43.0.107.jar`, and the Delicacies code named below.

## Goals / Non-Goals

**Goals:** the five dishes with chains that use the existing kitchen blocks only, every number in data, the loaf as a placed block cut with the knife, and no edits to the vessels, the stove or Foundation.

**Non-Goals:** see proposal.md.

## Decisions

### D1 Cuts: one ingredient per cut, chained
A cutting recipe takes one item (`CuttingRecipe(ingredient, tool, result)`), so mixing happens elsewhere:
- `cutting/pork_cutlet`: `minecraft:porkchop` into one Pork Cutlet (the thin cutlet that is beaten flat).
- `cutting/minced_pork`: a Pork Cutlet into two Minced Pork. The cutlet is the pork cut on its way to mince, so raw Porkchop keeps a single cutting recipe and the board never has to choose.
- `cutting/minced_beef`: `minecraft:beef` into two Minced Beef.
The vanilla items are named, not a tag: no common raw pork or raw beef tag exists in NeoForge 26.1 `Tags.Items` (verified: only `c:foods/raw_meat`, which also holds chicken and mutton).

### D2 Breading and mixing in the crafting grid
The board holds one item and the vessels cook, so mixing is crafting, as the bakery kneads dough:
- Breaded Cutlet: shapeless Pork Cutlet, `#c:flours/wheat`, `#c:eggs`, Bread Crumbs.
- Raw Cordon Bleu: shapeless Pork Cutlet, Ham, `#c:foods/cheese`, `#c:flours/wheat`, `#c:eggs`, Bread Crumbs (six, so a crafting table).
- Sausage Meat: shapeless Minced Pork, Minced Beef, `#c:crops/parsley`, `#c:dusts/salt` into two.
- Leberkaese Roll: shapeless Leberkaese Slice and Bread Roll.

### D3 Cheese through a tag only
`KitchenTags.CHEESE = c:foods/cheese` (NeoForge has no constant for it, verified). `ServusDelicaciesItemTagsProvider` writes the tag with no entries of its own (`tag(KitchenTags.CHEESE)`), so the file `data/c/tags/item/foods/cheese.json` exists even without `m14-sides`. The recipe loads with an empty tag: `Ingredient.CODEC` wraps `ExtraCodecs.nonEmptyHolderSet`, which only rejects a direct empty list, not a tag (verified in 26.1). `m14-sides` adds `servus_delicacies:cheese` to the same tag; then Cordon Bleu needs no change. Until then any mod's cheese in `c:foods/cheese` works, and without one the Raw Cordon Bleu cannot be crafted.

### D4 Pot, Pan and Oven recipes
All through the existing recipe records, so the vessels need no code:
- Pan: `pan/schnitzel` (Breaded Cutlet, 160 ticks), `pan/cordon_bleu` (Raw Cordon Bleu, 200 ticks).
- Pot: `pot/ham` (Porkchop, `#c:dusts/salt`, 250 mB water, 300 ticks, two Ham), `pot/weisswurst` (two Sausage Meat, 250 mB water, 200 ticks, two Weisswurst), `pot/sweet_mustard` (two Mustard Flour, one Sugar, 250 mB water, 200 ticks, two Sweet Mustard), `pot/gravy` (Chopped Onion, `#c:flours/wheat`, Bowl, 250 mB water, 160 ticks, one Gravy).
- Baking: `baking/leberkaese` (two Minced Pork, two Minced Beef, one `#c:dusts/salt`, 400 ticks, one loaf), `baking/roast_pork` (two Porkchop, one `#c:crops/onion`, one `#c:dusts/salt`, 400 ticks, two Roast Pork).
- Milling: `milling/mustard_flour` (`#c:crops/mustard`, one Mustard Flour, work 100), the same shape as the bakery's flour.

Each vessel input holds one item kind (`VesselBlockEntity#canHoldInput`), and none of these recipes names the same item twice, so they fit the four input stacks.

### D5 Gravy is a bowl food
Verified in 26.1 `Item.Properties#usingConvertsTo(Item)`: it sets `DataComponents.USE_REMAINDER`, which `ItemStack#finishUsingItem` applies after the item's own `finishUsingItem`, so a `DelicaciesFoodItem` registered with `usingConvertsTo(Items.BOWL)` and `stacksTo(16)` gives the bowl back like vanilla stews. Gravy is in `c:foods/soup` and is the gravy of the Roast Pork plate in `m14-teller`. Roast Pork is one item that shows its crackling; there is no separate crackling item.

### D6 Sliceable loaf (package `meat`)
Pure, JUnit tested `LoafSlices` (no Minecraft imports):
- `SLICES = 4`; the block state `cut` runs 0 to `SLICES - 1`.
- `left(cut)` = slices still on the loaf; `isLastSlice(cut)` = the next cut removes the block; `next(cut)` = the state after a cut that is not the last; out of range values throw.
- `box(cut)`: the loaf box in pixels, built facing north: `3 + 2.5 * cut, 0, 5` to `13, 5, 11`. Each slice takes 2.5 px off the west end.

`LeberkaeseBlock extends Block` with `FACING` (horizontal, from `context.getHorizontalDirection().getOpposite()`) and `CUT` (`IntegerProperty.create("cut", 0, 3)`). Shape per state from `PartShapes.shapeOf(new float[][] {LoafSlices.box(cut)}, facing)` (Foundation, as the vessels use it), built once per state in the constructor. Properties like vanilla `CakeBlock` (verified in 26.1): `strength(0.5F)`, `SoundType.WOOL`, `PushReaction.DESTROY`, plus `noOcclusion`. Unlike the cake it has no survive rule: a cake that loses its support vanishes without drops (`updateShape` returns air), and a loaf should not be lost that way. Placement still asks for a solid top face below (`canSurvive` with `Block.canSupportCenter(level, pos.below(), Direction.UP)`), checked only when placed.

`useItemOn`: a stack in `KitchenTags.KNIVES` cuts on the server: `player.getInventory().placeItemBackInInventory(new ItemStack(LEBERKAESE_SLICE))` (it drops what does not fit), `stack.hurtAndBreak(1, player, hand)`, a wool hit sound, item particles of the slice, then `setBlock(next)` or `removeBlock` with a `GameEvent.BLOCK_DESTROY`; returns `SUCCESS` on both sides. Any other stack returns `TRY_WITH_EMPTY_HAND`; `useWithoutItem` returns `PASS`.

Loot (`ServusDelicaciesBlockLoot`): one pool with an alternatives entry: when `cut = 0` the loaf item, otherwise for each `cut` the slice with `SetItemCountFunction` set to `LoafSlices.left(cut)`, each under a `LootItemBlockStatePropertyCondition`.

The Cutting Board recipe `cutting/leberkaese_slices` cuts a loaf item into `LoafSlices.SLICES` slices, so automation slices too.

### D7 Items
New items, appended in `ServusDelicaciesItems`: plain items Pork Cutlet, Minced Pork, Minced Beef, Breaded Cutlet, Raw Cordon Bleu, Sausage Meat, Mustard Flour, Sweet Mustard; `DelicaciesFoodItem`s Schnitzel, Cordon Bleu, Ham, Weisswurst, Leberkaese Slice, Leberkaese Roll, Roast Pork, Gravy; the loaf as a block item `leberkaese` with `stacksTo(16)`. Two lists keep the meat items together: `MEAT_FOODS` (the foods, for the data map and `c:foods`) and `MEAT_ITEMS` (the creative tab order, appended after the existing items). The loaf item shows the flat `leberkaese` sprite, not its block model.

### D8 Food values (data map, examples)
Vanilla Cooked Porkchop is 8 and 12.8. Schnitzel 10 and 16.0, Cordon Bleu 12 and 19.2, Roast Pork 10 and 16.0, Leberkaese Roll 11 and 17.6, Weisswurst 7 and 11.2, Leberkaese Slice 6 and 9.6, Ham 4 and 4.8, Gravy 3 and 3.6. Saturation is the hunger times 1.6 (the vanilla cooked meat modifier 0.8) except Ham and Gravy (1.2). Tags: the meat dishes (Schnitzel, Cordon Bleu, Ham, Weisswurst, Leberkaese Slice, Leberkaese Roll, Roast Pork) in `c:foods/cooked_meat`; every meat food in `c:foods`; Gravy in `c:foods/soup`; Mustard Flour in `c:flours/mustard` and `c:flours`. The raw cuts are not in `c:foods/raw_meat` because they cannot be eaten.

### D9 Loaf model (built facing north, pixels)
`MeatModels` (datagen, called once from `ServusDelicaciesBlockStateProvider`) writes `leberkaese_cut0` to `leberkaese_cut3`: one element each, `LoafSlices.box(cut)`, top `servus_delicacies:block/leberkaese_crust`, north, south, east and bottom `block/leberkaese_side`, west `block/leberkaese_side` when uncut and `block/leberkaese_inside` once cut. The block state turns the model by `FACING` (`ModelRotations.rotationY`) and picks it by `cut`.

| Pair | Shared plane? | How |
|---|---|---|
| loaf box / itself | no | one element per model |
| loaf bottom / block below | no | the loaf starts at y 0, back to back with the top face of the block below |

### D10 Textures
`tools/delicacies-textures/gen.py` gains the mockup's `blob`, the `WURST`, `PINK`, `CRUST`, `BREAD`, `HAM`, `CHEESE`, `CRACK`, `GRAVY`, `CER` ramps and the sprites `weisswurst`, `senf` (Sweet Mustard), `leberkas` (the loaf item), `leberkassemmel` (Leberkaese Roll), `schnitzel`, `cordon_bleu` and `schweinebraten` (Roast Pork). The Schnitzel drops the mockup's lemon wedge, since the mod has no lemons; the parsley stays. New sprites in the same style: Pork Cutlet (a pale pink flat cutlet), Minced Pork and Minced Beef (heaps in pink and in dark red), Breaded Cutlet (the cutlet shape in pale breading with crumb specks), Raw Cordon Bleu (the same with the ham and cheese seam), Ham (two folded pink slices with a pale fat rim), Sausage Meat (a pale heap with parsley flecks), Mustard Flour (a yellow heap of powder), Leberkaese Slice (a pink slab with a crust edge), Gravy (a blue banded bowl of brown gravy), and the block faces `leberkaese_crust`, `leberkaese_side` and `leberkaese_inside`. They are added as one appended block in `SPRITES`.

### D11 JEI
No new category: every new recipe uses an existing type. `ServusDelicaciesJeiPlugin#registerRecipes` adds `registration.addIngredientInfo(LEBERKAESE, Component.translatable("gui.servus_delicacies.jei.leberkaese_info"))` (verified: `IRecipeRegistration#addIngredientInfo(ItemLike, Component...)` in JEI 29.43). Mustard Flour shows in Foundation's milling category with no Delicacies code.

### D12 GameTests (`gametest/MeatGameTests`, registered in `DelicaciesGameTests`)
- `schnitzelChain`: a knife cuts a Porkchop on the board into a Pork Cutlet; the shipped crafting recipe turns it with Flour, an Egg and Bread Crumbs into a Breaded Cutlet; a Pan on a fuelled stove fries it into a Schnitzel.
- `weisswurstInThePot`: two Sausage Meat and 1000 mB of water give two Weisswurst and leave 750 mB.
- `noWeisswurstWithoutWater`: two Sausage Meat in a Pot without water stay raw.
- `leberkaeseSlicedWithKnife`: a placed loaf gives one slice per knife click into the player's inventory, the knife wears, `cut` rises, the fourth click removes the block; an empty hand does nothing.
- `cutLoafDropsItsSlices`: the loot of a loaf with `cut = 1` is three slices, of an uncut loaf the loaf.
- `roastPorkInTheOven`: two Porkchops, an Onion and a Salt in a Baking Oven on a fuelled stove give two Roast Pork.
- `ovenBakesLeberkaese`: two Minced Pork, two Minced Beef and a Salt bake one loaf.
- `gravyGivesTheBowlBack`: eating Gravy raises hunger by its data value and leaves a Bowl; a Pork Cutlet is no food.
- `meatRecipesShipped`: beef cuts into two Minced Beef, the loaf item cuts into four slices, the Cordon Bleu crafting recipe and the mustard milling recipe exist, Sausage Meat and the Leberkaese Roll are crafted.

JUnit: `LoafSlicesTest` (left counts, last slice, next, box shrinks from the west end, out of range throws).

### Settles from PLAN.md
PLAN.md's line lists "Cheese from the Fermenting Crock" under `m14-meat-dishes`, while the brief and the change list put Cheese into `m14-sides`. Decided: Cheese is built in `m14-sides`; this change only uses `c:foods/cheese`. PLAN.md is left alone by request of this run; archiving should move the cheese mention to the `m14-sides` line.

## Assumptions

- No casings: Sausage Meat simmers straight into Weisswurst.
- Ham is simmered (Kochschinken) in the Pot from a raw Porkchop, Salt and water, two per porkchop.
- Raw Porkchop is the pork shoulder; no separate cut item.
- A porkchop gives one cutlet, a cutlet two Minced Pork, a beef two Minced Beef.
- The loaf gives four slices (a block state constant like the cake's bites, not data), stacks to 16, and is not eaten by hand. Cut slices go into the inventory; a full inventory drops them.
- The Leberkaese Roll needs no mustard. Sweet Mustard is made now and served with the Weisswurst breakfast in `m14-teller`; it is not edible alone.
- Gravy is a bowl food (3 and 3.6) made in the Pot with a Bowl as an ingredient.
- All times and food values as listed in D4 and D8.
- The Schnitzel sprite leaves out the mockup's lemon.

## Risks / Trade-offs

- [Cheese missing] Without `m14-sides` or another cheese mod the Raw Cordon Bleu cannot be crafted -> JEI shows the empty tag slot; the Schnitzel chain works on its own.
- [A Bowl as a Pot ingredient] Hoppers above a Pot now feed Bowls into it -> only when a Bowl is offered; a stray Bowl blocks other recipes the same way any stray item does.
- [Shared files with `m14-sides`] Registries, providers, lang and gen.py are edited by both -> this change appends self contained blocks and lists, so merges are additive.
- [Loaf left floating] Without a survive rule a loaf stays when the block below is removed -> it is food on a table, not a falling block; breaking it gives the loaf or its slices back.

## Migration Plan

None. Nothing is released.
