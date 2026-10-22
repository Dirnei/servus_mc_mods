# Design

## Context

All three points are numbers or materials that already live in data: the recipes come from datagen (`ServusAlchemyRecipeProvider`, `LogisticsDataParts.recipes`), the corrosion step from `data/servus_shuttles/shuttles/field.json` with a fallback constant in `ShuttlesStatsManager`. No block, model or logic changes.

## Goals / Non-Goals

**Goals:**
- Recipes and the shipped number match the decisions in the proposal, with the generated JSON and the docs game data regenerated.

**Non-Goals:**
- No new block, no new code path, no change to how corrosion is counted.

## Decisions

- **Same patterns, other stone.** The Firebox keeps `BIB/BFB` with `Items.POLISHED_DEEPSLATE`, the Ash Pan `B B/BIB` with `Items.DEEPSLATE_TILES`, the Calciner `BIB/BCB` with `Items.STONE_BRICKS`. Each is the stone the part is drawn in, so the recipe reads like the block. The Ash Pan unlock criterion follows its stone (`has_deepslate_tiles`); the other two keep their furnace and cauldron unlocks.
- **Pipe keys.** The Item Pipe pattern needs copper and a chest, so the chest moves from key `C` to `H` (`CIC/GHG/CIC`); the Fluid Pipe is `CIC/GBG/CIC`. Copper in the corners, iron top and bottom: four copper and two iron. Unlock criteria stay (`has_iron`, `has_bucket`).
- **Corrosion 9000.** `field.json` and `DEFAULT_FIELD_CORROSION_WATER` both become 9000, so a world without the file ages at the same rate. For a field of five rails (1 mB per rail every 100 ticks) one step is 9000 / 5 x 100 = 180,000 ticks, 2h 30m of watering, which the docs state.
- **Tests.** The recipes are checked by GameTests that look them up through `ServusRecipes.craftingFor` with a `CraftingInput`, as `ForgeGameTests.forgeRecipeShipped` does: one per new recipe plus a check that the old brick grid crafts nothing. `RailCorrosionGameTests` reads the step from `ShuttlesStatsManager.fieldCorrosionWater()`, so it needs no change. No pure logic changes, so no new JUnit test.

APIs: vanilla `ShapedRecipeBuilder`, `RecipeManager` via `level.recipeAccess()`; nothing new from NeoForge.

PLAN.md decisions settled: none; this is part of the open "Balance pass".

## Risks / Trade-offs

- [Polished Deepslate and Deepslate Tiles need deepslate, so the Calciner moves a little later than Bricks did] → deepslate is common below y 0 and the Calciner structure already needs Deepslate Tiles around the Ash Pan.
- [Existing fields keep their counted water against the new step] → nothing breaks, the next step just comes later; the mods are unreleased.
