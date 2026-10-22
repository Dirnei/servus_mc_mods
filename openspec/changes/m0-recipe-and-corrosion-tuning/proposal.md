# Proposal

## Why

Three small tuning points from play: the Calciner parts are drawn in Stone Bricks, Polished Deepslate and Deepslate Tiles but crafted from vanilla bricks, the Item and Fluid Pipes cost six iron each although copper is the family's duct metal, and copper farm rails age too fast for a field that is watered all the time. This belongs to the balance work in `PLAN.md` ("Balance pass"); it is a small cleanup, so it carries the `m0` prefix.

## What Changes

- Calciner recipes keep their patterns but take the stone they are drawn in: the Firebox (`BIB/BFB`) uses Polished Deepslate, the Ash Pan (`B B/BIB`) Deepslate Tiles and the Calciner block (`BIB/BCB`) Stone Bricks instead of Bricks.
- The Athanor (`BCB/BFB/BBB`) takes Stone Bricks instead of Bricks; its hearth stays vanilla Bricks. The Coagulator becomes `SGS/GCG/SRS` with cobblestone in the corners instead of iron ingots.
- Item Pipe becomes `CIC/GHG/CIC` (copper ingot, iron ingot, glass, chest in the middle) and Fluid Pipe `CIC/GBG/CIC` (bucket in the middle), still eight each: four copper and two iron instead of six iron.
- Farm rail corrosion is 50% slower: `corrosion_water` in `shuttles/field.json` goes from 6000 to 9000 mB, and the code default follows.
- Docs follow the new materials and the new watering time.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `alchemy/calciner`: the Calciner crafting scenario names Stone Bricks instead of bricks.
- `alchemy/calciner-structure`: the parts requirement gains Firebox and Ash Pan recipe scenarios with their stone.
- `alchemy/athanor`: the Athanor is crafted from Stone Bricks.
- `alchemy/coagulator`: the Coagulator is crafted with cobblestone instead of iron.
- `foundation/utility-duct`: Item and Fluid Pipes are crafted from copper ingots, iron ingots, glass and a chest or bucket.
- `shuttles/rail-corrosion`: the `corrosion_water` example becomes 9000.

## Impact

- `alchemy` datagen `ServusAlchemyRecipeProvider` and the regenerated recipe and advancement JSON.
- `foundation` datagen `LogisticsDataParts` and the regenerated recipe JSON.
- `shuttles/src/main/resources/data/servus_shuttles/shuttles/field.json` and `ShuttlesStatsManager.DEFAULT_FIELD_CORROSION_WATER`.
- `docs/src/content/docs` (alchemy, foundation, shuttles, getting started) and `docs/src/data/gamedata.json`.

## Non-goals

- No change to the Calciner layout, its model or the stone blocks around it.
- No change to the Athanor hearth, which stays 13 vanilla Bricks.
- No change to Power Cable, Data Cable or other duct line recipes.
- No change to the anti-corrosion item use or any other field number.
