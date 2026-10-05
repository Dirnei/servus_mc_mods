# Tasks

## 1. Recipes and items

- [x] 1.1 Add `AssemblingRecipe`, `AssemblingRecipeSerializer`, `AssemblerRecipeInput`, `AssemblingRecipes` (sorted lookup) and `ServusManufacturingRecipes` registries, plus the items iron gear, copper wire and red pack with an item model provider and textures. Verify `gradlew build` passes.
- [x] 1.2 Add `AssemblingRecipeBuilder` and the three Tier 1 assembling recipes plus the assembler crafting recipe to the recipe provider. Verify `gradlew :manufacturing:runData` writes `recipe/assembling/iron_gear.json`, `copper_wire.json`, `red_pack.json` and `recipe/assembler.json`.

## 2. Assembler

- [x] 2.1 Add `AssemblerBlock`, `AssemblerBlockEntity` (selection, matching, consumption, data index 6), `AssemblerMenu` with `clickMenuButton`, registry entries, capabilities, stats JSON, blockstate, loot, tag and lang entries, textures. Verify `gradlew build` and `gradlew :manufacturing:runData` pass.
- [x] 2.2 Add the background texture to `MachineScreen.Layout`, `AssemblerScreen` with the recipe panel, and register it. Paint `gui/assembler.png`. Verify `gradlew build` passes.

## 3. Verification

- [x] 3.1 Server check over RCON: generator beside an assembler, four iron ingots in the inputs, iron gear selected through `data merge`; after 3 seconds the output holds gears and energy was spent. Then a cable chain variant: generator, three cables, assembler; the assembler's buffer fills and it crafts. Verify with `data get block`.
- [x] 3.2 Full check: `gradlew clean build` green, `gradlew runClientAll` reaches the title screen with no model, texture, recipe or screen errors. Commit.
