# Design

## Context

The rack recipe is generated in `ServusShuttlesRecipeProvider` with `ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, HEAVY_RACK)` and saved through the `heavyRack` output wrapper, which turns the shaped recipe into a `HeavyRackRecipe` with the same `ItemStackTemplate` result. `HeavyRackRecipe.assemble` copies the result from `ShapedRecipe.assemble` and sets the rack wood component.

## Decisions

### D1. Count in the recipe JSON

The count is the result count of the generated recipe (`ShapedRecipeBuilder.shaped(items, category, item, 3)`), so it lives in the datapack JSON like every recipe and a pack can change it. The wrapper already passes the result template on, count included, and `assemble` keeps the count when it sets the wood, so every rack of the stack carries the wood.

### D2. Patterns follow the recipe

A pattern for the rack recipe now yields three racks per craft. Autocrafting reads the recipe result, so no code change is expected; GameTests that count crafted racks are adjusted to the new result.

## NeoForge APIs

- Vanilla `ShapedRecipeBuilder.shaped(HolderGetter<Item>, RecipeCategory, ItemLike, int)` (26.1).

## Testing

GameTest `rackRecipePicksWood` checks the count of 3 and the wood on the stack. No pure logic.
