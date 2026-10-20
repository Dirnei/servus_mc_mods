## Context

See proposal.md for the why. Decided in the explore of 2026-10-20 (`temp/delicacies-brief.md`): Yeast crafted once from wheat, sugar and a water bottle, which the Fermenting Vat always gives back; a Cask with a brass tap; drinks only in half litre glasses (500 mB): Helles, Dunkles (Helles icon with darker beer), Weissbier in its own glass; drinking gives the empty glass back, refill at the Cask tap; no Mass. Look: `temp/delicacies-mockup.html`, `brewBoxes` in `temp/delicacies-mockup-src/template.html` (vat `16 x 14 x 16` with `foam` top and `vat_side` sides; Cask `14 x 16 x 14` in vanilla barrel textures with a `2 x 2 x 3` tap and a `1.5 x 1.5 x 1` spout in brass), sprites `halbe_helles`, `dunkles`, `halbe_glas`, `weissbier(full)` in `sprites.py`.

`m15-brewery-project`, `m15-crops`, `m15-malting`, `m15-brewhouse` (applied, not archived) left `:brewery` with the Beer Glass, the Malting Floor (game time batches checked by scheduled ticks), the Brew Kettle and the wort fluids `pale_wort`, `dark_wort`, `wheat_wort` in `registry/BreweryFluids`. `m14-tiled-stove` (applied) left `DelicaciesDataMaps.FOOD` (`servus_delicacies:food`, `FoodProperties.DIRECT_CODEC`, synced) and `DelicaciesFoodItem`. Another change (`m14-heating-element`) edits `brewhouse/` at the same time, so this change leaves `brewhouse/` alone. Sources checked: `minecraft-patched-26.1.2.116-sources.jar`, `neoforge-26.1.2.116-sources.jar` and the family code named below.

## Goals / Non-Goals

**Goals:** a vat that ferments the three worts in game time without ticking every tick, a Cask that taps glasses by data recipes, three drinks that return their glass and post a hook for the Rausch, buckets and capabilities everywhere, JEI, the mockup look without z-fighting, JUnit for the timing and GameTests for the behaviour.

**Non-Goals:** see proposal.md, Non-goals.

## Decisions

### D1 No lagering
The vat's output is the finished beer (Helles, Dunkles, Weissbier); there are no young beer fluids. The Cask stores and taps. This keeps the chain at one waiting step, the vat. Lagering in the Cask (a second wait that makes a better beer) is a later open point and is reported to the user instead of written into PLAN.md here.

### D2 Fermenting recipe (`servus_brewery:fermenting`)
`beer/FermentingRecipe(Ingredient yeast, FluidIngredient wort, Fluid result, int time) implements Recipe<FermentingInput>`, `FermentingInput(ItemStack yeast, FluidStack wort) implements RecipeInput` (one item). Codec fields `yeast` (`Ingredient.CODEC`), `wort` (`FluidIngredient.CODEC`, verified in `neoforge` `fluids/crafting/FluidIngredient`), `result` (`BuiltInRegistries.FLUID.byNameCodec()`), `time` (game ticks); stream codecs `Ingredient.CONTENTS_STREAM_CODEC`, `FluidIngredient.STREAM_CODEC`, `ByteBufCodecs.registry(Registries.FLUID)`, `VAR_INT`. `matches`: wort not empty, `wort.test`, `yeast.test`. The amount converts one to one. `isSpecial`, `PlacementInfo.NOT_PLACEABLE`, synced to clients with `ServusRecipes.syncToClients` (as the brewing recipe).

```json
{"type": "servus_brewery:fermenting", "yeast": "servus_brewery:yeast",
 "wort": {"fluid": "servus_brewery:pale_wort"}, "result": "servus_brewery:helles", "time": 36000}
```

Shipped: `fermenting/helles` (Pale Wort, 36000), `fermenting/dunkles` (Dark Wort, 36000), `fermenting/weissbier` (Wheat Wort, 24000).

### D3 Fermentation timing (pure)
`beer/Fermentation(long startedAt)` (no Minecraft imports): `start(now)`, `elapsed(now)`, `isDone(now, time)`, `remaining(now, time)` (never below 0), `percent(now, time)` (0 to 100). JUnit `FermentationTest`: not done before its time, done at and after it, remaining counts down and stops at 0, percent at start, half and end, a zero time is done at once, game time before the start (a world moved back) counts as 0.

### D4 Fermenting Vat (`beer/FermentingVatBlock`, `FermentingVatBlockEntity`)
`BaseEntityBlock` with `CONTENT` (`VatContent`: `EMPTY`, `WORT`, `FERMENTING`, `BEER`), no facing. The block entity holds `VatYeast extends ItemSlots(1)` (limit 1, valid for items some fermenting recipe takes as yeast, `extract` gives nothing while a batch runs, as `MaltingFloorBlockEntity.FloorSlot`), a tank `FluidSlot(vat_capacity, isWort)` and `@Nullable Fermentation`.

After any content change (server): a batch starts when the tank holds a fluid and a recipe matches (`Fermentation.start(gameTime)`, check scheduled); a running batch is kept as long as the recipe still matches; otherwise the batch is cleared. The look follows: empty `EMPTY`, a batch `FERMENTING`, a wort without batch `WORT`, anything else `BEER`; set only on change. `check()` from the scheduled block tick (`BlockBehaviour#tick`, `Level#scheduleTick`, as the Malting Floor): done turns the tank into `new FluidStack(result, amount)` with `setFluid` (which skips the wort validator) and clears the batch; otherwise schedules the next check in `min(remaining, 100)` ticks. Scheduled ticks are saved with the chunk and game time runs on, so an unloaded vat finishes on its first check after loading. Saved: `Yeast`, `Tank`, `Started`.

`VatFluids extends LegacyFluidHandler` (Foundation), one tank: `fill` only while no batch runs and only what the tank's validator takes; `drain` only when the tank does not hold a wort (finished beer). Registered as `Capabilities.Fluid.BLOCK` on every side; `Capabilities.Item.BLOCK` is the yeast slot.

Hand use (`useItemOn`): `Transfer.isFluidContainer` goes through `FluidUtil.interactWithFluidHandler(player, hand, pos, vatFluids, null)` (as the Brew Kettle); Yeast goes into the slot; others pass. `useWithoutItem`: sneaking takes the Yeast back (`placeItemBackInInventory`) unless a batch runs; otherwise an overlay status: `message.servus_brewery.vat.empty`, `.wort` (amount, fluid, "no yeast"), `.fermenting` (amount, fluid, percent), `.beer` (amount, fluid). Breaking drops the yeast (`preRemoveSideEffects`); the fluid is lost. `animateTick`: while `FERMENTING`, now and then a `BUBBLE_POP` over the foam.

### D5 Tapping recipe (`servus_brewery:tapping`)
`beer/TappingRecipe(Ingredient glass, FluidIngredient beer, int amount, ItemStackTemplate result) implements Recipe<TappingInput>`, `TappingInput(ItemStack glass, FluidStack beer)`. `matches`: `glass.test`, `beer.test` and at least `amount` in the stack. Shipped `tapping/helles` (Beer Glass, Helles, 500, Glass of Helles), `tapping/dunkles` (Beer Glass, Dunkles), `tapping/weissbier` (Weissbier Glass, Weissbier). So the Weissbier Glass takes only Weissbier and Weissbier only the Weissbier Glass, by data. Not in JEI in this change.

### D6 Cask (`beer/CaskBlock`, `CaskBlockEntity`)
`BaseEntityBlock` with `FACING` (horizontal, toward the placing player: `context.getHorizontalDirection().getOpposite()`), rotate and mirror as usual. The block entity holds a `FluidSlot(cask_capacity, isBeer)` where beer is any fluid that some fermenting recipe makes. `Capabilities.Fluid.BLOCK` on every side is the slot itself (`FluidSlot` is a `ResourceHandler<FluidResource>`).

`useItemOn`: a stack some tapping recipe takes as a glass, clicked on the front, taps: the first tapping recipe that matches drains its amount and the player gets the result (`ItemUtils.createFilledResult`, so a stack of 16 glasses loses one and the full glass goes to the inventory, as the vanilla bottle fill); no match tells `message.servus_brewery.cask.wrong_glass` or `.too_little`. "The front" is the hit point's distance from the block centre along `FACING`: at least the barrel front (7/16 minus a hair), so the front face and every face of the tap count. Fluid containers go through `FluidUtil.interactWithFluidHandler`. `useWithoutItem`: overlay `message.servus_brewery.cask.status` (amount, beer) or `.empty`. The beer is lost when the Cask is broken.

### D7 Glasses and drinks (`beer/BeerItem`)
- `beer_glass` becomes `registerItem(..., props.stacksTo(16))`; new `weissbier_glass`, stacks to 16.
- `helles`, `dunkles`, `weissbier`: `BeerItem` with `stacksTo(1)`, `CONSUMABLE = Consumables.DEFAULT_DRINK` (drink animation and sound, verified) and `usingConvertsTo(glass)` (`USE_REMAINDER`, applied by `ItemStack#finishUsingItem` after the item's own `finishUsingItem`, verified in 26.1). Names "Glass of Helles", "Glass of Dunkles", "Glass of Weissbier".
- `BeerItem#use` reads `DelicaciesDataMaps.food(stack)`: no entry passes; a player who cannot drink (`Player#canEat(canAlwaysEat)`) fails; otherwise starts using. `finishUsingItem` calls `FoodProperties#onConsume` (hunger, saturation, sound) and, on the server for a player, posts `BeerDrunkEvent`, then the vanilla consume. `DelicaciesFoodItem` is not extended because its constructor fixes the eating consumable.
- Food data (`data/servus_delicacies/data_maps/item/food.json` in Brewery's generated resources; NeoForge merges data map files of several packs): Helles 2 / 1.0, Dunkles 3 / 1.5, Weissbier 3 / 1.2, all `can_always_eat`.

### D8 The Rausch seam
`beer/BeerDrunkEvent extends PlayerEvent` (NeoForge `event/entity/player/PlayerEvent`, verified) with `getDrink()` (a copy of the glass before drinking), posted on `NeoForge.EVENT_BUS` once per drink, server side only. `m15-rausch` subscribes to it; this change adds no listener and no effect.

### D9 Beer fluids
`BreweryFluids` registers `helles` (`#E6A01C`), `dunkles` (`#4E260C`), `weissbier` (`#EBA845`) exactly as the worts. The record `Wort` is renamed `BrewFluid`, the lists are `WORTS`, `BEERS` and `ALL`. The client fluid models, the bucket item models (NeoForge `fluid_container`) and the liquid block models iterate `ALL`.

### D10 Stats
`beer/CellarStats(int vatCapacity, int caskCapacity)` with a `Partial`, loaded by `CellarStatsManager` from `data/<ns>/cellar/stats.json` (file `servus_brewery:stats`), registered with `AddServerReloadListenersEvent`, the shape of `BrewhouseStatsManager`. Shipped: `vat_capacity` 4000, `cask_capacity` 16000. Capacities are read when the block entity is made.

### D11 Look and models
Vat (no facing), pixels:

| Part | from | to | Faces |
|---|---|---|---|
| north wall | 0,0,0 | 16,14,1 | all, `#side`; down culled |
| south wall | 0,0,15 | 16,14,16 | all |
| west wall | 0,0,1 | 1,14,15 | all but north and south |
| east wall | 15,0,1 | 16,14,15 | all but north and south |
| floor | 1,0,1 | 15,1,15 | up `#floor`, down `#side` |
| contents | 1,12,1 | 15,12,15 | up only, `#content` (wort, foam or beer) |

Overlaps: the east and west walls draw no faces on the planes `z 1` and `z 15`, where the north and south walls' inner faces are; the floor draws no side faces, which would lie on the walls' inner faces at `x 1, 15` and `z 1, 15`; the contents face at `y 12` is a single up face and no other face lies on `y 12`; the wall tops at `y 14` meet nothing. `#side` is the generated `fermenting_vat_side` (spruce staves with two iron bands, `IRON` ramp), `#floor` is `minecraft:block/spruce_planks`.

Cask, built facing north, rotated by `ModelRotations.rotationY(FACING)`:

| Part | from | to | Faces |
|---|---|---|---|
| barrel | 1,0,1 | 15,16,15 | sides `minecraft:block/barrel_side`, top `barrel_top`, bottom `barrel_bottom` |
| tap | 7,4,-2 | 9,6,1 | all but south (on the barrel front), `cask_tap` |
| spout | 7.25,2.5,-2 | 8.75,4,-1 | all but up (on the tap's down face) |
| handle | 7.5,6,-1.5 | 8.5,8,-0.5 | all but down (on the tap's up face) |

Overlaps: the tap's back face would lie on the barrel front (dropped); the spout's top and the handle's bottom would lie on the tap's down and up faces (dropped); tap and spout fronts both at `z -2` only touch along `y 4`; no other pair shares a plane. Shape: barrel plus tap boxes via `PartShapes.shapeOf`. The tap is brass on every face (`cask_tap`, generated from the `BRASS` ramp); the barrel is vanilla wood as decided.

### D12 Textures (`tools/brewery-textures/gen.py`)
Ported from `sprites.py`: `mug(beer, ...)` for `helles` (`HELL`, foam `FOAM`, seed `helles`), `dunkles` (the Helles sprite with `(230,160,28)` to `(112,58,22)` and `(199,125,14)` to `(78,38,12)`), `weissbier(full)` for `weissbier` and `weissbier_glass`; Python's `random.Random(seed)` gives the mockup's noise. New: `yeast` (a fresh yeast cube in paper), block `fermenting_vat_side`, `vat_wort`, `vat_foam`, `vat_beer` (the mockup's `liquid` tiles, opaque), `cask_tap` (brass). Block sprites stay fully opaque or clear.

### D13 Recipes, tags, loot, lang (datagen)
- Yeast: shapeless Wheat, Sugar and a Water Bottle (`DataComponentIngredient.of(false, DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER), Items.POTION)`, verified in `neoforge` `common/crafting/DataComponentIngredient`); the bottle is used up, as a potion has no crafting remainder.
- Beer Glass x2: `G G` / `GGG` Glass. Weissbier Glass x2: `G G` / `G G` / ` G `. Fermenting Vat: `P P` / `IPI` / `PPP` (spruce planks, iron ingots). Cask: `PPP` / `PGP` / `PPP` with planks of any wood around a Gold Ingot (brass stands in as gold).
- Tags: vat and Cask `minecraft:mineable/axe`. Loot: both drop themselves.
- Lang: items, blocks, fluids, buckets, messages, JEI title.

### D14 JEI
`FermentingRecipeCategory` (yeast slot, wort fluid slot with the recipe's example amount 1000 mB, arrow, beer fluid slot, the time in game days and minutes), catalyst Fermenting Vat, in `ServusBreweryJeiPlugin`.

### D15 GameTests (`gametest/BeerGameTests`, listed in `BreweryGameTests`)
- `vatFermentsWortWithYeastAndReturnsIt` (Pale Wort through the fluid capability, Yeast through the item capability, the batch's start moved back by the recipe time through saved data as `MaltingGameTests` does: Helles, the yeast still extractable), `noYeastNoBeer`, `notDoneBeforeItsTime`, `vatKeepsWortAndYeastWhileFermenting`, `vatBucketsByHand`, `recipesAreShipped`.
- `caskFillsAGlassOfHelles`, `caskFillsAGlassOfDunkles`, `weissbierRefusesAPlainGlass` (and fills a Weissbier Glass), `weissbierGlassTakesOnlyWeissbier`, `tooLittleBeerTapsNothing`, `caskTakesOnlyBeer`, `tappingOnlyAtTheFront`.
- `drinkingReturnsTheGlass` (Helles and Weissbier: the glass back, food level up, the event posted once).
- `glassesStackTo16`, `yeastRecipe`.

## Settles from PLAN.md
PLAN.md lists `m15-beer` as above; no open decision of section 5 is touched. PLAN.md is not edited in this change (the run's instructions); the decisions for the user to carry over: the vat output is final (lagering later), tapping and fermenting are data recipe types, and `BeerDrunkEvent` is the Rausch seam.

## Assumptions

- The whole tank ferments as one batch; wort cannot be added during a batch, and wort cannot be drained back out.
- One Yeast per vat, never used up; it can be taken back only between batches.
- Fermenting times 36000 ticks for Helles and Dunkles (one and a half days, lagers take longer) and 24000 for Weissbier (data).
- The vat shows one wort, foam and beer colour for every kind; the contents surface has one fixed height.
- The Cask holds one beer at a time; tapping is only possible on the front side (the tap); glasses are not filled by pipes.
- Food values: Helles 2 / 1.0, Dunkles 3 / 1.5, Weissbier 3 / 1.2, drinkable when full (data).
- The Beer Glass now stacks to 16 instead of 64 and gets a recipe.
- Recipes: two Beer Glasses from five Glass, two Weissbier Glasses from five Glass in a tall shape, the vat from spruce planks and iron, the Cask from planks and a Gold Ingot for the brass tap.
- Fluids are lost when a vat or Cask is broken (as the Brew Kettle).

## Risks / Trade-offs

- [Data maps from two mods in one file path] Delicacies and Brewery both ship `servus_delicacies:food` entries; NeoForge merges data map files across packs (not `replace`), so both sets apply.
- [Recipe lookups] The vat looks for its recipe only after a content change and on its scheduled checks; the Cask only on a click.
- [Hit test on the tap] The front test uses the hit point; a click on the barrel's side near the front edge does not count, which is what a player expects.

## Migration Plan

None. Nothing is released.
