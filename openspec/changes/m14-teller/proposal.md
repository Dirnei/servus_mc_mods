## Why

The Delicacies kitchen now makes the dishes and the sides, but nothing brings them together into a meal (PLAN.md, Open: M14 Delicacies, `m14-teller`: "plated meals with portions and a meal buff"). A Bavarian kitchen ends at the table: Roast Pork with two dumplings and red cabbage, Schnitzel with potato salad, and the Weisswurst breakfast. A placed meal that is eaten portion by portion, and rewards the finished plate with a short buff, makes cooking the full chain worth it.

## What Changes

- **Plate**: a white ceramic plate with a blue rim, crafted from Clay Balls and a blue dye, placed on any solid top. It is a flat block (a 1 px plate on the table, food boxes up to a few pixels above it); an empty plate drops itself.
- **Plated meals**, each a placed block on the plate that faces the player and shows its food as small boxes (mockup section "Teller"):
  - **Roast Pork Plate**: Roast Pork with crackling on a pool of gravy, two dumplings and red cabbage.
  - **Schnitzel Plate**: two Schnitzel pieces and two heaps of potato salad.
  - **Weisswurst Breakfast**: two Weisswurst, two Pretzel pieces and a dollop of Sweet Mustard.
  - **Weisswurst Breakfast with Weissbier**: the same with a Weissbier glass on the plate, only craftable when Servus: Brewery is installed.
- **Assembly by crafting** (shapeless, so pipes and autocrafting can make meals): Plate, Roast Pork, two dumplings (`servus_delicacies:dumplings`: Bread or Potato Dumpling), Braised Red Cabbage and Gravy; Plate, Schnitzel and Potato Salad; Plate, two Weisswurst, a Pretzel and Sweet Mustard. Gravy and Potato Salad give their Bowl back in the grid. The Weissbier version adds one item of `servus_delicacies:breakfast_beers` (optionally `servus_brewery:weissbier`) and is loaded only with the `neoforge:mod_loaded` condition for `servus_brewery`. No compile dependency on Brewery.
- **Eating**: a meal holds four portions. Each empty hand right click by a player who can eat eats one portion: hunger and saturation from the new data map `servus_delicacies:meal`, the eat sound, one food box disappears. The last portion leaves the empty Plate block in place and gives the meal's finish effects from the same data map: **Properly Full**, a new mild healing effect (example: 45 to 60 seconds, half a heart every 5 seconds). The Weissbier breakfast hands the player the beer from its tag after the last portion.
- **Breaking**: a full meal drops itself; a started meal drops only the Plate.
- Textures in `tools/delicacies-textures/gen.py`: the mockup's `plate` and `ceramic`, the food box textures (gravy, crackling, roast, dumpling, red cabbage, schnitzel, potato salad, weisswurst, pretzel, mustard, weissbier) and the Properly Full icon.

## Capabilities

### New Capabilities
- `delicacies/teller`: the Plate, the plated meals, their crafting, portions, eating, finish effects, drops and look, the `servus_delicacies:meal` data map and the Properly Full effect.

### Modified Capabilities
None. Gravy and the bowl sides gain a crafting remainder (the Bowl), which only matters in crafting recipes that use them; the ones that exist now are the new meal recipes.

## Non-goals

- Assembling a meal on a placed plate by hand; more meals than these four.
- Rausch or any drink effect on the plate: the Weissbier is handed over and drunk through Brewery.
- A table or Biergarten furniture (later open point).
- Plates stacking or a dish rack.

## Impact

- New Delicacies code: package `teller` (`MealPortions`, `MealKind`, `MealPart`, `MealParts`, `PlateBlock`, `MealBlock`, `Meal`, `ProperlyFullEffect`, `TellerTags`), `registry/ServusDelicaciesEffects`, `datagen/TellerModels`, `gametest/TellerGameTests`, JUnit `MealPortionsTest` and `MealPartsTest`.
- Changed (appended blocks): `ServusDelicacies` (effect register), block and item registries (Gravy and bowl foods with a crafting remainder), `DelicaciesDataMaps` (meal map), datagen providers (block states, loot, item tags, recipes, data maps, lang), `DelicaciesGameTests`, `tools/delicacies-textures/gen.py`.
- No Foundation, Alchemy, Shuttles or Brewery changes. Does not touch `stove/` or `firebox/`.
