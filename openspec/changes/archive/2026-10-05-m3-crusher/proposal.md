# Proposal

## Why

The Crusher is the second research reward and the first yield multiplier: one raw ore becomes two grit, and grit smelts into ingots. PLAN.md milestone M3, item "Crusher".

## What Changes

- Recipe type `servus_manufacturing:crushing`: one counted input, a result and a time, as datapack JSON through datagen, plus a JEI category.
- Items **Iron Grit** and **Copper Grit** with smelting and blasting recipes to ingots, so the Burner Smelter and vanilla furnaces handle them.
- **Crusher**: a powered machine with one input and one output, 20 FE/t, running the crushing recipe of its input at the recipe's time. Side configuration, output push and the shared GUI apply.
- Built in crushing recipes: raw iron and raw copper to two grit each, cobblestone to gravel, gravel to sand (example values, retunable).
- The Crusher is assembled from an Electric Extractor, iron gears and copper wire, gated by the crusher tech. The tech's unlock id points at the recipe.

## Capabilities

### New Capabilities
- `manufacturing/crusher`: crushing recipes as data and the crusher's behavior.

### Modified Capabilities
None.

## Non-goals

- Ore doubling for other mods' ores (datapacks can add crushing recipes).
- Grit washing or further processing chains.

## Impact

- New recipe type and serializer registered beside assembling.
- New block, block entity, menu, GUI layout, items and textures.
