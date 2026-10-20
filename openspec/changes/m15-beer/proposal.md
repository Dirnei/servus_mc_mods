## Why

The Brew Kettle of `m15-brewhouse` brews Pale, Dark and Wheat Wort, but nothing turns wort into beer yet, and the Beer Glass is still a creative only item (PLAN.md, Open: M15 Brewery, `m15-beer`: "Yeast, Fermenting Vat, Cask with a brass tap, Helles, Dunkles and Weissbier in half litre glasses"). This change closes the chain from the field to the glass.

## What Changes

- **Yeast**: a new item, crafted once from Wheat, Sugar and a Water Bottle. The Fermenting Vat never uses it up.
- **Fermenting Vat**: a new block, an open spruce vat with iron bands (`temp/delicacies-mockup.html`, `vat_side` with a foamy top). It takes wort (fluid capability, buckets) and one Yeast (item capability, by hand). With both it ferments the whole tank over in game time (data, examples one to one and a half in game days, game time based and checked by scheduled ticks, never every tick) into the finished beer: Pale Wort to Helles, Dark Wort to Dunkles, Wheat Wort to Weissbier. The yeast stays in the vat and can be taken back after the batch. The top shows the wort, foam while it ferments, and the beer when it is done. Recipes are data, type `servus_brewery:fermenting`.
- **No lagering step**: the vat's beer is final; the Cask only stores and taps it.
- **Cask**: a new block, a wooden barrel (vanilla barrel textures) with a brass tap on the front. It holds one beer (example 16000 mB) and offers the fluid capability. Right clicking the front with an empty glass taps 500 mB into a full glass: a Beer Glass becomes a Glass of Helles or Dunkles, and Weissbier fills only the new **Weissbier Glass**, which takes nothing else. Which glass takes which beer is data, type `servus_brewery:tapping`.
- **Drinks**: Glass of Helles, Glass of Dunkles, Glass of Weissbier. Drinking plays the drink animation, gives the empty glass back, restores a little hunger and saturation from the Delicacies food data map (`servus_delicacies:food`), and posts a `BeerDrunkEvent` on the NeoForge event bus: the seam the Rausch counter of `m15-rausch` will listen to. No effects yet. Full glasses stack to 1, empty glasses to 16 (the Beer Glass changes from 64). The Beer Glass and the Weissbier Glass get crafting recipes.
- **Beer fluids** Helles, Dunkles and Weissbier with buckets, as tinted water like the worts.
- **Balance data**: vat and cask capacity in `data/servus_brewery/cellar/stats.json`; fermenting and tapping recipes and the drinks' food values as JSON.
- **JEI**: a Fermenting category (yeast, wort, beer, time) with the vat as catalyst.
- Textures in `tools/brewery-textures/gen.py` (the approved `halbe_helles` and `weissbier` sprites full and empty, Dunkles as Helles recoloured, Yeast, vat staves with iron bands, wort, foam and beer surfaces, the brass tap), datagen for models, block states, loot, tags, recipes, data maps and lang.
- Pure logic with JUnit tests: the fermentation timing. GameTests for the vat, the cask, the glasses and drinking.

## Capabilities

### New Capabilities
- `brewery/beer`: Yeast, the Fermenting Vat and its fermenting recipes, the beer fluids, the Cask and its tapping recipes, the glasses and drinks, the drink hook, automation and JEI.

### Modified Capabilities
None. `brewery/mod-setup` (delta of the unarchived `m15-brewery-project`) says only that the Beer Glass is the tab icon; its stack size and recipe are new here.

## Non-goals

- The Rausch counter, its levels and effects, the hangover (`m15-rausch`); this change only posts the event.
- Lagering or ageing in the cask, a Mass (litre) glass, Radler.
- Filling glasses anywhere but the Cask, or glasses through pipes.
- A GUI; everything works by right click and capabilities.

## Impact

- New in `brewery/`: `beer/` (vat, cask, recipes, fermentation timing, stats, drink item, event), beer fluids in `registry/BreweryFluids`, JEI category, GameTests and JUnit tests, textures.
- Changed: `tools/brewery-textures/gen.py`, the Brewery registries, events, client, JEI plugin and datagen providers.
- No Foundation, Delicacies or `brewhouse/` source changes; Brewery reads the public Delicacies food data map `DelicaciesDataMaps.FOOD`.
