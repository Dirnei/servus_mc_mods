# Design

## Context

- `StorageTerminalScreen.orderResult()` computes the grid's result on the client with `ServusRecipes.displayResult`, the recipe's display stack without components. The server's `StorageTerminalMenu.orderPreview` answers with `PatternEditing.encode(...).result()`, which is `EncodedPattern.assembledResult`: the stack `CraftingRecipe.assemble` makes, carrying `servus_shuttles:rack_wood` or `servus_foundation:material`. `currentPreview()` drops any preview whose item is not `ItemStack.isSameItemSameComponents` with the client's result, so for `HeavyRackRecipe` and every `MaterialRecipe` it returns null every frame: the Save button needs a preview and stays hidden, and Start shows `craft_wait` ("Calculating...").
- `ProductionTerminalScreen.preview()` uses the same display result; only the picture is wrong there, its Save goes to the server.
- `ProductionTerminalMenu.openEditorWith` fills from `CraftingGrid.templateOf`, which takes `ServusRecipes.firstItem` of each ingredient: the tag's first item and a bare item without components.
- `StorageTerminalMenu.fillGhosts` gets JEI's displayed stacks per cell and picks the one with the highest `ItemKey` count. `ItemKey` includes components, so a bare Rail from JEI never matches the stocked oak Rail and counts 0.
- Crafting recipes are synced to clients (`ServusRecipes.syncToClients(() -> RecipeType.CRAFTING)` in `ServusShuttles`), so `ServusRecipes.byId` and `CraftingRecipe.assemble` work on the client level.

## Goals / Non-Goals

**Goals:**
- One function decides the grid's result, used by both screens and by the server, so client and server compare equal stacks.
- One stock-aware cell choice used by both JEI fills.

**Non-Goals:**
- Any-variant patterns and planner changes (`m8-pattern-variants`).

## Decisions

1. **Client result through the server's function.** Both screens call `PatternEditing.encode(level, cells).map(EncodedPattern::result)` instead of `CraftingGrid.match(...).map(displayResult)`. `encode` uses only `ServusRecipes` lookups and `CraftingRecipe.matches/assemble`, which run on a client level with the synced crafting recipes. Alternative: loosen `currentPreview()` to compare items only. Rejected: the result icon and tooltip would still show the plain rack, and two racks of different wood could swap previews.

2. **Cell choice matches stock by item, keeps the stock's components.** A shared helper beside `CraftingGrid` (for example `CraftingGrid.stockedChoice`) takes the cell's accepted items and the warehouse's requestable stock (`TerminalWarehouses.stock`), keeps the stock entries whose item is accepted, and returns the stack of the entry with the highest count, components included. Without any, it returns the first accepted item. The Storage Terminal passes JEI's displayed stacks as the accepted items; the Production Terminal passes the recipe ingredient's items (`ServusRecipes.items(Ingredient)`) with the shaped layout `templateOf` already computes. Matching on the item and not the `ItemKey` is what lets a bare Rail from JEI find the oak Rail in stock. Alternative: `Ingredient.test` on every stock entry. Equivalent for the Production Terminal, but the Storage Terminal only has JEI's stacks, not the ingredient; the item set works for both.

3. **Picking the highest count is pure logic.** The choice among `(key, count)` candidates in stock order, with ties to the first, goes into a small generic class with no Minecraft imports (for example `shuttles/terminal/MostStocked`), unit tested. The menu code only maps stacks to keys and back.

4. **Production Terminal finds its warehouse like the Storage Terminal.** `TerminalWarehouses.online(level, terminal.getBlockPos())` works for any member of a data line. No warehouse or a line in conflict means no stock, so cells fall back to the first accepted item.

No PLAN.md open decision is settled here. NeoForge and vanilla APIs used: `CraftingRecipe.matches/assemble(CraftingInput)`, `Ingredient.items()`, `ItemStack.isSameItemSameComponents`, already in use in `EncodedPattern` and `ServusRecipes`; no new events or capabilities.

Testing: `MostStocked` with JUnit. GameTests on the server side: the shared result function returns the oak rack for the rack grid and equals the server preview item; `fillGhosts` with a bare Rail option picks the stocked oak Rail; `openEditorWith` on the rack recipe with only spruce planks stocked fills spruce. The screens themselves are checked in game.

## Risks / Trade-offs

- [Client `assemble` reads registries that differ from the server] -> both sides have the same synced recipes and built-in registries; the server's preview is still the authority for what is crafted.
- [Encoding runs every frame on the client] -> `encode` is a recipe lookup plus one `assemble`; cache the result by the cells as `askedGrid` already does if a frame cost shows up.
- [Old patterns saved with a bare Rail stay unusable] -> out of scope; the player saves them again, which now picks the stocked Rail.
