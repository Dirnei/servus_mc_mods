## Why

The Delicacies kitchen bakes and fries, but the Bavarian sides are missing (PLAN.md, Open: M14 Delicacies, `m14-sides`): Sauerkraut, Red Cabbage, Bread Dumpling, Potato Dumpling and potato salad. The plated meals of `m14-teller` (Roast Pork with two dumplings and red cabbage, Schnitzel with potato salad) need them, and the Cordon Bleu of `m14-meat-dishes` needs Cheese (`c:foods/cheese`). Sauerkraut and Cheese take days, which needs a new kitchen block that keeps time without a fire: the Fermenting Crock.

## What Changes

- **Fermenting Crock**: a brown stoneware crock with a water seal rim and a lid, placed anywhere (not on the stove). It holds four ingredient stacks, one result stack and a 4000 mB tank. When its contents match a data driven `servus_delicacies:fermenting` recipe it closes its lid and ferments for the recipe's time in game ticks (examples: Sauerkraut three in-game days, Cheese two). The time is measured on the level's game time, so it keeps counting while the chunk is unloaded; the crock checks itself with scheduled block ticks, never per tick. When the time is up it turns every full batch of its contents into the result and opens. While sealed nothing goes in or out except the result; bubbles rise at the water seal. Open, it shows its contents, its fluid and its result. An empty hand on a sealed crock tells what it makes and how many in-game hours are left. Item and fluid capabilities for pipes and Shuttles.
- **Milk as a fluid**: Delicacies turns on NeoForge's milk fluid, so a Milk Bucket empties into the crock and the Pot, and recipes ask for the fluid tag `c:milk`.
- **Sauerkraut**: Shredded Cabbage and Salt (`c:dusts/salt`) fermented into Sauerkraut; Sauerkraut and a Bowl craft into a Bowl of Sauerkraut, which is eaten and gives the Bowl back.
- **Cheese**: Milk and Salt fermented into Cheese wedges, in `c:foods/cheese` and `c:foods`; edible.
- **Red Cabbage**: Red Cabbage cut into Shredded Red Cabbage on the Cutting Board, braised with an Apple, Sugar and water in the Pot into Braised Red Cabbage; with a Bowl it crafts into a Bowl of Red Cabbage.
- **Bread Dumpling**: Bread Crumbs, an Egg, Chopped Onion and Parsley simmered in milk in the Pot.
- **Potato Dumpling**: Potatoes grated on the Cutting Board into Grated Potato, simmered in the Pot with Boiled Potatoes, Salt and water.
- **Potato Salad**: Boiled Potatoes, Chopped Onion and a Bowl crafted into a Potato Salad (a bowl food).
- Food values in the `servus_delicacies:food` data map (moderate: sides, not meals). The edible sides are in `c:foods`, the bowl foods also in `c:foods/soup`, Cheese in `c:foods/cheese`.
- JEI: a Fermenting category (items, fluid, result, time in days) with the crock as catalyst; the new pot and cutting recipes appear in the existing categories.
- Textures in `tools/delicacies-textures/gen.py`: the mockup's `sauerkraut`, `blaukraut`, `semmelknoedel`, `kartoffelknoedel` and the `bowl` helper, new sprites for Sauerkraut and Braised Red Cabbage in bulk, Cheese, Shredded Red Cabbage, Grated Potato and Potato Salad, and the crock's stoneware, seal and lid textures.

## Capabilities

### New Capabilities
- `delicacies/fermenting-crock`: the Fermenting Crock, the `servus_delicacies:fermenting` recipe type, game time fermentation, its look, hand use and automation.
- `delicacies/sides`: Sauerkraut, Cheese, Red Cabbage, Bread Dumpling, Potato Dumpling and Potato Salad, their recipes, bowls, tags and food values, and milk as a fluid.

### Modified Capabilities
None. The Pot, Pan, oven and Cutting Board keep their behavior; they only get new shipped recipes.

## Non-goals

- Plated meals and meal buffs (`m14-teller`); Cordon Bleu (`m14-meat-dishes`).
- Vinegar, brine or whey as items or fluids; more than one cheese.
- Breaking a crock's seal early (breaking the crock drops its contents).
- Fermenting beer (`m15-beer` has its own vat).

## Impact

- New Delicacies code: package `crock` (`Fermentation`, `CrockBatch`, `FermentingRecipe`, `FermentingCrockBlock`, `FermentingCrockBlockEntity`, `CrockParts`), `cooking/TankVesselBlockEntity`, `client/CrockRenderer`, `integration/jei/FermentingRecipeCategory`, `gametest/SidesGameTests`, JUnit `FermentationTest` and `CrockBatchTest`.
- Changed: `PotBlockEntity` (its tank moves into `TankVesselBlockEntity`), `VesselBlockEntity` (`canHoldInput` overridable), `VesselRecipeCategory` (time text and note overridable), `ServusDelicacies` (milk fluid), block, block entity, item and recipe registries, `ServusDelicaciesEvents`, `ServusDelicaciesClient`, datagen providers, lang, JEI plugin, `DelicaciesGameTests`, `tools/delicacies-textures/gen.py`.
- No Foundation, Alchemy, Shuttles or Brewery changes.
