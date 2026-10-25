# Tasks

## 1. Recipes

- [x] 1.1 New datagen helper `foundation/.../datagen/MillingRecipes.java` with the recipe table from the design (stone and deepslate to cobble, cobble to gravel, sandstone to sand, sugar, glowstone dust, bone meal, blaze powder, string, clay balls, quartz, amethyst shards, the 24 flowers at twice the vanilla dye). Tags are resolved through the item registry lookup, the way `ServusDelicaciesRecipeProvider` does for `milling/flour`. `MachineDataParts.recipes` calls it in place of the inline `milling/sand`. Run `gradlew :foundation:runData` and check `foundation/src/generated/resources/data/servus_foundation/recipe/milling/` holds `sand.json` unchanged plus one file per new recipe. Verify: `gradlew :foundation:build` green.
- [x] 1.2 GameTests in `foundation/.../machine/mill/MillGameTests.java`: `millRefusesOtherItems` uses Dirt instead of Cobblestone. New `millingVanillaRecipesShipped` checks Stone to 1 Cobblestone, Cobblestone to 1 Gravel, Glowstone to 4 Glowstone Dust, Red Wool to 4 String, Poppy to 2 Red Dye, Rose Bush to 4 Red Dye. New `millingRefusesOresAndGems` checks that Iron Ore and Diamond have no recipe. New `millingInputsAreDisjoint` checks that no item is accepted by two `servus_foundation` milling recipes. Verify: `gradlew :foundation:runGameTestServer` green.

## 2. Docs and changelog

- [x] 2.1 Docs: the built-in milling table in `docs/src/content/docs/reference/datapack/recipes.md` lists every new recipe with input, result and work (the flowers as one row with the rule and the five tall ones named). The Mill line in `docs/src/content/docs/mods/foundation.md` says it grinds stone down to sand and turns blocks such as glowstone, quartz and wool back into their pieces. Refresh `docs/src/data/gamedata.json` with `gradlew :alchemy:runData`. Verify: no em dash in the diff, docs tests pass (`npm test` in `docs/`).
- [x] 2.2 `PLAN.md`: add this change to the M14 list while it is open. Verify: `openspec validate m14-mill-vanilla-recipes` passes.

## 3. Verification

- [x] 3.1 Full `gradlew build test` and `:foundation:runGameTestServer` green. The game launches with all mods.
- [x] 3.2 User play check in game: a cranked Mill turns Stone into Cobblestone, then Gravel, then Sand. Glowstone, Quartz, Amethyst and Clay blocks give 4 pieces each, any Wool gives String, Sugar Cane gives 2 Sugar, a Rose Bush gives 4 Red Dye. Iron Ore and Diamonds are refused. JEI's Milling category lists all the new recipes.

## Workflow follow-up

- On archive: groom `PLAN.md` (remove this item from M14, add the open point "ore and gem milling, decide on ore doubling" under Foundation). Add an "Added" line to `foundation/CHANGELOG.md` under Unreleased.
