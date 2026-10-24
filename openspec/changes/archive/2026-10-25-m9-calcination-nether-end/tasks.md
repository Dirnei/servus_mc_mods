# Tasks

## 1. Ranking

- [x] 1.1 Add pure `alchemy/.../alchemy/calciner/RecipeSpecificity.java` (record `Rank(boolean tag, int items, String id)`, comparator, `best`) with JUnit `RecipeSpecificityTest`: item beats tag, a subset tag beats its superset, equal counts fall to the id, every order of the input gives the same winner, an empty list gives nothing. Verify: `gradlew :alchemy:test` green.

## 2. Recipe data

- [x] 2.1 Add pure `alchemy/.../alchemy/calciner/CalcinationDimension.java` (enum with ids, `byId`) with a JUnit test for the ids and an unknown id. Add the optional `dimension` field to `CalcinationRecipe` (codec with default `overworld`, stream codec as ordinal). Verify: `gradlew build` green and the existing generated calcination JSON unchanged by `runData`.
- [x] 2.2 Give `AlchemyRecipeTable.Calcine` its dimension and add the Nether and End rows from design.md; `ServusAlchemyRecipeProvider` writes the dimension. Verify: `gradlew :alchemy:runData` writes the 15 new recipe files with their `dimension`, no Dragon's Breath recipe, and `gradlew build` green.

## 3. Lookup

- [x] 3.1 Rename the private `ServusRecipes.recipes(Level)` in `foundation/.../production/ServusRecipes.java` to a public `recipeMap(Level)`. Add `alchemy/.../alchemy/calciner/CalcinationRecipes.java` (`forItem`, `burns`, rank cache per `RecipeMap`). Use it in `CalcinerBlockEntity.currentRecipe()`. Verify: `gradlew build` green.
- [x] 3.2 JEI: `CalcinationRecipeCategory` shows `CalcinationRecipes.burns(...)`; `ServusAlchemyJeiPlugin` skips calcination recipes that burn nothing. Verify: `gradlew build` green.
- [x] 3.3 GameTests in `alchemy/.../gametest/CalcinerGameTests.java`: `netherWartBurnsWithItsOwnRecipe`, `crimsonStemBurnsAsAStem`, `dragonsBreathIsNotCalcinable`, `cropsRecipeDoesNotBurnNetherWart`. Verify: `gradlew :alchemy:runGameTestServer` green.

## 4. Integration

- [x] 4.1 Full `gradlew build`, unit tests and the alchemy GameTests green.
- [x] 4.2 Play check in game by the user from main:
  - Nether Wart, a Nether Wart Block, a Crimson Stem, a Blaze Rod and an Ender Pearl each burn in a Calciner with the amounts of their recipe.
  - JEI: the uses of Nether Wart show only the Nether Wart recipe; the Crops recipe never cycles to Nether Wart.
  - Dragon's Breath does not go into the Calciner.

  Verify: the user confirms in game.

## 5. Play check fixes

- [x] 5.1 The Nether Wart Block burns too: `AlchemyRecipeTable.calcination()` row `nether_wart_block` (0, 540, 180) after `nether_wart`, journal hint in `ServusAlchemyLanguageProvider`, GameTest `netherWartBlockBurnsIntoNineWarts` in `CalcinerGameTests`. Verify: `gradlew :alchemy:runData`, `gradlew build` and `gradlew :alchemy:runGameTestServer` green.

## Workflow follow-up

- At archive: add a line under `## Unreleased` in `alchemy/CHANGELOG.md` (Added: Nether and End matter burns in the Calciner; Fixed: overlapping calcination recipes pick the most specific one).
