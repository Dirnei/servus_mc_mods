# Design

## Context

See proposal.md for motivation. Manufacturing's Assembler shows the shape: `ConfiguredRecipe(id, template)` resolved against the recipe manager, a plan that gathers exact template counts from the inputs with Lib's `IngredientGatherer`, round robin over craftable recipes with Lib's `RoundRobin`, remainders through `getRemainingItems`, outputs pushed every 8 ticks into adjacent item handlers, and a sided handler that accepts only used items. Storage cannot reference those classes, so the station gets its own small versions. The Lib production capability exists since `m7-lib-production-api`; the Machine Dock feeds the block it faces through `Capabilities.ItemHandler.BLOCK` and collects through its own intake.

## Goals / Non-Goals

**Goals:**
- A producer that lives entirely in Storage, usable by Storage GameTests.
- Same contract as a machine: sided item handler, output push, production capability.

**Non-Goals:**
- See proposal.

## Decisions

### Settles no PLAN.md open decision

### Block entity
`CraftingStationBlockEntity` (package `io.github.dirnei.mc.servus.storage.crafting`) with `ItemStackHandler inputs(9)`, `outputs(4)`, `pattern(9)` ghost cells, `List<StationRecipe> recipes` where `record StationRecipe(ResourceLocation id, List<ItemStack> template)` with NBT save and load, `int progress`, `int cursor`, `int active`. `serverTick`: find the active recipe with `RoundRobin.next` over `plan(recipe).isPresent()`, where `plan` merges the template into `Map<ItemKey, Integer>`, gathers with `IngredientGatherer.gather` over the inputs, re-matches `CraftingInput.of(3, 3, template)` through `RecipeManager.getRecipeFor(RecipeType.CRAFTING, ...)` and checks the result and `getRemainingItems` fit the outputs by simulation; when `progress` reaches `StorageStatsManager.craftingStationTicks()` it takes the gathered counts, inserts result and remainders, advances the cursor. Every 8 ticks outputs are pushed into every adjacent `IItemHandler`. `saveFromPattern()` matches the pattern grid and adds a recipe (refusals: no match, duplicate, full, reported through the menu's action bar message); `removeRecipe(index)`. The block state has `FACING` and `LIT` (lit while a craft is in progress).

### Sided handler and capabilities
`sideHandler()` is an `IItemHandler` over inputs and outputs: `insertItem` only into input slots and only when `uses(stack)` (some configured template contains the item), `extractItem` only from output slots. `ServusStorageEvents` registers it for every side and registers `ServusLibCapabilities.PRODUCTION` with a `ProductionProvider` built from the recipes like the Assembler's.

### Menu and screen
`CraftingStationMenu`: nine `GhostSlot`s over the pattern (set from the cursor, clear with an empty cursor), input `SlotItemHandler`s, output slots (no placing), player inventory, buttons through `clickMenuButton`: `BUTTON_SAVE = 100`, `BUTTON_REMOVE_FIRST = 200 + index`; a `DataSlot` for progress and a message id `DataSlot` for the last refusal. `CraftingStationScreen` draws the grid at the left, the Save button under it, recipe cells (result icons) in a column at the right, inputs and outputs below, inventory at the bottom; clicking a cell sends the remove button.

### Data and datagen
`crafting_station.json` with `work_ticks` 40 and `recipes` 4, read by `StorageStatsManager`. Datagen: blockstate with `FACING` and `LIT` (an `orientedLit` variant of the existing helper), loot, pickaxe tag, recipe (crafting table over a chest framed by iron ingots), lang. Placeholder textures.

### Minecraft side verification
GameTests: `craftingStationCraftsConfiguredRecipe` (configure sticks from a pattern of two planks, put four planks in, expect four sticks after `work_ticks` and eight after twice), `craftingStationReportsProduction` (capability query), `craftingStationRefusesUnusedItems` (sided handler refuses cobblestone, accepts planks), `dockServesCraftingStation` (dock faces the station, planks fed through the dock, sticks arrive in the dock's collect buffer and then in a crate).

## Risks / Trade-offs

- [Template items, not tags] → like the Assembler; the pattern the player lays out is what gets consumed.
- [No power makes it a free Assembler for vanilla recipes] → it is slower by data (40 ticks against the Assembler's 1) and makes only vanilla crafting recipes; balance lives in JSON.

## Open Questions

None.
