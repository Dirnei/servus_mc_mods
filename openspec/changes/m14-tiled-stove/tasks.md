# Tasks

## 1. Pure logic

- [x] 1.1 `firebox/Firebox`, `firebox/HeatSource`, `firebox/FuelSupply`, `firebox/FuelFire` (D1); JUnit `FuelFireTest` (cold without demand and keeps its fuel, lights on demand and is hot at once, burns the full burn time, burns out when the demand stops, relights from the next item while demanded, goes cold without fuel, heat 1 while hot, restore from saved ticks). Verify: `gradlew :delicacies:test` and `gradlew build` green.
- [x] 1.2 `stove/StoveRow` (D2); JUnit `StoveRowTest` (single tile too short, two tiles from either end, middle click, gap ends the row, exactly max, max plus one too long). Verify: `gradlew :delicacies:test` green.
- [x] 1.3 `cooking/SlotMatcher` (D4); JUnit `SlotMatcherTest` (one requirement one slot, two requirements need distinct slots, backtracking finds the other assignment, leftover non empty slot fails, empty slots are ignored, too few slots fail). Verify: `gradlew :delicacies:test` green.

## 2. Stove

- [x] 2.1 `kitchen/KitchenStats`, `kitchen/KitchenStatsManager` with `data/servus_delicacies/kitchen/stats.json`; `kitchen/KitchenSlots`; `firebox/DelicaciesCapabilities`; `stove/StoveTileBlock`, `stove/StoveTileBlockEntity` (forming, unforming, fuel, fire, lit states, status message, flame particles) (D2, D3); `registry/ServusDelicaciesBlockEntities`; block and item registration; capability and reload listener registration in `ServusDelicacies`. Verify: `gradlew build` green.

## 3. Cooking

- [x] 3.1 `cooking/VesselRecipe`, `PotRecipe`, `PanRecipe`, `VesselInput`; `board/CuttingRecipe`, `CuttingInput`; `registry/ServusDelicaciesRecipes` (types, serializers, client sync) (D4, D5). Verify: `gradlew build` green.
- [x] 3.2 `cooking/VesselParts`, `VesselBlock`, `PotBlock`, `PanBlock`, `VesselBlockEntity`, `PotBlockEntity`, `PanBlockEntity` (hand use, capabilities, tick, sync, steam) (D4); registration. Verify: `gradlew build` green.
- [x] 3.3 `board/CuttingBoardBlock`, `CuttingBoardBlockEntity`, Kitchen Knife item (D5); registration. Verify: `gradlew build` green.
- [x] 3.4 `food/DelicaciesDataMaps`, `food/DelicaciesFoodItem`; food and ingredient items, tab order (D6). Verify: `gradlew build` green.

## 4. Look

- [x] 4.1 `tools/delicacies-textures/gen.py`: port `tex_noise`, `kachel`, `kachel_door` (cold, lit, flicker frame), `herd_top` (cold, lit); new food, ingredient and knife sprites; flicker `.mcmeta` (D8). Verify: the script runs clean, ported sprites match the mockup, look at an 8x preview.
- [x] 4.2 Datagen: stove, vessel and board models and block states, item models, lang, loot (`dropSelf`), block and item tags, recipes, the food data map (D7, D9); overlap check from the design table against the final boxes. Verify: `:delicacies:runData`, generated files committed, `gradlew build` green.
- [x] 4.3 Client: `client/ServusDelicaciesClient` with `VesselRenderer` and `CuttingBoardRenderer` (D4, D5). Verify: `gradlew build` green.

## 5. JEI

- [x] 5.1 JEI API `compileOnly` in `delicacies/build.gradle`; `integration/jei/ServusDelicaciesJeiPlugin`, `PotRecipeCategory`, `PanRecipeCategory`, `CuttingRecipeCategory`, lang (D10). Verify: `gradlew build` green.

## 6. GameTests

- [x] 6.1 `gametest/StoveGameTests`, `CookingGameTests`, `CuttingBoardGameTests` (D11), registered in `DelicaciesGameTests`. Verify: `:delicacies:runGameTestServer` and `gradlew build test` green.

## 7. Play check

- [ ] 7.1 Play check by the user:
  - Craft Stove Tiles, place two, three and five in a row; the carrot forms two and three (door on the clicked face, or toward you when clicking the top), refuses one and five; breaking a tile unforms the stove and drops the fuel; sneak carrot dismantles a tile.
  - Coal by hand and through a hopper; the fire lights only when a vessel has something to cook; the door glows and flickers, the plates look lit, flames and crackle at the door; it goes out when the fuel ends.
  - Pot: water bucket in, Potatoes in, Boiled Potatoes after about ten seconds, steam while cooking, the water surface sinks; empty hand takes the result. Pan: Egg to Fried Egg, Chopped Onion to Fried Onions, Boiled Potatoes and Chopped Onion to Fried Potatoes. Both cook on every tile of the stove, never without fire.
  - Cutting Board: Cabbage and Onion cut with the knife, one per click, the knife wears; hopper feeds the board.
  - Eat every food; JEI shows the pot, pan and cutting categories.
  - Look at stove, Pot, Pan and Cutting Board from several angles and while items and fluid are inside: no flicker; tile, door, plate, steel and iron textures read right on every face.
