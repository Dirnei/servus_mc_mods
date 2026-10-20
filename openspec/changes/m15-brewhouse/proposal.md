## Why

Servus: Brewery has barley, hops, the Malting Floor and grist, but nothing yet dries green malt or turns grist and water into wort (PLAN.md, Open: M15 Brewery, `m15-brewhouse`: "the Brewhouse firebox multiblock with the Brew Kettle and the Malt Kiln"). The Fermenting Vat of `m15-beer` needs a wort per beer, so Helles, Dunkles and Weissbier start here. The Brewhouse is the Delicacies firebox in bricks (PLAN.md 2.6), so it reuses `FuelFire` and the `Firebox`/`HeatSource` seam that the later Heating Element plugs into.

## What Changes

- **Brewhouse Bricks**: a new block, vanilla bricks with an iron top plate, crafted from bricks and iron. **Brew Kettle**: a new copper block. A Brewhouse is built from six Brewhouse Bricks in a row of three, two deep, with four Brew Kettle blocks on the left two by two of it, and formed with the Foundation Configuration Carrot like the Tiled Stove. Breaking any part unforms it and drops what it holds.
- **Look** after `temp/delicacies-mockup.html` option D (`sudhaus`): a low bricked firebox with two iron fire doors on the front, a big copper kettle with a dome and a vent pipe on the left, and a grate tray on the right. The doors glow and flicker while the fire burns; the vent pipe steams while the kettle brews; the tray shows its malt.
- **One firebox** for the whole Brewhouse: one fuel slot (any furnace fuel), lit only while the kettle or the kiln has work, with the Delicacies fire logic. Every formed brick offers the Delicacies firebox capability.
- **Brew Kettle**: takes water (tank, example 4000 mB), grist and hops; while the firebox is hot it brews a recipe into wort in a second tank. Three wort fluids with buckets: **Pale Wort**, **Dark Wort**, **Wheat Wort**, so the Fermenting Vat tells Helles, Dunkles and Weissbier apart. Recipes are data, type `servus_brewery:brewing`.
- **Malt Kiln**: the two right bricks are two trays of one stack each. Green Malt dries to **Pale Malt**; in dark mode (toggled with the carrot on a tray) it is kept on the fire longer and comes out **Dark Malt**; Green Wheat Malt dries to **Wheat Malt** in both modes. Recipes are data, type `servus_brewery:kilning`.
- **Hand use**: fuel on any brick, malt on a tray, grist and hops on the kettle, a water bucket into and an empty bucket out of the kettle; empty hand takes a tray's stack or tells the fire or kettle state.
- **Automation**: `Capabilities.Item` on the bricks under the kettle (fuel), on each tray (green malt in, finished malt out) and on the kettle (grist and hops in); `Capabilities.Fluid` on the kettle (water in, wort out).
- **Balance data**: kettle and wort tank sizes in `data/servus_brewery/brewhouse/stats.json`; brewing and kilning recipes as JSON.
- **JEI**: Brewery gets a JEI plugin with a Brewing and a Kilning category.
- Textures in `tools/brewery-textures/gen.py` (copper, fire door cold and lit, grate, malt on the kiln), wort fluids as tinted water, `en_us`, datagen for models, block states, loot, tags and recipes.
- Pure logic with JUnit tests: the Brewhouse layout search, the slicing of the one big model into its blocks, and the kiln timer. GameTests for forming, kiln, kettle, hand use and pipes.

## Capabilities

### New Capabilities
- `brewery/brewhouse`: the Brewhouse multiblock (bricks, kettle blocks, forming, unforming, firebox and fuel, look), the Brew Kettle with its brewing recipes and the wort fluids, the Malt Kiln with its kilning recipes and modes, automation and JEI.

### Modified Capabilities
None. `brewery/malting` (delta of the unarchived `m15-malting`) already says Pale, Dark and Wheat Malt come only from the Malt Kiln; this change delivers that. `delicacies/tiled-stove` (delta of the unarchived `m14-tiled-stove`) already says other Servus mods offer the same firebox and reuse its fire logic.

## Non-goals

- The Heating Element (`m14-heating-element`); the Brewhouse keeps the `HeatSource` seam it will use.
- Fermenting, casks, yeast and beer (`m15-beer`).
- A GUI; everything works by right click and capabilities.
- A mirrored Brewhouse with the kettle on the right, or larger brewhouses.
- Rendering the kettle's contents: the kettle is closed under its dome.

## Impact

- New in `brewery/`: `brewhouse/` (layout and model slicing, bricks and kettle blocks, the shared block entity, kettle, kiln, recipes, stats), wort fluids, a JEI plugin, a client class for fluid models and particles, datagen additions, GameTests and JUnit tests, textures.
- Changed: `brewery/build.gradle` (JEI API compile only), `tools/brewery-textures/gen.py`, the Brewery mod class, events, registries and datagen providers.
- No Foundation, Alchemy, Shuttles or Delicacies source changes; Brewery uses the public Delicacies firebox types (`Firebox`, `HeatSource`, `FuelFire`, `FuelSupply`, `DelicaciesCapabilities.FIREBOX`) and helpers (`KitchenSlots`, `SlotMatcher`) as they are.
