# Design

## Context

The assembler crafts one selected assembling recipe from four slots. The user wants general crafting with a configured recipe list and a cap. Choices made with the user: pattern grid plus JEI transfer, cap of 2 for the Mk1, round robin.

APIs verified: `CraftingInput.of(width, height, items)`, `ShapedRecipe.getWidth/getHeight/getIngredients`, `CraftingRecipe.getRemainingItems`, `RecipeType.CRAFTING`, `AbstractContainerMenu.clicked`, `BlockEntity.getUpdateTag/getUpdatePacket`, `Level.sendBlockUpdated`, JEI `IRecipeTransferHandler` and `IRecipeTransferRegistration.addRecipeTransferHandler`, `RecipeTypes.CRAFTING`.

## Decisions

### D1: ConfiguredRecipe
Recipe id plus a template list of concrete stacks. Crafting: nine cells as laid out (pattern) or derived from the recipe's ingredients (JEI), shaped recipes keep their layout top left aligned. Assembling: one counted stack per ingredient. Resolved lazily against the recipe manager. Saved as NBT and sent to the client through the block entity update tag, so the GUI panel reads the client block entity.

### D2: Planning a craft
`plan(recipe)` gathers the template's item counts from the nine inputs with `IngredientGatherer` (item identity is item plus components), validates with the recipe itself (`CraftingRecipe.matches` on a `CraftingInput` of the template, or `AssemblingRecipe.matches` on the gathered stacks), computes result and remainders and checks that they fit. `canWork` keeps the active recipe while it stays plannable, otherwise picks the next plannable one with `RoundRobin` from the cursor. `finishWork` executes the plan and moves the cursor past the active index. Work time is the plan's time, so `workTicks()` depends on the active recipe.

### D3: Ghost grid
The block entity owns a nine slot pattern handler. The menu exposes it through `PatternSlot`s that refuse pickup and placement; `clicked` on a pattern slot copies one of the carried stack into the ghost or clears it. The slots sync like any menu slot. Plus (button 100) asks the server to match the pattern first against assembling recipes (counted cells) and then against crafting recipes; clear is button 101; buttons 0 to 8 remove a configured recipe.

### D4: Research gating moves to add time
A locked assembling recipe is refused on add with an action bar message. Crafting recipes are never gated.

### D5: JEI transfer
One `AssemblerRecipeTransferHandler` per JEI recipe type (crafting and assembling) sends `ConfigureRecipePayload(recipeId)`; the server adds the recipe to the assembler behind the player's open menu. No slot moving, so JEI shows no missing item errors.

### D6: Screen layout
Inputs 3 by 3 at 8/17, pattern grid at 62/17, plus at 124/17, output at 124/35, progress bar under the output, clear at 124/55, energy bar at 152/17, side panel left as before, configured recipe panel right with one cell per recipe slot.

## Pure Java vs Minecraft

- Pure Java, JUnit tested: `IngredientGatherer`, `RoundRobin`.
- Minecraft, verified over RCON with `data merge` of the recipe list: vanilla sticks from planks, gear and wire round robin, a cap and a lock message are manual checks together with the pattern grid and the JEI button.

## Risks / Trade-offs

- [Template fixes one ingredient variant] -> Documented; alternatives come with a later "any matching" mode if players ask.
- [Existing assemblers lose their selection] -> Test worlds only; noted in the proposal.
- [Nine capability lookups and plans per tick] -> Planning runs only while the machine has recipes and inputs; cheap at Tier 1 scale.
