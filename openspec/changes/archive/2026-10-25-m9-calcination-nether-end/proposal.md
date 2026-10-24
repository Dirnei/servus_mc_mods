# Proposal

## Why

The Calciner only knows Overworld matter: nothing from the Nether or the End burns, although Nether matter is the natural source of Sulfur and End matter of Mercury. Adding them exposes a hidden bug: when two calcination recipes match one item (Nether Wart is in `#c:crops`, Crimson and Warped Stems are in `#minecraft:logs`), the Calciner takes whichever recipe the recipe map lists first, so the result depends on load order and JEI shows the general recipe as if it applied.

## What Changes

- When several calcination recipes match an item, the most specific one wins: a recipe that names items directly beats a tag, and of two tags the one with fewer items wins, so a tag that is a subset of another beats the bigger tag. Ties go to the recipe id. The choice never depends on load order, and the Calciner, JEI and the journal agree on it.
- JEI's Calcination category shows in a recipe's input slot only the items that recipe really burns: Nether Wart no longer cycles through the Crops recipe.
- A calcination recipe gets an optional `dimension` field (`overworld`, `nether` or `end`, default `overworld`) that says where its matter comes from. The journal uses it in `m9-calcination-table` to group its table; it changes nothing at the machine.
- New shipped recipes, numbers are datapack examples. Nether leans to Sulfur: Netherrack, Soul Sand and Soul Soil, Nether Wart, Nether Wart Block, Crimson Stems, Warped Stems, Blaze Rod, Magma Cream, Glowstone Dust, Nether Quartz. End leans to Mercury: End Stone, Purpur Block, Chorus Fruit, Popped Chorus Fruit, Ender Pearl, Shulker Shell.
- Dragon's Breath stays unburnable; it is kept for the Prelum (`alchemy_plan.md`).

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `alchemy/calciner`: "Calcination recipes are data" gains the `dimension` field; new requirements for the most specific recipe and for the shipped Nether and End matter.
- `alchemy/recipe-viewer`: the Calcination category shows only the items a recipe really burns.

## Impact

- `CalcinationRecipe`: optional `dimension` field in the codec and the stream codec.
- New pure ranking class with JUnit tests; a recipe lookup helper used by `CalcinerBlockEntity`, the JEI category and later the journal.
- Foundation `ServusRecipes`: exposes the recipe map of a level so the lookup can cache per map.
- `AlchemyRecipeTable` and `ServusAlchemyRecipeProvider`: new rows with their dimension; generated recipe JSON.
- The journal's "What Burns" list grows by the new rows (it is replaced by a table in `m9-calcination-table`).
- GameTest: Nether Wart burns with its own recipe, not the Crops recipe.
- `alchemy/CHANGELOG.md` gets its line at archive time.

## Non-goals

- Specificity for other recipe types (Athanor, Purifier, Prelum); none of them overlap today.
- The journal table and the per team reveal (`m9-calcination-table`).
- NeoForge's `recipe_priorities.json`; the Calciner does not read it (see design).
