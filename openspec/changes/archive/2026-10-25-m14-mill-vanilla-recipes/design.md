# Design

## Context

The Mill, its `servus_foundation:milling` recipe type and the JEI category exist since `m14-mill`. A recipe is one sized ingredient, one `ItemStackTemplate` result and an amount of work. The Mill takes the first recipe that matches its input (`ServusRecipes.first`), so two recipes for the same item would make one of them unreachable. Foundation's only milling recipe, `milling/sand`, is written in `MachineDataParts.recipes` through `RecipeOutput.accept`. Delicacies ships `milling/flour` (`#c:crops/wheat`) and `milling/mustard_flour`. Brewery ships three malt grists. None of them overlap with the items in this change.

Work today: a Hand Crank gives 25 per turn (about 0.8 s), an Electric Motor 5 per tick. Gravel into Sand takes 200.

## Goals / Non-Goals

**Goals:**
- Every recipe from the proposal is datagen output, with the counts and work as data in the generated JSON.
- One readable table in datagen, so changing or adding a recipe is one line.

**Non-Goals:**
- Any change to `MillingRecipe`, `MillBlockEntity` or the JEI category.

## Decisions

**A small datagen helper `MillingRecipes` beside `MaterialRecipes`.** `MachineDataParts.recipes` calls it in place of the inline sand recipe. It has a `mill(output, name, ingredient, count, result, resultCount, work)` method and the recipe table. The ids are `servus_foundation:milling/<name>`, and `milling/sand` keeps its id. Putting a dozen more `output.accept` lines into `MachineDataParts` would bury the crafting recipes there.

**Ingredients use `c` tags where NeoForge defines a tight one, otherwise the item.** I checked the tags in `NeoForgeItemTagsProvider` (NeoForge 26.1.2.116 sources):

| Recipe | Ingredient | Result | Work (example) |
|---|---|---|---|
| `sand` | Gravel | 1 Sand | 200 (unchanged) |
| `cobblestone` | Stone | 1 Cobblestone | 100 |
| `cobbled_deepslate` | Deepslate | 1 Cobbled Deepslate | 100 |
| `gravel` | `#c:cobblestones/normal` | 1 Gravel | 200 |
| `gravel_from_deepslate` | `#c:cobblestones/deepslate` | 1 Gravel | 200 |
| `sand_from_sandstone` | `#c:sandstone/uncolored_blocks` | 4 Sand | 200 |
| `red_sand` | `#c:sandstone/red_blocks` | 4 Red Sand | 200 |
| `sugar` | `#c:crops/sugar_cane` | 2 Sugar | 100 |
| `glowstone_dust` | Glowstone | 4 Glowstone Dust | 100 |
| `bone_meal` | `#c:bones` | 4 Bone Meal | 100 |
| `blaze_powder` | `#c:rods/blaze` | 3 Blaze Powder | 200 |
| `string` | `#minecraft:wool` | 4 String | 100 |
| `clay_ball` | Clay | 4 Clay Ball | 100 |
| `quartz` | Block of Quartz | 4 Nether Quartz | 200 |
| `amethyst_shard` | Block of Amethyst | 4 Amethyst Shard | 200 |
| `<color>_dye_from_<flower>` | each flower | 2x vanilla dye count | 100 |

- Stone and Deepslate stay plain items. `#c:stones` also holds andesite, diorite, granite and tuff, and those should not turn into cobblestone.
- The uncolored and red sandstone block tags also hold the cut, chiseled and smooth forms. They all give 4 sand, because each of them is crafted from one sandstone's worth of sand.
- Bones, blaze rods and sugar cane use their `c` tags, the same way Delicacies mills `#c:crops/wheat`, so modded variants work too.

**The flower list is an explicit table taken from vanilla's `oneToOneConversionRecipe` calls in `VanillaRecipeProvider` (26.1.2), not read at datagen time.** There are 24 flowers. Wither Rose, Cornflower, Blue Orchid, Azure Bluet, Oxeye Daisy, White Tulip, Allium, Orange Tulip, Pink Tulip, Cactus Flower, Poppy, Red Tulip, Open Eyeblossom, Closed Eyeblossom, Lily of the Valley, Dandelion, Wildflowers, Torchflower and Pink Petals give 2 dye. Lilac, Peony, Rose Bush, Sunflower and Pitcher Plant give 4. Golden Dandelion is left out because it is crafted from gold nuggets, and Beetroot because it is not a flower. Reading vanilla's dye recipes from the registry at datagen time would save the table but would pull in Beetroot and the Golden Dandelion and depend on how vanilla names its recipes. Going by Minecraft versions, the list rarely changes.

**Disjoint inputs are checked by a GameTest, not by convention.** A new GameTest collects every loaded `servus_foundation` milling recipe and asserts that no item is accepted by two of them. Every ingredient's items are checked across all recipes. A future recipe that clashes with an existing one then fails the build instead of quietly never running.

**No PLAN.md open decision is settled here.** Ore doubling stays undecided and is recorded as an open point in `PLAN.md` when this change is archived.

APIs used, all existing: `RecipeOutput.accept` with `ServusRecipes.key`, `SizedIngredient`, `Ingredient.of(HolderSet)` for tags through the registries lookup (as in `ServusFoundationRecipeProvider`), `ItemStackTemplate(Item, int)`, and NeoForge `Tags.Items`.

Pure Java: nothing new. Minecraft-touching: datagen output and GameTests in `MillGameTests`.

## Risks / Trade-offs

- [A modpack tag adds a block to `#c:cobblestones/normal` that another mod already mills] → The first matching recipe wins. That is the existing lookup rule, and packs can override either recipe by id.
- [Wool to 4 String turns dyed wool back into string, so the dye is lost] → Accepted. Color is the player's choice to throw away.
- [Doubling dye and making bone meal cheaper makes dyes and bone meal easier to get] → Small numbers in JSON, and the play check can lower them.
- [`millRefusesOtherItems` uses Cobblestone as the refused item] → The test moves to Dirt, the same item as the updated spec scenario.
