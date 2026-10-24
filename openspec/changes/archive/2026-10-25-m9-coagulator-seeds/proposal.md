# Proposal

## Why

The Coagulator is picked like a vending machine: a popup lists every recipe and a click decides what comes out. That is neither alchemy nor fun, and it says nothing about where a product comes from. Alchemists grew crystals from a seed: like begets like. With Kandis, Brimstone and Cinnabar from `m9-purifier-washes` the first seeds of Salt Crystal, Sulfur and Quicksilver exist, so the Coagulator can grow what lies in its mold instead.

## What Changes

- The recipe popup, the "No recipe" entry, the select button and the persisted selection go. A saved selection is ignored; a Coagulator from an older world needs a seed.
- The Coagulator gets a seed slot holding a stack of one seed item. The seed decides which coagulation recipe runs; without a seed nothing runs; taking the seed out stops it.
- A coagulation recipe gets an optional `seed` ingredient. Without one its seed is its own result item (like begets like). The shipped first seeds: Salt Crystal grows from Kandis or a Salt Crystal, Sulfur from Brimstone or Sulfur, Quicksilver from Cinnabar or Quicksilver.
- One seed item belongs to exactly one recipe. Datagen fails on a duplicate; recipe loading logs it and keeps the seed with the recipe whose id sorts first, deterministically. The lookup is pure Java with JUnit.
- Every finished cycle uses up one seed. A transmutation seed (Kandis, Brimstone, Cinnabar) turns into the result; a seed that is the result itself comes back with it, so the cycle gives one more (one Redstone seed becomes two Redstone). The Coagulator stops when the seeds run out. The consumed item ingredient stays as it is.
- A seed of a chapter locked recipe grows nothing until the owner's team has finished the chapter; the GUI names the chapter.
- A formed Coagulator takes seeds through the item capability, into an empty seed slot or onto the same kind, besides the ingredient; another kind is refused while one is in. Automation never takes the seeds out; a player takes them out in the GUI or by sneaking with an empty hand on the Coagulator. Using seed items on a formed Coagulator puts as many in as fit.
- The seed crystal lies in the mold and grows with the progress of a cycle.
- GUI: seed slot under the arrow, ingredient and output slots, progress, gauges; hovering the seed slot says what it grows, what that needs and which chapter it waits for.
- The Coagulator book entry explains seeds and lists the transmutations; JEI shows the seed in the Coagulation category; the production view reports the seed's recipe with one seed among the inputs.
- GameTests: 3 Kandis grow 3 Salt Crystals and are used up; 2 Redstone seeds give 4 Redstone; pipes add to the same seed and refuse another; no seed does nothing; a locked recipe's seed does nothing; a duplicate seed stays with the first recipe.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `alchemy/coagulator`: recipes get a seed; the seed replaces the recipe choice, the stop entry and the popup; uniqueness, locking, growing look, GUI, book list.
- `alchemy/coagulator-structure`: the item port takes seeds into the seed slot, a stack of one kind.
- `alchemy/recipe-viewer`: the Coagulation category shows the seed.
- `alchemy/journal`: chapter recipes are locked by seed instead of by selection.
- `alchemy/data-line`: the Coagulator reports its seed's recipe.

## Impact

- `CoagulationRecipe`: `seed` field, `seedIngredient()`.
- New pure `alchemy/coagulator/SeedIndex` and `SeedText` with JUnit; `CoagulatorSeeds` builds the index per recipe map and logs duplicates.
- `CoagulatorBlockEntity`: seed slot 2, no selection, lock state in the menu data, client sync of the seed and the cycle; `CoagulatorMenu`, `CoagulatorScreen` (no popup), `CoagulatorBlock` (seed by hand), `CoagulatorRenderer` (growing seed), the Principle Vat model (shorter middle spout so the crystal fits under it), the GUI texture.
- `AlchemyRecipeTable.Coagulate` gets seeds; the recipe provider checks uniqueness and writes `seed`.
- `CoagulationRecipeCategory`, `WorkshopCategory.coagulator()`, lang.
- Foundation's `RecipePickerPopup` loses its only user and is deleted.
- GameTests in `CoagulatorGameTests` and `AlchemyGameTests`; a gametest only recipe pair added through NeoForge's `ModifyRecipeJsonsEvent` when the gametest runs set `servus.gametest.recipes`.
- `alchemy/CHANGELOG.md` gets its line at archive time.

## Non-goals

- More than one seed slot, or several recipes per seed.
- New coagulation recipes beyond the three transmutations.
