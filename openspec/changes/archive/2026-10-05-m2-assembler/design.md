# Design

## Context

Manufacturing has the burner and powered machine bases, the generator and the cable network. No custom recipe type exists yet. See proposal.md and the delta spec.

APIs verified in the NeoForge 21.1.255 sources:
- `Recipe<T extends RecipeInput>` (`matches`, `assemble`, `canCraftInDimensions`, `getResultItem`, `getSerializer`, `getType`), `RecipeSerializer` (`codec()`, `streamCodec()`), `RecipeInput` (`getItem`, `size`).
- `SizedIngredient.FLAT_CODEC`, `STREAM_CODEC`, `test`, `count`, `of(ItemLike, int)`.
- `RecipeManager.getAllRecipesFor`, `byKey`; `RecipeOutput.accept(ResourceLocation, Recipe, AdvancementHolder)`.
- `AbstractContainerMenu.clickMenuButton(Player, int)` and `MultiPlayerGameMode.handleInventoryButtonClick(int, int)`.
- `GuiGraphics.renderItem`, `renderComponentTooltip`.

## Goals / Non-Goals

**Goals:**
- Recipes are plain data with counts, matching Factorio style ratios.
- Selection uses vanilla plumbing only.
- The assembler is the first powered machine and proves generator and cable delivery end to end.

**Non-Goals:**
- Gating, upgrades, JEI.

## Decisions

### D1: Recipe type
`AssemblingRecipe(List<SizedIngredient> ingredients, ItemStack result, int time, Optional<ResourceLocation> tech)` with `AssemblingRecipeSerializer` (map codec and stream codec) and `AssemblerRecipeInput` wrapping the four input stacks. `matches` sums matching stacks per ingredient and requires the count. Ingredients of one recipe must not overlap (two ingredients matching the same item), which Tier 1 recipes satisfy; documented as a data rule. Registered through `DeferredRegister` for `Registries.RECIPE_TYPE` and `RECIPE_SERIALIZER`.

### D2: Assembler block entity
`AssemblerBlockEntity extends PoweredMachineBlockEntity`: slots 0 to 3 inputs, 4 output. Holds `selectedRecipeId` (saved as a string), resolves the recipe lazily through the recipe manager and caches it until the id changes or the level reloads. `canWork` requires a resolved recipe, `matches`, and room for the result. `finishWork` consumes the counted ingredients across the input slots and inserts the result. Work time comes from the recipe, so `getData(DATA_WORK_TICKS)` returns the recipe time. Container data index 6 carries the selected recipe's index in the sorted recipe list, computed on the server; `DATA_COUNT` becomes 7.

### D3: Sorted recipe list
`AssemblingRecipes.sorted(Level)` returns all assembling recipes sorted by id. Client and server hold the same recipes, so an index is a stable reference for one session. The menu's `clickMenuButton(player, id)` selects `sorted.get(id)`, or clears when `id` equals the list size.

### D4: Screen
`MachineScreen.Layout` gains a background texture. `AssemblerScreen extends MachineScreen` with layout `POWERED` on `textures/gui/assembler.png` (inputs at 44/62 by 17/35, output at 116/35, arrow at 79/34, energy bar at 152/17). It draws a recipe panel right of the GUI: a dark box with result icons in three columns, the selected one highlighted, tooltips with result name and ingredients, click sends `handleInventoryButtonClick`.

### D5: Data and recipes
`AssemblingRecipeBuilder` in datagen writes recipes to `data/servus_manufacturing/recipe/assembling/<name>.json` without advancements. Crafting recipe for the assembler: iron ingots, copper ingots, redstone and a crafting table. Stats `data/servus_manufacturing/machine/assembler.json`: `energy_per_tick` 20, `energy_capacity` 8000.

## Pure Java vs Minecraft

All Minecraft. Verified by datagen output, server run over RCON (generator next to an assembler with ingots, recipe selected by `data merge` of the saved id, output checked after the recipe time; a cable chain variant for distant power), client launch for models and screen registration. The recipe panel interaction is a manual check.

## PLAN.md decisions settled

None. Recipe numbers follow the Tier 1 table.

## Risks / Trade-offs

- [Recipe index as selection id] -> Only used for the click round trip; the block stores the id. A datapack reload between opening and clicking could mis-select once; harmless.
- [Four input slots with sum matching] -> Simple and enough for Tier 1 recipes of up to four ingredients.
