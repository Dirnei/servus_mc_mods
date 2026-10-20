# Tasks

## 1. Pure logic

- [x] 1.1 `SlotMatcherTest`: add `noRequirementsMatchOnlyEmptySlots` (no requirements match all empty slots, fail on any filled slot) (D3). Verify: `gradlew :delicacies:test` green.

## 2. Oven and recipes

- [x] 2.1 `cooking/BakingRecipe`; baking type, serializer and sync in `registry/ServusDelicaciesRecipes` (D1). Verify: `gradlew :delicacies:compileJava` green.
- [x] 2.2 `cooking/VesselParts` oven parts, `cooking/BakingOvenBlock`, `cooking/BakingOvenBlockEntity`; block (light while baking), block entity and item registration; oven item capability in `ServusDelicaciesEvents` (D1, D6). Verify: `gradlew build` green.
- [x] 2.3 Items: Flour, Dough, Salt, Wood Ash, Lye, Bread Crumbs, Bread Roll; Pretzel becomes a `DelicaciesFoodItem` in `FOODS`; tab order; `kitchen/KitchenTags` for flour, dough, salt and ash (D2 to D5). Verify: `gradlew build` green.

## 3. Look

- [x] 3.1 `tools/delicacies-textures/gen.py`: port `semmel` and `mehl`; new Dough, Salt, Wood Ash, Lye, Bread Crumbs sprites; oven door cold and lit; preview for every sprite (D7). Verify: the script runs clean, look at the 8x previews.
- [x] 3.2 Datagen: oven model and block state (facing, lit), item models, lang, loot (`dropSelf`), mineable tag, item tags, recipes (milling, dough, campfire and furnace ash, pot salt and lye, baking, bread crumbs, oven crafting), food data map (D2 to D6); overlap check from the D6 table against the final boxes. Verify: `:delicacies:runData`, generated files committed, `gradlew build` green.

## 4. JEI

- [x] 4.1 Baking category, recipes and catalysts in `integration/jei/ServusDelicaciesJeiPlugin`, lang (D8). Verify: `gradlew build` green.

## 5. GameTests

- [x] 5.1 `gametest/BakeryGameTests` (D9), registered in `DelicaciesGameTests`. Verify: `:delicacies:runGameTestServer` and `gradlew build test` green.

## 6. Play check

- [ ] 6.1 Play check by the user:
  - Mill Wheat into Flour with the Hand Crank and the Electric Motor; craft Dough from three Flour and a Water Bucket, the bucket stays.
  - Boil a Pot of water alone into Salt; burn a log on a Campfire and smelt Charcoal into Wood Ash; Wood Ash and water in the Pot give Lye.
  - Craft the Baking Oven, place it on a Tiled Stove: Dough bakes into a Bread Roll; two Dough, Lye and Salt bake into two Pretzels; nothing bakes without fire or off the stove; the window glows and the oven gives light only while it bakes; hopper in and out.
  - Cut a Bread Roll into Bread Crumbs; eat a Bread Roll and a Pretzel.
  - JEI shows the Baking category, salt and lye in the Pot category and flour in the milling category.
  - Look at the oven from several angles, cold and baking: no flicker; steel on every body face, door and window read right, brass handle.
