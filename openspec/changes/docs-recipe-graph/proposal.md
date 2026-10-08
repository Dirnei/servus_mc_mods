# Proposal

## Why

Balance talks like "use more copper, add a circuit item for the shuttle" need to show how every Servus recipe connects, and the Balance lab can't show that today. The game data export only covers Manufacturing's production recipes (assembling, crushing, smelting) and skips every crafting recipe, so the Shuttle, the cables, the crates, the stations and all of Lib, Logistics and Storage are missing from the site.

This is tooling for the docs site, not a PLAN.md milestone item. It extends the "Documentation site and Balance lab" entry in PLAN.md section 4. It does not block or depend on the open `m8-data-cable` change.

## What Changes

- The Manufacturing data run exports the recipes of all four Servus mods: crafting recipes (shaped, shapeless and Servus types built on a shaped grid, such as the heavy rack) with their grid, and special recipes such as the facade as a marked entry without ingredients. They go in a new `crafting` section, so the production `recipes` section and the chain math stay as they are.
- Item display names come from the language files of all four mods, so Storage items get their real names even though Storage is not on Manufacturing's classpath.
- The game data schema goes from 1 to 2.
- A new Balance lab page, "Recipe graph", draws one layered graph of every recipe defined in a `servus_*` namespace, from raw materials on the left to products on the right. Vanilla recipes never appear. Vanilla items and tags appear only as inputs or outputs of Servus recipes.
- Nodes are coloured by mod and can be filtered by mod. Clicking an item highlights everything it is made from, back to the leaves, and a side panel shows the item's recipes, what it is used in, and its raw cost in vanilla items per one item.

## Non-goals

- Drafting crafting recipes, drawing a 3x3 grid in an editor, changing recipe ingredients from the graph, and showing drafts in the graph. These go in the follow-up change `docs-recipe-editor`.
- Using crafting recipes in the chain view or the ratio math, including the Assembler running crafting recipes.
- Applying lab edits or drafts to the graph. The graph shows the committed game data.
- Any change to recipes in the game. The Copper Circuit idea will be its own change, discussed with this graph.

## Capabilities

### New Capabilities
- `docs/recipe-graph`: the Recipe graph page of the Balance lab: which recipes and items it shows, the layered layout, mod colours and filter, ancestor highlight, and the item panel with recipes, uses and raw cost.

### Modified Capabilities
- `lib/gamedata-export`: the export covers recipes from all four mods, adds the `crafting` section with grids and special recipes, takes item names from all four language files, and moves to schema 2.

## Impact

- `manufacturing/src/main/java/.../datagen/gamedata/` (`GameDataExportProvider`, `GameDataJson`, a new pure crafting recipe reader with JUnit tests).
- `manufacturing/build.gradle`: a system property naming the four generated resource folders.
- `docs/src/data/gamedata.json` regenerated at schema 2.
- `docs/src/lib/gamedata.ts` (schema 2, `crafting` section), a new pure `docs/src/lib/recipegraph.ts` with vitest tests, a new `RecipeGraph` component, a new `docs/src/content/docs/lab/recipes.mdx` page, a sidebar entry in `docs/astro.config.mjs`, and styles in `docs/src/styles/lab.css`.
- No game code, no recipe, no gameplay change.
