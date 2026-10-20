# Tasks

## 1. Pure logic

- [x] 1.1 `crock/Fermentation`, `crock/CrockBatch` (D1); JUnit `FermentationTest` and `CrockBatchTest` with the cases of D10. Verify: `gradlew :delicacies:test` green.

## 2. Crock and recipes

- [x] 2.1 `crock/FermentingRecipe`; fermenting type, serializer and sync in `registry/ServusDelicaciesRecipes` (D2). Verify: `gradlew :delicacies:compileJava` green.
- [x] 2.2 `cooking/TankVesselBlockEntity` with `PotBlockEntity` on it; `VesselBlockEntity#canHoldInput` protected (D3). Verify: `gradlew :delicacies:build` green (existing Pot GameTests later in 5.1).
- [x] 2.3 `crock/CrockParts`, `crock/FermentingCrockBlock`, `crock/FermentingCrockBlockEntity` (check, seal, batches, status, bubbles); block, block entity and item registration; item and fluid capabilities in `ServusDelicaciesEvents`; milk fluid in `ServusDelicacies` (D3, D4, D5). Verify: `gradlew :delicacies:build` green.
- [x] 2.4 Items: Shredded Red Cabbage, Grated Potato, Sauerkraut, Braised Red Cabbage, Cheese, Bowl of Sauerkraut, Bowl of Red Cabbage, Potato Salad, Bread Dumpling, Potato Dumpling; tab order; `KitchenTags.CHEESE` (D6). Verify: `gradlew :delicacies:build` green.

## 3. Look

- [x] 3.1 `tools/delicacies-textures/gen.py`: appended block with the mockup's `bowl`, `sauerkraut`, `blaukraut`, `semmelknoedel`, `kartoffelknoedel`, the new item sprites and the crock textures (D8). Verify: the script runs clean, look at the 8x previews.
- [x] 3.2 Datagen: crock models (open, sealed) and block state, item models, lang, loot, mineable tag, item tags, recipes (cutting, fermenting, pot, crafting, crock), food data map (D6, D7); overlap check from the D7 table against the final boxes. Verify: `:delicacies:runData`, generated files committed, `gradlew :delicacies:build` green.
- [x] 3.3 Client: `client/CrockRenderer` (open crock: fluid, items, result), registered in `ServusDelicaciesClient` (D7). Verify: `gradlew :delicacies:build` green.

## 4. JEI

- [x] 4.1 `VesselRecipeCategory` hooks, `integration/jei/FermentingRecipeCategory`, plugin registration, catalyst, lang (D9). Verify: `gradlew :delicacies:build` green.

## 5. GameTests

- [x] 5.1 `gametest/SidesGameTests` (D10), registered in `DelicaciesGameTests`. Verify: `:delicacies:runGameTestServer` and `gradlew :delicacies:build` green.

## 6. Play check

- [ ] 6.1 Play check by the user:
  - Craft the Fermenting Crock from Bricks and place it; it needs no stove.
  - Cut Cabbage into Shredded Cabbage, put four in and one Salt: the lid goes on, bubbles rise at the water seal, an empty hand tells "Fermenting into Sauerkraut" and the hours left; Salt from a hopper stays out while sealed. After three in-game days (game time; `/time set` and sleeping do not shorten it) the lid is off and four Sauerkraut show inside; leave the area for a while and come back: the time counted.
  - A Milk Bucket into an open crock, then Salt: Cheese after two in-game days; the Bucket comes back.
  - Pot on a lit stove: Braised Red Cabbage (Shredded Red Cabbage, Apple, Sugar, water), Bread Dumplings (Bread Crumbs, Egg, Chopped Onion, Parsley, milk from a Milk Bucket), Potato Dumplings (Grated Potato cut from a Potato, Boiled Potatoes, Salt, water).
  - Craft Bowl of Sauerkraut, Bowl of Red Cabbage and Potato Salad; eat them (the Bowl comes back), the dumplings and the Cheese.
  - JEI shows the Fermenting category with the time in days, and the new pot and cutting recipes.
  - Look at the crock open, filled with items and milk, sealed and done, from several angles: no flicker on lid, skirt, lip, neck, fluid and items; stoneware reads brown on every face, water shows in the seal.
