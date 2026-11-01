# Proposal

## Why

A player reported that the Fluid Tank cannot be crafted from the Storage Terminal. JEI's transfer button does nothing for the Fluid Tank, Facades and Hollow Facades, at the Storage Terminal's order grid and at the Production Terminal's editor. Their real recipes are custom recipes without ingredients, so JEI shows stand-in recipes whose ids the server does not know, and the terminals find nothing to lay out. This is in the released Foundation 0.7.0 and Shuttles 0.7.0.

Milestone: M5 Logistics (Fluid Tank, facades). This is a bug fix, not an open point from PLAN.md.

## What Changes

- New ingredient type `servus_foundation:floor_block` that accepts any solid full block without a block entity (the rule facades and tank floors already use) and lists every such block for recipe viewers.
- The Fluid Tank recipe becomes a real shaped recipe (`CGC` / `G G` / `CFC`) with the floor ingredient in the bottom middle. The crafted tank still gets the floor block. JEI shows this real recipe, and the floor cell cycles through the allowed blocks. The JEI stand-in recipe goes away.
- The Facade recipe becomes a real shapeless recipe (floor block plus anything in `c:nuggets`) giving six facades of that block.
- The Hollow Facade recipe becomes a real shaped ring of eight Facades around an empty centre giving eight Hollow Facades. Facades of different blocks still craft nothing.
- Recipe ids, result counts and what players can craft stay the same. Already crafted items keep their components. No migration.
- JEI's transfer button at both terminals also sends the items JEI shows in each cell. When the server does not know the recipe id (display-only recipes such as the facade examples, from us or any mod), the cells are laid out from those items, most stocked first. "Facade (Oak Planks)" lays out oak planks. Known recipes keep today's lookup, so the Fluid Tank lays out the floor block the warehouse holds most of.
- JEI keeps the per-block Facade and Hollow Facade example recipes. The generic facade recipes are hidden in JEI, because their plain output would show as an extra, nameless facade.
- CHANGELOG `Fixed` lines for Foundation and Shuttles.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/fluid-tank`: crafting is a plain shaped recipe whose floor cell accepts any solid full block, and recipe viewers show it with the allowed floors.
- `foundation/facades`: Facade and Hollow Facade crafting are plain recipes; JEI keeps the per-block examples and does not show the generic recipes.
- `shuttles/storage-terminal`: JEI fills the order grid from the stacks JEI shows per cell, which also works for display-only recipes.
- `shuttles/production-terminal`: the recipe viewer transfer fills the editor the same way.

## Non-goals

- No new recipes, no change to what blocks count as a floor or facade, no balance change.
- EMI and REI transfer handlers. They read the real recipes from now on, but no transfer into the terminals is added for them.
- Changing how Any patterns pick materials. The tank's floor following the input is whatever the existing material rule does with the new recipe.

## Impact

- Foundation: `logistics/tank/FluidTankRecipe`, `logistics/facade/FacadeRecipe`, `logistics/facade/HollowFacadeRecipe`, new floor ingredient and its registration in `NeoForgeRegistries.INGREDIENT_TYPES`, `ServusLogisticsRecipes`, datagen in `LogisticsDataParts`, `integration/jei/ServusFoundationJeiPlugin`, GameTests in `FluidTankGameTests` and `ServusLogisticsGameTests`.
- Shuttles: `integration/jei/StorageTerminalRecipeTransferHandler` and `ProductionTerminalRecipeTransferHandler`, `OrderGridRecipePayload` and `OpenRecipeEditorPayload` carry shown cells instead of an id, `StorageTerminalMenu.fillFromRecipe`, `ProductionTerminalMenu.openEditorWith`, `pattern/CraftingGrid.templateOf`.
- Generated recipe JSON for `fluid_tank`, `facade` and `hollow_facade` changes from an empty custom type to full recipe JSON under the same ids.
- Foundation and Shuttles are released together. Shuttles does not need a higher minimum Foundation version.
