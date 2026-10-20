## Why

Beer is brewed from malted grain, and Servus: Brewery has barley but no malt yet (PLAN.md, Open: M15 Brewery, `m15-malting`: "the Malting Floor, light and dark malt, grist from the Mill"). The Brewhouse (`m15-brewhouse`) kilns green malt and the Brew Kettle mashes grist, so the floor, the malt items and the milling recipes have to exist first. Helles and Dunkles differ by their malt, and Weissbier traditionally needs wheat malt, so the brew kettle must be able to tell pale, dark and wheat grist apart.

## What Changes

- **Soaked grain**: eight Barley around a Water Bucket make eight Soaked Barley; eight Wheat around a Water Bucket make eight Soaked Wheat. The bucket comes back empty.
- **Malting Floor**: a low spruce floor block, 3 px tall, crafted from spruce slabs. Right click with soaked grain spreads it on the floor (up to one stack). After a germination time from data (example: one in-game day) the grain has sprouted into Green Malt (barley) or Green Wheat Malt (wheat), shown on the floor. Right click with an empty hand turns the grain, which shortens the rest of the time by a bonus from data, at most once per cooldown. Empty hand on finished green malt takes it out; sneaking with an empty hand takes out whatever lies there. Breaking the floor drops it and its grain.
- **Germination is data**: a recipe type `servus_brewery:germinating` (input, result, time, turn bonus, turn cooldown). Brewery ships soaked barley to green malt and soaked wheat to green wheat malt.
- **Automation**: the floor exposes `Capabilities.Item.BLOCK` on every side: pipes insert soaked grain and extract only finished green malt.
- **Malt items**: Green Malt, Green Wheat Malt, Pale Malt, Dark Malt and Wheat Malt. Pale, Dark and Wheat Malt come from the Malt Kiln of `m15-brewhouse`; this change adds no recipe that makes them.
- **Grist**: Pale Malt Grist, Dark Malt Grist and Wheat Malt Grist, one item per malt so the brew kettle tells them apart. Brewery ships `servus_foundation:milling` recipes: Pale Malt to Pale Malt Grist, Dark Malt to Dark Malt Grist, Wheat Malt to Wheat Malt Grist.
- Textures in `tools/brewery-textures/gen.py` (the approved `malz` sprite for Pale Malt, `malt_floor` for the green malt on the floor), `en_us` names, datagen for models, loot, recipes and compostables.
- Pure germination timing (`Germination`) with JUnit tests; GameTests for the floor and the Mill.

## Capabilities

### New Capabilities
- `brewery/malting`: soaking grain, the Malting Floor and germination, the malt and grist items, and the Brewery milling recipes.

### Modified Capabilities
None. The Brewery creative tab requirement (delta of the unarchived `m15-brewery-project`) already says the tab lists every Brewery item. The Mill's spec (`m14-mill`) already says other mods add milling recipes as data.

## Non-goals

- The Malt Kiln and any recipe that makes Pale, Dark or Wheat Malt (`m15-brewhouse`).
- Mashing grist in the Brew Kettle (`m15-brewhouse`, `m15-beer`).
- A GUI or a JEI category for the Malting Floor.
- Soaking grain on the floor itself or with water bottles.
- Multiblock floors: each floor block is its own batch.

## Impact

- New in `brewery/`: `malting/` (Malting Floor block and block entity, floor content enum, pure `Germination`, the germinating recipe), `registry/ServusBreweryBlockEntities`, `registry/ServusBreweryRecipes`, items in `registry/ServusBreweryItems`, a capability registration, datagen for the floor model, loot, recipes, compostables and lang, GameTests, JUnit tests, textures.
- Changed: `tools/brewery-textures/gen.py` (new sprites), the Brewery mod class and datagen providers.
- No Foundation, Alchemy, Shuttles or Delicacies source changes.
