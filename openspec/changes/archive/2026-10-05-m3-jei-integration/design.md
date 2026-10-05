# Design

## Context

Manufacturing has a custom assembling recipe type. No recipe viewer support exists. See proposal.md.

JEI 19.57.0.451 API inspected with javap: `IModPlugin` with `registerCategories`, `registerRecipes`, `registerRecipeCatalysts`; `IRecipeCategory` with `getRecipeType`, `getTitle`, `getIcon`, `getWidth`, `getHeight`, `setRecipe`, `draw`; `RecipeType.createRecipeHolderType(ResourceLocation)`; `IRecipeLayoutBuilder.addInputSlot(x, y)`, `addOutputSlot(x, y)`, `IRecipeSlotBuilder.setStandardSlotBackground`, `addItemStacks`, `addItemStack`; `IGuiHelper.createDrawableItemLike`, `createAnimatedRecipeArrow`.

## Decisions

### D1: Dependencies
Root `subprojects` repositories gain BlameJared (JEI), Modrinth maven (Jade), FTB maven (Ultimine, FTB Library) and Architectury maven, each limited to its group through `content { includeGroup }`. Manufacturing declares JEI common and NeoForge API as `compileOnly` and the JEI mod plus Jade, FTB Ultimine, FTB Library and Architectury as `localRuntime`, so nothing is published as a hard dependency.

### D2: Plugin
`ServusManufacturingJeiPlugin` (`@JeiPlugin`) with a `RecipeType<RecipeHolder<AssemblingRecipe>>` created from the assembling recipe type id, the `AssemblingRecipeCategory` (120 by 54: two by two input grid, animated arrow, output, time text) and the assembler as catalyst. Recipes come from the client level's recipe manager at registration time.

### D3: Lang
Category title key `gui.servus_manufacturing.jei.assembling`.

## Pure Java vs Minecraft

All Minecraft and JEI. Verified by a client launch with JEI present (no plugin errors in the log, JEI reports the plugin) and a manual look in game.

## Risks / Trade-offs

- [JEI API changes across 19.x] -> Versions pinned in `gradle.properties`.
- [FTB and Jade jars in the run but not shipped] -> Documented as dev conveniences; the mod metadata declares no dependency on them.
