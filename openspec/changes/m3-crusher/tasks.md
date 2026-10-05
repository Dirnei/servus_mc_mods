# Tasks

## 1. Recipes and items

- [x] 1.1 Add `CrushingRecipe`, `CrushingRecipeSerializer`, registry entries, `CrushingRecipeBuilder`, grit items with models and textures, smelting and blasting recipes, the four crushing recipes. Verify `gradlew build` and `gradlew :manufacturing:runData` pass and write `recipe/crushing/raw_iron.json`.

## 2. Crusher

- [x] 2.1 Add `CrusherBlock`, `CrusherBlockEntity`, `CrusherMenu`, layout and GUI texture, registry entries, capabilities, screen, stats JSON, blockstate, loot, tag, lang, the gated assembling recipe, tech unlock id, and the JEI crushing category and catalyst. Verify `gradlew build` and `gradlew :manufacturing:runData` pass.

## 3. Verification

- [x] 3.1 Server check over RCON: crusher with 4 raw iron beside a generator yields 8 iron grit; a smelter with grit yields ingots. `gradlew :manufacturing:runGameTestServer` passes, `gradlew clean build` green, `gradlew runClientAll` reaches the title screen with JEI loaded. Commit.
