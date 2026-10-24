# Design

## Context

- `CoagulationRecipe(input, ingredient, resultTemplate, time, chapter)`; `CoagulatorBlockEntity` keeps `selected` (a recipe id), reports it as `DATA_SELECTED` and saves it as `Recipe`; `CoagulatorMenu` turns button ids 99 and 100+ into `select`; `CoagulatorScreen` draws the select button and a `RecipePickerPopup`.
- The block entity is a Foundation `MachineBlockEntity` with 2 slots (0 ingredient, 1 output). Its `ItemPort` exposes 2 slots to every face; all side modes are closed (`AlchemyMachines.closeAllSides`), so the base class never pulls or pushes. Loading a saved inventory never resizes the machine, so a third slot is empty in old worlds.
- `MachineMenu.quickMoveStack` moves into machine slots in the order they were added, and `moveItemStackTo` respects a slot's `getMaxStackSize`.
- Coagulation recipes reach the client through `ServusRecipes.syncToClients`, so the screen, the renderer and JEI read the same recipes as the server.
- The formed casting table (`AlchemyModels.coagulator`) has its iron mold top at y 13 inside x and z 3 to 13; the middle Vat's spout (`vatModel` piece 1) runs from y -4 to 0, that is y 12 to 16 of the Coagulator block, through the middle of the mold.
- No shipped coagulation recipe names a chapter, and the server recipe map cannot be changed at runtime. NeoForge 26.1.2's `ModifyRecipeJsonsEvent` (on `NeoForge.EVENT_BUS`, fired in `RecipeManager.prepare` before parsing, map `Identifier -> JsonElement` changed in place) lets code add recipe JSON while recipes load.
- JEI 29.43 for 26.1.2: `IRecipeLayoutBuilder.addInputSlot(x, y)`; the roles are INPUT, OUTPUT, CRAFTING_STATION and RENDER_ONLY.

## Goals / Non-Goals

**Goals:**
- What lies in the mold decides what grows, for players, pipes and the Crafting CPU alike.
- Seed lookup and the uniqueness rule are pure, tested and the same on server, client and in datagen.

**Non-Goals:**
- Several seed slots, seeds for other machines.

## Decisions

**Recipe field.** `CoagulationRecipe` gets `Optional<Ingredient> seed` (codec `seed`, optional; stream codec `Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC`), placed before `chapter`. `seedIngredient()` is the seed or `Ingredient.of(result item)`. Datagen writes `seed` only when it differs from the default.

**Pure index.** `alchemy/coagulator/SeedIndex<R>` (no Minecraft imports; JUnit `SeedIndexTest`):
- `record Entry<R>(String recipeId, List<String> seedItems, R recipe)`.
- `SeedIndex.build(entries)` sorts the entries by recipe id; each seed item goes to the first entry that names it; a later entry naming it is recorded as `Conflict(item, keptRecipe, rejectedRecipe)` and does not get it.
- `lookup(item)` gives the recipe, `seedsOf(recipeId)` the kept items in the order given, `conflicts()` every conflict in a fixed order.
- Tests: default and explicit seeds, lookup of an unknown item, a conflict keeps the first id whatever the input order, the conflict list names both, an entry that loses all its seeds can no longer be found.

`alchemy/coagulator/SeedText.line(seedLabel, resultLabel)` gives "Kandis grows Salt Crystal" (JUnit `SeedTextTest`).

**Index per recipe map.** `alchemy/coagulator/CoagulatorSeeds` builds the index from `ServusRecipes.all(level, COAGULATION)`, with each recipe's `seedIngredient().items()` as item ids, and caches it against the `RecipeMap` instance (a reload makes a new map). On a server level it logs every conflict once per build with `LOGGER.error("Seed {} of coagulation recipe {} is already the seed of {}; ignored", ...)`. API: `recipeFor(level, stack)`, `isSeed(level, stack)`, `seedsOf(level, recipeId)`.

**Datagen check.** `AlchemyRecipeTable.Coagulate` gets `List<Item> seeds` (empty means its result). `ServusAlchemyRecipeProvider.coagulation` builds a `SeedIndex` from the rows and throws `IllegalStateException` naming the item and both rows if it has conflicts, so `runData` fails. Shipped seeds: `salt_crystal` = Kandis, Salt Crystal; `sulfur` = Brimstone, Sulfur; `quicksilver` = Cinnabar, Quicksilver.

**Seeds are used up.** Every finished cycle takes one seed out of slot 2. `CoagulationRecipe.resultFrom(seed)` is the result, grown by one when `selfSeed(seed)` (the seed is the result item), so a transmutation seed (Kandis) turns into the product and a self seed (Salt Crystal, Redstone) comes back doubled. The rule is per seed item, not per recipe: the shipped Salt Crystal recipe names Kandis and Salt Crystal as seeds. This keeps the crafting side simple: autocrafting always feeds one seed per batch. The Journal's `coagulate` record counts what came out.

**Block entity.** Slot 2 is the seed (`SLOT_SEED`), an input slot that accepts any item `CoagulatorSeeds.isSeed` knows, up to its stack size and only of the kind already in it. The pure `alchemy/coagulator/SeedSlot.room(held, sameKind, maxStackSize)` (JUnit `SeedSlotTest`) says how many more fit; `seedRoom(stack)` uses it for the port, the block and `acceptsInput`, and `plantSeed(stack)` puts that many in. `selected`, `select`, `DATA_SELECTED` and the saved `Recipe` go. `seedRecipe()` resolves the seed through `CoagulatorSeeds`; `growingRecipe()` is the seed's recipe when it may grow. `canWork` needs `growingRecipe()`, and the ingredient slot accepts what `seedRecipe()` takes. Menu data gets `DATA_LOCKED` (1 while the seed's recipe waits for a chapter). The production view reports `growingRecipe()` with one seed among the inputs and `resultFrom(seed)` as the output.

**Item port and pipes.** The port has 3 slots: 0 ingredient (insert only), 1 output (extract only), 2 seed (insert only, a stack of one seed item, never extracted). NeoForge's handlers and Foundation's pipes try slots in index order, so an item the seed's recipe takes as its ingredient fills slot 0 before it could become a seed, and a seed only lands in an empty seed slot or one holding the same item. Nothing automated takes the seed out, so a hopper under the Coagulator cannot stop it by accident; players take it out by hand. Accepting seeds from pipes means a pipe that delivers products of the same kind to an empty Coagulator also seeds it, with as many as it pushes; that is the like begets like rule and is shown in the book.

**By hand.** `CoagulatorBlock.useItemOn`: the Journal forms; seed items on a formed Coagulator go in as far as `seedRoom` allows; anything else opens the GUI as before. `useWithoutItem` while sneaking gives all seeds back (to the hand, or dropped when the inventory is full); without sneaking it opens the GUI. The menu's `SeedSlot` (a `MachineSlot` that accepts any seed, so a click swaps one seed kind for another) lets the player put seeds in and take them out.

**GUI.** The texture `textures/gui/coagulator.png` gets a third slot frame under the arrow (slot at x 82, y 54), the select button area goes. `CoagulatorScreen` drops the popup and all its input handling; Foundation's `RecipePickerPopup` has no other user and is deleted. Hovering the seed slot replaces the vanilla tooltip: the result's name ("Grows Salt Crystal"), the needs lines of the old recipe tooltip, and the chapter line in red while `DATA_LOCKED` is 1; an empty seed slot says "Seed: a crystal of what should grow"; an item that is no seed cannot be put in.

**Growing look.** The update tag carries `Seed` (item id), `Growing` (a cycle is running), `Progress` and `WorkTicks`. The server resends when the seed, the running state or the cycle restarts change, about once per cycle, not every tick; the client takes the receive time and counts on locally, so the crystal grows smoothly. `CoagulatorRenderer` draws the seed item (`RenderSubmit.item`, `ItemDisplayContext.FIXED`) lying flat in the middle of the mold, turned with the build's facing, scaled from 0.3 to 0.6 with the cycle's share (4.8 to 9.6 px wide, inside the 10 px mold).
- Z-fighting check: the item model is 1 px thick, so at scale 0.6 it spans 0.6 px; its centre sits at y 13.35, its bottom at 13.05 or higher, 0.05 px above the iron top at 13, so no face shares a plane with the mold. The middle Vat's spout is shortened to y -2 to 0 of the Vat (14 to 16 of the Coagulator block), so its bottom face at 14 stays above the crystal's highest point (13.65) and its top face still meets the Vat floor's underside as before (back to back and hidden, unchanged). The spout no longer crosses the iron mold box, which removes the old overlap of spout and mold faces at y 12 and 13.

**JEI.** `CoagulationRecipeCategory` grows to 52 px high: the seed slot at (1, 30) with the seed items as an input and a tooltip line saying one is used up per cycle and what it gives, "Seed" written beside it at (21, 35), the time stays under the arrow. A recipe whose only seed is its result shows the result with one more; a recipe with a self seed among others names the doubled count in the result's tooltip. Uses of Kandis show the Salt Crystal recipe.

**Book.** `WorkshopCategory.coagulator()` rewrites "Joining" for seeds (put seeds of what should grow into the seed slot, one is used up per cycle and comes back doubled, the Coagulator stops when they run out, take them out to stop sooner; pipes may add seeds of the same kind) and says that a stranger seed turns into one crystal of the product and adds a "Seeds" list page with `SeedText.line` for every shipped seed that is not the recipe's own result, closed by "Every other product grows from a crystal of itself." in the text. The "What It Makes" lines stay as they are (their format belongs to `alchemy/journal`, which `m9-calcination-table` also changes).

**Gametest recipes.** `gametest/GameTestRecipes` registers a `ModifyRecipeJsonsEvent` listener from the Alchemy mod constructor only when the system property `servus.gametest.recipes` is true, which the root `build.gradle` sets for `gameTestServer` and `gameTestServerAll` alone. It adds `servus_alchemy:gametest/locked_seed` (50 mB Salt for an Echo Shard, chapter Villach, seed Echo Shard) and `servus_alchemy:gametest/duplicate_seed` (50 mB Salt for a Flint, seed Kandis, which `coagulation/salt_crystal` keeps). Play and production runs never see them.

**Verification.** GameTests:
- `kandisSeedsAreUsedUp`: 3 Kandis and 1,000 mB Salt make three Salt Crystals, the seed slot ends empty and 250 mB Salt stay.
- `redstoneSeedsDouble`: 2 Redstone and the principles of three cycles make four Redstone, the seed slot ends empty and one cycle of principles stays.
- `productionListsTheSeed`: Kandis reports one Kandis in and one Salt Crystal out; a Salt Crystal seed reports one Salt Crystal in and two out.
- `noSeedDoesNothing`: 250 mB Salt, no seed, nothing made after 100 ticks, Salt unchanged.
- `lockedSeedGrowsNothing`: Echo Shard seed, a fresh team, 50 mB Salt, nothing after 100 ticks, `DATA_LOCKED` is 1; finishing Villach for the team then grows the Echo Shard.
- `duplicateSeedStaysWithTheFirstRecipe`: `CoagulatorSeeds.recipeFor(Kandis)` is `coagulation/salt_crystal`, `gametest/duplicate_seed` has no seeds.
- `pipeAddsSeedsButNeverTakesThem`: the port takes 16 Kandis into the empty seed slot and 4 more on top, refuses Brimstone and extracts nothing from slot 2.
- `pipeLaysAStackOfSelfSeeds`: the port takes all of 16 Salt Crystals.
- `seedByHand`: 3 Cinnabar used on the table all become the seed, Brimstone is refused, sneaking with an empty hand gives all 3 back.
- Existing tests that selected a recipe put the recipe's result in as the seed; `noRecipeStopsTheCoagulator` becomes `takingTheSeedOutStopsTheCoagulator`; `coagulatorKeepsAndReportsItsSelection` becomes `coagulatorKeepsItsSeedAndReportsItsRecipe`.

## Risks / Trade-offs

- [A pipe of Salt Crystals into an empty Coagulator seeds it with all of them] → by design; the book says so, and the player takes them out by hand.
- [Old worlds lose their selection] → the mod is unreleased for this machine's players; the Coagulator simply waits for a seed.
- [A datapack recipe whose seed is taken loses it] → the error log names both recipes; JEI shows the seeds each recipe really has.
- [Changing the shared Vat model] → only the middle piece's spout gets shorter; the left and right spouts and the plain Vat stay.
