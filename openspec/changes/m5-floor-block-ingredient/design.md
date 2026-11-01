# Design

## Context

See proposal.md for the bug. Today:

- `FluidTankRecipe`, `FacadeRecipe` and `HollowFacadeRecipe` extend `CustomRecipe`. They are written by `SpecialRecipeBuilder` in `LogisticsDataParts` as `{"type": "servus_foundation:fluid_tank"}` and so on, decoded with a unit codec, and have no `placementInfo` ingredients.
- `ServusFoundationJeiPlugin.registerRecipes` adds display-only crafting recipes: `fluid_tank_example`, `facade_<block>` and `hollow_facade_<block>`. Facades have a JEI subtype interpreter by block, so the per-block examples are what "Facade (Oak Planks)" lookups find.
- Both terminal transfer handlers send only `recipe.id()`. The server runs `CraftingGrid.templateOf(level, id, stock)`, which needs a server recipe with readable ingredients. It finds nothing for the stand-in ids and silently does nothing.
- The Crafting Station already crafts by real recipe id with `matches` and `assemble`, so hand-made tank and facade patterns work today.

## Goals / Non-Goals

**Goals:**
- Real, readable recipes for the tank, Facade and Hollow Facade under their current ids.
- JEI "+" works at both terminals for these recipes and for any display-only crafting recipe.

**Non-Goals:**
- No change to the floor and facade block rule, results or counts.
- No EMI or REI transfer handlers.

## Decisions

### 1. `servus_foundation:floor_block` as a NeoForge custom ingredient

`FloorBlockIngredient implements ICustomIngredient` (NeoForge 26.1, `net.neoforged.neoforge.common.crafting.ICustomIngredient`, checked in the 26.1.2 sources jar). It is registered as an `IngredientType` in `NeoForgeRegistries.INGREDIENT_TYPES` from Foundation, with a `MapCodec.unit` codec and a unit stream codec, since it has no settings.

- `test(stack)`: the item is a `BlockItem` whose block passes the floor rule.
- `items()`: every registered item that passes `test`. `Ingredient` caches the list itself (stated in the interface javadoc), and it is first asked after registries are frozen.
- `isSimple()`: true, because the answer depends only on the item. Vanilla's stacked-contents matching can then use it.
- `display()`: the default composite of `items()`, so JEI cycles through every floor.

The floor rule moves from `FacadeRecipe.isFacadeable` to the ingredient as a static `accepts(Block)`. Facade placement and tank code call that, so one rule serves recipes and placement.

Alternative: a block tag filled by datagen. Rejected, because the tag would only cover vanilla blocks and blocks from mods we know; the rule must accept blocks from any mod in the pack.

### 2. Recipes subclass vanilla recipe classes, like `HeavyRackRecipe`

- `FluidTankRecipe extends ShapedRecipe`: `CGC` / `G G` / `CFC` with copper ingot, glass and the floor ingredient. `assemble` calls `super.assemble` and sets the floor read from the bottom middle cell (`FluidTankItem.withFloor`). The pattern is symmetric, so a mirrored match reads the same cell.
- `FacadeRecipe extends ShapelessRecipe`: the floor ingredient plus `#c:nuggets`, result six `facade`. `assemble` copies the block onto the result (`FacadeItem.of`).
- `HollowFacadeRecipe extends ShapedRecipe`: a ring of `Ingredient.of(facade)` around an empty centre, result eight `hollow_facade`. It overrides `matches` to also require one block across the ring, and `assemble` copies that block.

Each keeps its own serializer id (`servus_foundation:fluid_tank`, `facade`, `hollow_facade`) with the shaped or shapeless codec decoding to the subclass, the same way `HeavyRackRecipe` and `FluidRackRecipe` do in Shuttles. Datagen builds the subclass directly (`ShapedRecipePattern.of` plus the subclass constructor) and hands it to `RecipeOutput.accept` without an advancement, like the special recipes before. This writes the same shaped and shapeless JSON as the builders and avoids reading private fields of the built `ShapelessRecipe`. The recipe file ids stay `servus_foundation:fluid_tank`, `facade` and `hollow_facade`, so saved patterns that name them keep working.

### 3. JEI: keep per-block facade examples, hide the generic facade recipes

The real tank recipe is shown as is, and `fluid_tank_example` is removed. The generic Facade and Hollow Facade recipes have results without a block, so JEI would list a nameless facade, and lookups by block would not find them. They are hidden with `IRecipeManager.hideRecipes(RecipeTypes.CRAFTING, ...)` in `onRuntimeAvailable` (JEI 26.1.2 common-api 29.43.0.107, checked with javap). JEI keeps hidden recipes in an identity set, so the plugin takes JEI's own holders from `createRecipeLookup(RecipeTypes.CRAFTING).get()` and hides those whose value is a `FacadeRecipe` or `HollowFacadeRecipe`. The `facade_<block>` and `hollow_facade_<block>` example recipes stay.

### 4. Transfer: the recipe id when the server knows it, else the shown stacks

`OrderGridRecipePayload` and `OpenRecipeEditorPayload` carry the recipe id plus nine cells. For each cell the payload holds the item stacks JEI shows (`IRecipeSlotsView.getSlotViews(RecipeIngredientRole.INPUT)`, `IRecipeSlotView.getItemStacks()`, checked with javap), at most 64 per cell.

On the server:
- **Known id** (a crafting recipe in the server's recipe map): `CraftingGrid.templateOf` as today. It reads the full ingredient, so the tank's floor cell considers every floor block, not just 64 of them.
- **Unknown id** (display-only recipes such as the facade examples): the new `CraftingGrid.templateOfShown(cells, stock)`. Per cell it picks the stocked stack matching a shown stack, most stocked first, else the first shown stack. A shown stack with a non-default component patch matches only stock with the same components (so oak and brick Facades are told apart). A plain shown stack matches stock of that item with any components (so oak Rails still count as Rails).

The shown path relies on JEI's vanilla crafting category giving nine input slot views in grid order. Verified at apply time in JEI 26.1.2 (29.43.0.107): `CraftingGridHelper.createInputSlots` adds nine input slots row by row (y outer, x inner), and `getCraftingIndex` places a smaller shaped or a shapeless recipe into those 3x3 positions, so slot views map to cells by index. The client sends the cells through one helper (`ShownCells`) shared by both transfer handlers, and the payload codec decodes stacks with the untrusted item stack codec since the client sends them.

Alternative: always send the shown stacks. Rejected, because a floor cell in a big pack lists thousands of blocks, and serverbound custom payloads are size limited. The cap of 64 is enough for display-only recipes, which list few items per cell.

Alternative: resolve the stocked choice on the client. Rejected, because the Production Terminal's client does not hold the line's stock.

### 5. Pure logic and tests

- The cell choice (match shown stacks against stock, most stocked first, component rule) touches `ItemStack` and stays in Shuttles. `MostStocked.pick` already holds the ordering. It is covered by GameTests, not JUnit.
- Recipe behaviour is covered by GameTests in Foundation: the tank with a stone brick floor, the glass floor refused, a facade from stone plus a gold nugget, a slab refused, the stone hollow ring, the mixed ring refused, and an ingredient check that the floor ingredient lists stone bricks and not glass. The existing `instanceof FacadeRecipe` and `instanceof FluidTankRecipe` checks keep compiling.
- Transfer behaviour: GameTests on `StorageTerminalMenu.fillFromRecipe` and `ProductionTerminalMenu.openEditorWith` with a known id (tank, cobblestone stocked) and with an unknown id plus shown cells (brick Facade ring with stone and oak Facades stocked). The JEI button itself is checked in the play check.

## Risks / Trade-offs

- [Any patterns on the tank now see every floor block as an alternative. `PatternVariants.of` assembles once per accepted item, which means thousands of calls in a big pack] → Measure the save and order grid preview with the tank in the play check. If it is slow, cache `PatternVariants` per pattern and recipe-map generation.
- [The tank becomes a material pattern like the racks (the floor follows the input)] → This is the existing material rule. Check in the play check that the craftable list shows one "Fluid Tank" entry.
- [Recipes in other datapacks or KubeJS scripts that target the empty `servus_foundation:fluid_tank` type] → Unlikely. A recipe file with the old empty body fails to decode and is skipped with a log line. Note it in the CHANGELOG line.
- [JEI slot order differs from grid order] → Verify at apply time (decision 4) before writing the shown path.
- [`isSimple` true while `items()` is computed from the block rule] → It is consistent as long as the rule depends only on the item's block's default state, which it does.

## Migration Plan

No world data changes. Item components (`floor`, facade block) are untouched. Shuttles' payload change is client and server of the same mod, so it needs no Foundation bump. The tank transfer needs the new Foundation recipe to work, so release Foundation and Shuttles together. Shuttles' minimum Foundation version does not have to rise, because older Foundation only means the tank keeps using the shown path.
