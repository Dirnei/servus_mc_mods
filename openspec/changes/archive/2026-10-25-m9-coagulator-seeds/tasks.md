# Tasks

## 1. Pure logic

- [x] 1.1 Add pure `alchemy/.../alchemy/coagulator/SeedIndex.java` (`Entry`, `Conflict`, `build`, `lookup`, `seedsOf`, `conflicts`) with JUnit `SeedIndexTest`: lookup of explicit and single seeds, unknown item, a conflict keeps the recipe whose id sorts first in any input order, the conflict names item and both recipes, a recipe that lost all seeds is not found. Verify: `gradlew :alchemy:test` green.
- [x] 1.2 Add pure `alchemy/.../alchemy/coagulator/SeedText.java` with JUnit `SeedTextTest`. Verify: `gradlew :alchemy:test` green.

## 2. Recipes

- [x] 2.1 `CoagulationRecipe`: optional `seed`, `seedIngredient()`; `CoagulatorSeeds` (index per recipe map, error log on conflicts, `recipeFor`, `isSeed`, `seedsOf`). Verify: `gradlew build` green.
- [x] 2.2 `AlchemyRecipeTable.Coagulate` seeds; `ServusAlchemyRecipeProvider` checks uniqueness with `SeedIndex` and writes `seed` for Salt Crystal, Sulfur and Quicksilver. Verify: `gradlew :alchemy:runData` writes the three `seed` fields, `gradlew build` green.

## 3. Machine

- [x] 3.1 `CoagulatorBlockEntity`: seed slot, no selection, `seedRecipe`, `growingRecipe`, `DATA_LOCKED`, three slot item port, production view, update tag with seed and cycle. `CoagulatorMenu`: `SeedSlot`, no recipe buttons. `CoagulatorBlock`: seed by hand, sneak to take it. Verify: `gradlew build` green.
- [x] 3.2 `gametest/GameTestRecipes` with the `ModifyRecipeJsonsEvent` listener, registered from `ServusAlchemy` when `servus.gametest.recipes` is set; root `build.gradle` sets it for `gameTestServer` and `gameTestServerAll`. Verify: `gradlew build` green.
- [x] 3.3 GameTests in `CoagulatorGameTests` (`kandisSeedGrowsSaltCrystals`, `noSeedDoesNothing`, `lockedSeedGrowsNothing`, `duplicateSeedStaysWithTheFirstRecipe`, `pipeSetsTheSeedButNeverTakesIt`, `seedByHand`, `takingTheSeedOutStopsTheCoagulator`) and the existing ones in `CoagulatorGameTests` and `AlchemyGameTests` moved to seeds. Verify: `gradlew :alchemy:runGameTestServer` green.

## 4. Client

- [x] 4.1 GUI texture with the seed slot frame; `CoagulatorScreen` without popup, seed tooltip; lang keys for the seed texts, the old popup keys removed. Verify: `gradlew build` green.
- [x] 4.2 `CoagulatorRenderer` draws the growing seed; the middle Vat spout in `AlchemyModels.vatModel` ends at y -2. List every overlapping pair (seed and mold top, seed and spout, spout and Vat floor, spout and mold) and confirm no shared planes. Verify: `gradlew :alchemy:runData` and `gradlew build` green.
- [x] 4.3 `CoagulationRecipeCategory` with the seed slot and label. Verify: `gradlew build` green.

## 5. Book and integration

- [x] 5.1 `WorkshopCategory.coagulator()`: Joining text for seeds and the "Seeds" list from `SeedText`. Verify: `gradlew :alchemy:runData` lists the three transmutations, `gradlew build` green.
- [x] 5.2 Full `gradlew build`, unit tests and the alchemy GameTests green; `gradlew :alchemy:runData` output committed.
- [x] 5.3 Play check in game by the user from main:
  - The Coagulator GUI shows the seed slot under the arrow, no select button; the seed tooltip names what it grows and its needs.
  - Kandis, Brimstone and Cinnabar turn into Salt Crystal, Sulfur and Quicksilver, one per cycle; each product seeds itself and comes back doubled; the Coagulator stops when the seeds run out.
  - The seed lies in the mold under the shorter spout and grows smoothly during a cycle; no flicker from any angle.
  - A pipe fills the seed slot with a stack and adds more of the same seed, but refuses another; a hopper never takes the seeds; sneaking with an empty hand takes them all out and stops the work.
  - JEI shows the seeds; the book explains seeds and lists the three transmutations.

  Verify: the user confirms in game.

## 6. Play check fixes

- [x] 6.1 Every seed is used up, one per cycle: a transmutation seed turns into the result, a self seed comes back doubled. `CoagulationRecipe.selfSeed`/`resultFrom`; pure `alchemy/.../coagulator/SeedSlot` with JUnit `SeedSlotTest`; `CoagulatorBlockEntity` (seed stack of one kind, `seedRoom`, `plantSeed`, `finishWork`, production view with the seed as input), `CoagulatorBlock`, `CoagulatorMenu`, `CoagulatorScreen` and `CoagulationRecipeCategory` tooltips, lang and the book's Joining and First Seeds pages. GameTests in `CoagulatorGameTests`: `kandisSeedsAreUsedUp`, `redstoneSeedsDouble`, `productionListsTheSeed`, `pipeAddsSeedsButNeverTakesThem` (was `pipeSetsTheSeedButNeverTakesIt`), `pipeLaysAStackOfSelfSeeds`, `seedByHand`, `itemsAtTheCastingTable`; `kandisSeedGrowsSaltCrystals` replaced by `kandisSeedsAreUsedUp`. Verify: `gradlew :alchemy:runData`, `gradlew build`, `gradlew test` and `gradlew :alchemy:runGameTestServer` green.

## Workflow follow-up

- At archive: add under `## Unreleased` in `alchemy/CHANGELOG.md`: Changed: the Coagulator grows what its seeds are and uses one per cycle, a crystal of the product comes back doubled and Kandis, Brimstone or Cinnabar turn into the first Salt Crystal, Sulfur and Quicksilver; the recipe popup is gone.
- At archive: PLAN.md has no open point for this change; nothing to remove.
