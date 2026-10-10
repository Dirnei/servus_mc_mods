# Design

## Context

See proposal.md for the motivation. Current state:

- `ServusLogisticsItems.FACADE` is a plain `Item` registration. The facade's block is the data component `ServusLogisticsComponents.FACADE_BLOCK`. A stack without the component counts as stone (`FacadeItem.blockOf`).
- `ServusLogisticsEvents.onBuildCreativeTab` adds `ServusLogisticsItems.CREATIVE_TAB_ORDER` to the Servus tab. That list holds the lines and the Fluid Tank but not the facade.
- `ServusFoundationJeiPlugin.registerRecipes` adds a single example shapeless recipe, Stone Bricks plus an Iron Ingot giving `FacadeItem.of(STONE_BRICKS, FacadeRecipe.RESULT_COUNT)`.
- JEI 19.57.0.451 is a compile only API dependency in `foundation`. Manufacturing brings JEI into the dev runs.

## Goals / Non-Goals

**Goals:**
- One list of example blocks drives the creative tab, the JEI recipes and the information page, so they never drift apart.

**Non-Goals:**
- A dynamic list of all full blocks, and a block rendering facade icon (see the proposal's non-goals).

## Decisions

### D1. One example list on `FacadeItem`

`FacadeItem.EXAMPLES` is a `List<Block>`: `STONE`, `STONE_BRICKS`, `OAK_PLANKS`, `BRICKS`, `DEEPSLATE_TILES`. A static helper, `FacadeItem.examples()`, returns one-count stacks via `FacadeItem.of`.

The list is fixed in code, not datapack JSON. It is presentation, not balance, and the creative tab is built before datapacks load on the client.

**Alternative:** a block tag. Rejected because tags are not bound when the creative tab first builds in some contexts, and the list is cosmetic.

### D2. Creative tab

`ServusLogisticsEvents.onBuildCreativeTab` calls `event.accept(stack)` for each example stack after `CREATIVE_TAB_ORDER`. `BuildCreativeModeTabContentsEvent.accept(ItemStack)` is the NeoForge 1.21.1 overload for stacks with components.

JEI builds its item list from creative tab contents, so this alone makes the facades show up.

### D3. JEI subtype interpreter

The plugin overrides `IModPlugin.registerItemSubtypes(ISubtypeRegistration)` and calls `registerSubtypeInterpreter(ServusLogisticsItems.FACADE.get(), interpreter)`. The interpreter is an `ISubtypeInterpreter<ItemStack>` that returns:

- from `getSubtypeData`: `FacadeItem.blockOf(stack)`, a `Block`, which is a stable identity;
- from `getLegacyStringSubtypeInfo`: the block's registry id as a string.

Both signatures were checked with `javap` against `jei-1.21.1-common-api-19.57.0.451.jar` in the Gradle cache.

### D4. One example recipe per facade

`registerRecipes` builds one `ShapelessRecipe` per example block, with the inputs `Ingredient.of(block)` and `Ingredient.of(Items.IRON_INGOT)` and the result `FacadeItem.of(block, FacadeRecipe.RESULT_COUNT)`. The id is `jei/facade_<block path>`. They are registered with `RecipeTypes.CRAFTING` like today.

**Alternative:** one recipe with a cycling ingredient. Rejected because the result would not cycle with the input, and the recipe would show oak planks giving stone brick facades.

### D5. Information page

`registration.addItemStackInfo(FacadeItem.examples(), Component.translatable("jei.servus_foundation.facade.info"))`, through `IRecipeRegistration.addItemStackInfo(List<ItemStack>, Component...)`, also checked with `javap`.

The English text comes from language datagen in `LogisticsDataParts`: "Craft any full block without a block entity with one iron ingot to get six facades of that block. Use a facade on a Utility Duct to hide it; sneak with the Configuration Carrot to take it off."

### PLAN.md open decisions

None are touched. The change adds M9 item 1c.

### Pure versus Minecraft

- No pure logic.
- GameTest `facadeExamplesAreCraftable`: for every block in `FacadeItem.EXAMPLES`, the facade recipe crafts six facades of that block. This keeps the list valid if the recipe rules change.
- Manual with JEI in the dev client: the creative tab, the search, the recipes and the information page.

## Risks / Trade-offs

- [The facade icon is the same for every example, so JEI shows five identical icons] → The names differ, and the information page explains. A block rendering icon is a non-goal for now.
- [A stack without the component behaves as stone and would be a duplicate entry] → It is never added to the tab. The interpreter maps it to stone, so it merges with the stone example if it shows up anywhere.
