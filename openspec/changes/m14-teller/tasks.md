# Tasks

## 1. Pure logic

- [x] 1.1 `teller/MealPortions`, `teller/MealPart`, `teller/MealKind`, `teller/MealParts` (D2, D7); JUnit `MealPortionsTest` (last portion, after eating, out of range throws) and `MealPartsTest` (shown parts per count, base kept, parts inside the block and on the plate, no shared planes where parts overlap). Verify: `gradlew :delicacies:test` green.

## 2. Blocks, items and data

- [x] 2.1 `teller/Meal` and `DelicaciesDataMaps.MEAL`; `teller/ProperlyFullEffect` and `registry/ServusDelicaciesEffects`, registered in `ServusDelicacies` (D4, D5). Verify: `gradlew :delicacies:compileJava` green.
- [x] 2.2 `teller/PlateBlock`, `teller/MealBlock` (portions, eating, finishing, beer hand over); block and item registration, creative tab order, crafting remainder for Gravy and the bowl foods, `teller/TellerTags` for dumplings and breakfast beers (D1, D3, D5). Verify: `gradlew :delicacies:build` green.

## 3. Look

- [x] 3.1 `tools/delicacies-textures/gen.py`: appended block with the plate, ceramic, food box textures, the Weissbier glass and the Properly Full icon (D8). Verify: the script runs clean, look at the 8x previews.
- [x] 3.2 Datagen: `datagen/TellerModels` (plate and meal models per portion count, block states, item models), loot (D6), item tags, recipes (D1, with the Brewery condition), meal data map (D4), lang; overlap check from the D7 table against the final boxes. Verify: `:delicacies:runData`, generated files committed, `gradlew :delicacies:build` green.

## 4. GameTests

- [x] 4.1 `gametest/TellerGameTests` (D9), registered in `DelicaciesGameTests`. Verify: `:delicacies:runGameTestServer` and `gradlew :delicacies:build` green.

## 5. Play check

- [ ] 5.1 Play check by the user:
  - Craft Plates from Clay Balls and a blue dye; place one on a table: white with a blue rim, flat; it drops itself.
  - Craft a Roast Pork Plate (Roast Pork, two dumplings, Braised Red Cabbage, Gravy; the Bowl stays in the grid), a Schnitzel Plate (Schnitzel, Potato Salad) and a Weisswurst Breakfast (two Weisswurst, Pretzel, Sweet Mustard); place them, they face you.
  - Hungry, right click each with an empty hand four times: one food box goes per click, the gravy and the mustard stay until the end, hunger rises; after the last an empty Plate stays and Properly Full shows with its icon and heals slowly. Full, a click does nothing.
  - Break a full meal (it drops itself) and a started one (only the Plate).
  - With Servus: Brewery installed: JEI shows the Weisswurst Breakfast with Weissbier; craft it with a Weissbier, the glass stands on the plate, and the Weissbier comes back after the last portion. Without Brewery the recipe is absent and the log is clean.
  - Look at every meal from several angles at every portion count: no flicker between plate, gravy, roast, dumplings, cabbage, schnitzel, salad, sausages, pretzel, mustard and glass.
