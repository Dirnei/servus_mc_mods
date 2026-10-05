# Design

## Context

Assembling recipes, the powered machine base, JEI plugin and research gating exist. See proposal.md.

## Decisions

### D1: Crushing recipe type
`CrushingRecipe(SizedIngredient input, ItemStack result, int time)` implementing `Recipe<SingleRecipeInput>`; `matches` tests the ingredient and count against the single input stack. Serializer with map and stream codecs like the assembling one. Registered as `servus_manufacturing:crushing`. `CrushingRecipeBuilder` writes to `recipe/crushing/<name>.json`.

### D2: Crusher block entity
`CrusherBlockEntity extends PoweredMachineBlockEntity`, slots 0 input and 1 output, defaults `powered(60, 20, 8000)`. The recipe lookup is cached per input item; `workTicks()` returns the recipe time; `canWork` requires the recipe, enough input count and output room; `finishWork` consumes the input count and inserts the result. Layout `CRUSHER` on a generated `crusher.png` (input at 56/35, output at 116/35, arrow, energy bar).

### D3: Items and smelting
`iron_grit`, `copper_grit` as simple items with painted textures; `SimpleCookingRecipeBuilder.smelting` and `blasting` recipes to ingots with 0.7 experience and vanilla times.

### D4: JEI
`CrushingRecipeCategory` with one input slot, arrow, output and time; the crusher is its catalyst.

### D5: Recipes and tech
Crusher assembling recipe: 1 Electric Extractor, 4 Iron Gear, 6 Copper Wire, 200 ticks, tech `crusher`. Crushing: raw iron to 2 iron grit (60 ticks), raw copper to 2 copper grit (60), cobblestone to gravel (40), gravel to sand (40). Tech unlock id updated.

## Pure Java vs Minecraft

All Minecraft. Verified over RCON: crusher with raw iron beside a generator yields two grit per cycle; grit in a smelter yields ingots; client launch for assets and JEI.

## Risks / Trade-offs

- [Grit as a second intermediate changes the reference block ratios if players switch] -> Opt in; the reference block uses the burner chain and its test is unchanged.
