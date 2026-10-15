# Design

## Context

See proposal.md for the why. Foundation's machine base (`MachineBlock`, `MachineBlockEntity`, `MachineMenu`, `MachineScreen` with a `Layout`) gives facing, lit state, side configuration with the front locked off, slot role rules for automation, output push, carrot configure and dismantle, drops, and data driven stats from `data/<ns>/machine/<name>.json` with code defaults in `MachineDefaults`. The removed Burner Smelter (commit 6c7e7f657) was a burner machine on that base that smelted with `SingleInputRecipes` as its production view; the Forge follows it without the fuel. The vanilla furnace reaches the data line only through a Machine Adapter profile; the Forge exposes the production capability itself like the Athanor and the Coagulator. Alchemy's multiblock machines lock all sides off (`AlchemyMachines.closeAllSides`) because their ports are fixed; the Forge is a single block machine, so it keeps Foundation's side configuration.

## Goals / Non-Goals

**Goals:**
- A furnace that smelts every one item smelting recipe without fuel, at a pace from data.
- The Foundation machine behaviour (screen, sides, push, carrot, drops) and the production capability for the Crafting CPU.
- A compact family style model with an always burning, full bright fire.

**Non-Goals:**
- Blasting or smoking recipes, a speed upgrade, experience from smelting, a fuel slot, upkeep.
- Any Elemental Sight interaction: the Forge is not hidden.

## Decisions

### D1 Classes
New package `forge`: `SalamanderForgeBlock extends MachineBlock` (block entity type, default state lit, client `animateTick` with flame and smoke particles at the mouth and the furnace crackle now and then), `SalamanderForgeBlockEntity extends MachineBlockEntity` (2 slots: 0 input, 1 output) and `SalamanderForgeMenu extends MachineMenu` (input at 56,17, output at 116,35, like the Burner Smelter without its fuel slot at 56,53). Registered in `ServusAlchemyBlocks`, `ServusAlchemyItems` (creative tab after Ignis Perpetuus), `ServusAlchemyBlockEntities`, `ServusAlchemyMenus`.

### D2 Smelting
The server looks the recipe up with a `RecipeManager.createCheck(RecipeType.SMELTING)` cached check and `getRecipeFor(new SingleRecipeInput(stack), serverLevel)`, then `recipe.value().assemble(input)` for the result (both verified in the 26.1.2 sources of `AbstractFurnaceBlockEntity`). Whether a stack may go into the input is `level.recipeAccess().propertySet(RecipePropertySet.FURNACE_INPUT).test(stack)`, which works on the client and the server, so the menu slot and automation agree. A work cycle runs while the input has a recipe and the output takes the result; when it finishes, one input item is used and the result goes to the output.

### D3 Work time from data
`AlchemyMachineDefaults` registers `salamander_forge` as kind `crafter` with recipe type `minecraft:smelting` and `work_ticks` 200; `data/servus_alchemy/machine/salamander_forge.json` repeats it. The Forge uses `work_ticks` for every recipe instead of each recipe's cooking time: every vanilla smelting recipe takes 200 ticks, so it smelts at a vanilla furnace's pace, and a datapack makes it faster with one number. The production recipes report the same time.

### D4 Always lit
`tick()` returns true, so the base keeps `LIT` true; the default state is lit, so a freshly placed Forge is lit before its first tick. The light level is a constant 13 in the block properties. The block state model ignores `LIT` (one model, rotated by facing). The menu data returns 1 for burn time and burn duration, so the screen draws the full furnace flame.

### D5 Sides follow Foundation
The Forge keeps Foundation's default side configuration: every side Auto, the front locked off. Auto lets pipes and hoppers insert into the input and extract from the output and lets the Forge push its output into neighbours, so a hopper above and a chest below work out of the box. Players who want strict top in, bottom out set it with the carrot or the side panel.

### D6 Production capability
`ServusAlchemyEvents` registers `Capabilities.Item.BLOCK` (`sideHandler(side)`) and `ServusFoundationCapabilities.PRODUCTION` for the Forge block entity on every side. The provider lists `SingleInputRecipes.list(server, RecipeType.SMELTING, recipe -> workTicks())` and reports as active the recipe whose input matches the input slot (the Burner Smelter's code). No `UPKEEP` capability, so on a data line the Forge is a producer only. The CPU delivers order inputs through the item capability like it does for the Athanor.

### D7 Screen
`client/MachineScreens.SALAMANDER_FORGE = new Layout(true, true, false, gui("salamander_forge"))`: flame, arrow, no energy bar, with the side panel; registered in `ServusAlchemyClient` with a `MachineScreen`. The background `textures/gui/salamander_forge.png` is Foundation's `burner_machine.png` with the fuel slot painted over, made by a script in the job directory.

### D8 Model
Foundation's `ModelBuilder.ElementBuilder` gains `lightEmission(int)`, written as the element's `light_emission` (field verified in 26.1.2 `CuboidModelElement`, 0 to 15). Quads take their chunk layer from the sprite's transparency (`BakedQuad.MaterialInfo.of`), so the fire's cutout needs no render type. In model space the front faces north (z 0). Boxes (pixels):
- back block, deepslate bricks, `(0,0,4)-(16,16,16)`;
- left and right pillars, deepslate bricks, `(0,0,0)-(3,16,4)` and `(13,0,0)-(16,16,4)`;
- lintel `(3,11,0)-(13,16,4)` and sill `(3,0,0)-(13,3,4)`, deepslate bricks;
- embers, magma, light emission 15, `(3,3,2)-(13,5,4)`;
- fire, vanilla `fire_0`, a plane at z 3 from `(3,5)` to `(13,11)` with north and south faces, uv `0,6,16,16`, light emission 15;
- emblem plate `(5,12,-0.5)-(11,15,0)`, front face the new `salamander_forge_emblem` texture (uv `0,4,16,12`), sides, top and bottom the family steel `servus_foundation:block/electric_pump_steel`, no back face.

Overlap check:
- Back block and pillars, lintel, sill: they meet at z 4 with faces of opposite direction only; the pillars' outer faces (x 0 and x 16, z 0..4) and the back block's (z 4..16) lie on one plane but only touch along an edge.
- Pillars and lintel, sill: meet at x 3 and x 13 with opposite faces; their front faces at z 0 touch only along edges.
- Embers: bottom on the sill top at y 3, sides on the pillars at x 3 and 13, back on the back block at z 4: all opposite faces. Its front at z 2 and top at y 5 are free.
- Fire plane at z 3: no other face lies at z 3 (embers front z 2, back block z 4); its edges touch the pillars and the embers only.
- Emblem plate: its back would lie on the lintel front at z 0, so it has no back face; its other faces are at z -0.5, x 5, x 11, y 12, y 15, where nothing else lies.
- Top faces at y 16 (back, pillars, lintel) touch only along edges.

### D9 Recipe
Shaped crafting recipe in `ServusAlchemyRecipeProvider`: `DDD / DID / DFD` with D Deepslate Bricks, I Ignis Perpetuus, F Furnace. Ignis Perpetuus needs a Citrinitas Athanor and a befriended salamander, so the recipe is gated by the stage without a research check, like every other crafting recipe of the family. A furnace given the eternal flame reads as what it is.

### D10 Workshop, JEI, docs
`WorkshopCategory` adds `salamander_forge` "The Salamander Forge" (icon the Forge) at the left of "The Salamander", parent "The Salamander", condition the Schwaz research node like the salamander entry: spotlight, the crafting page, and a text on using it (no fuel, pipes, data line). `ServusAlchemyJeiPlugin` adds the Forge as a crafting station for `RecipeTypes.SMELTING` (verified in JEI 29.43 API). Docs: a Salamander Forge section in `docs/.../mods/alchemy.md` and a row in `reference/datapack/machines.md`.

## Risks / Trade-offs

- [A free furnace makes coal less useful] → It needs a befriended Nether salamander and a Citrinitas Athanor; the pace is the vanilla furnace's, so it saves fuel, not time.
- [Auto sides push output into any neighbour inventory, also a duct beside it] → Same as every Foundation machine; the side panel sets other modes.
- [One work time for all recipes] → Every vanilla smelting recipe takes 200 ticks; modded recipes with other times smelt at the Forge's pace.

## Assumptions to confirm with the user

- Speed: 200 ticks per item for every recipe (`work_ticks` in `salamander_forge.json`), the pace of a vanilla furnace; not faster.
- Smelting only (`minecraft:smelting`), not blasting or smoking.
- No experience is given for smelted items.
- Sides: Foundation's default (every side Auto, front closed), not a fixed top in, bottom out; the carrot sets that up.
- Recipe: a crafting table recipe `DDD / DID / DFD` (Deepslate Bricks, Ignis Perpetuus, Furnace), not an Athanor craft.
- Light level 13, always, also when idle.
- The Workshop entry opens with Wandering years, like "The Salamander"; no journal task.
- Look: deepslate brick body, open mouth with magma embers and a full bright fire, a small steel plate with a salamander above the mouth; no chimney.
