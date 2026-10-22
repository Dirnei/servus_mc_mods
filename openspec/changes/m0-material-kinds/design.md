# Design: m0-material-kinds

## Context

See proposal.md for why. Today `foundation/.../wood/` holds the whole mechanism:

- `Wood`: default oak, the `ModelProperty` for the wood, the board placeholder `servus_foundation:block/wood_board`, the `SWAPS` property, the set of registered wood blocks, `stamp` for hand built drops.
- `WoodSlot` (block entity side: component, save key `Wood`, update tag, model data), `WoodHolder`, `WoodBlockItem` (tooltip `tooltip.servus_foundation.wood`), `WoodContent` (component `servus_foundation:wood`, serializer `servus_foundation:wood_shaped`).
- `WoodRecipe` (a `ShapedRecipe` subclass that reads `ItemTags.PLANKS` in `assemble`), `WoodPick` (pure pick rule, `WoodPickTest`).
- Client: `WoodModel` (a `DynamicBlockStateModel` wrapper), `WoodItemModel`, `WoodModels` (wraps registered blocks in `ModelEvent.ModifyBakingResult`, registers the item model type `servus_foundation:wood` in `RegisterItemModelsEvent`), `WoodSkin` (board, property, fallback, item lookup; the rack brings its own), `WoodTextures` (`spriteOf` is the particle sprite of the material block's default state, which works for any block).
- Datagen: `WoodRecipes.output` wraps a `RecipeOutput`; `ModelProvider.itemModelTypeOf` writes `servus_foundation:wood` for registered blocks; `LayeredModels` puts the board under an overlay.

The Heavy Duty Rack in Shuttles has its own component (`servus_shuttles:rack_wood`), recipe and skin, and writes the save key `Wood` by hand.

## Goals / Non-Goals

**Goals:**
- One mechanism, named material, with a kind per block and per recipe.
- The wood kind behaves exactly as the wood material did; the stone kind exists and resolves, ready for `m9-stone-sink-chimney`.

**Non-Goals:**
- A public kind registry for other mods; the two kinds are constants in Foundation.
- Changing the rack's own component and recipe; only its save key follows the rename.

## Decisions

### D1 Names
Package `io.github.dirnei.mc.servus.foundation.material` with `Material`, `MaterialKind`, `MaterialSlot`, `MaterialHolder`, `MaterialBlockItem`, `MaterialContent`, `MaterialRecipe` and `MaterialPick` (test `MaterialPickTest`); client `MaterialModel`, `MaterialItemModel`, `MaterialModels`, `MaterialSkin`, `MaterialTextures`; datagen `MaterialRecipes`. Ids: component `servus_foundation:material`, serializer `servus_foundation:material_shaped`, item model type `servus_foundation:material`, save key `Material` (in `MaterialSlot` and in `HeavyRackBlockEntity`).

`Material` collides with `net.minecraft.client.resources.model.sprite.Material`, which `MaterialModel` uses for `particleMaterial`. `MaterialModel` refers to the vanilla class by its full name; no other file needs both. A name like `ServusMaterial` was rejected because the user asked for the plain rename.

### D2 Kinds as an enum
`MaterialKind` is an enum with `WOOD` and `STONE`. Each constant holds:

| field | wood | stone |
|---|---|---|
| serialized name (recipe and item model JSON) | `wood` | `stone` |
| ingredient tag | `ItemTags.PLANKS` | `servus_foundation:stones` (`TagKey.create(Registries.ITEM, ...)`) |
| default block | `minecraft:oak_planks` | `minecraft:bricks` |
| placeholder texture | `servus_foundation:block/wood_board` | `minecraft:block/bricks` |
| tooltip key | `tooltip.servus_foundation.wood` ("Wood: %s") | `tooltip.servus_foundation.stone` ("Stone: %s") |

An enum keeps the codec a one liner (`StringRepresentable.fromEnum`) and keeps the set closed, as the non-goal wants. `Material.register(blockId, kind)` replaces `Wood.register(blockId)`; `Material.kindOf(blockId)` returns the kind or null; `Material.or(id, kind)`, `Material.of(stack)` (kind from the stack's block item), `Material.block(id, kind)` and `Material.stamp` follow the old helpers.

### D3 One component, kind from the block
The component stays a plain `Identifier` (the material block). The kind is not stored: it belongs to the block and is looked up through `Material.kindOf`. `Material.or` falls back to the kind's default when the id names no block. A record component `{kind, id}` was rejected as redundant: no stack or block entity needs its kind without its block.

### D4 Recipe names its kind
`MaterialRecipe` adds a `kind` field to the shaped recipe codec (`MaterialKind.CODEC.fieldOf("kind")`) and to its stream codec. `assemble` collects, in reading order, every ingredient stack that carries a material component (a rail in a rail recipe) or is a block item in the kind's tag, and records `MaterialPick.pick`. `MaterialRecipes.output(output, registries, kind)` replaces `WoodRecipes.output(output, registries)`; every current call passes `MaterialKind.WOOD`. Recipe viewers still see a shaped recipe (the class extends `ShapedRecipe`, as today), so JEI keeps cycling the planks tag. The game data reader (`CraftingRecipeReader`) goes by shape, so the extra field does not disturb it.

### D5 Skin per kind
`MaterialSkin` keeps its role (placeholder, model data property, fallback, item lookup, swaps); Foundation's own skins come from `MaterialSkin.of(kind)`, one per kind, with the kind's placeholder and default. The rack still builds its own skin with `heavy_rack_board`. `MaterialModels` wraps each registered block's state models with the skin of the block's kind.

### D6 One item model type with a kind field
Only `servus_foundation:material` is registered. Its JSON gains a field: `{"type": "servus_foundation:material", "model": ..., "kind": "wood"}`, and the codec picks the skin from it. `ModelProvider.itemModelTypeOf` writes the type and the kind from `Material.kindOf(itemId)`. A type per kind (`material_wood`, `material_stone`) was rejected because the user named one loader.

### D7 Stone tag content
`ServusFoundationItemTagsProvider` adds `servus_foundation:stones` with full cube blocks only (no slabs, stairs or walls):
- bricks;
- stone, smooth stone, stone bricks, mossy, cracked and chiseled stone bricks, cobblestone, mossy cobblestone;
- andesite, diorite, granite and their polished forms;
- deepslate, cobbled deepslate, polished deepslate, deepslate bricks and tiles with their cracked forms, chiseled deepslate;
- blackstone, polished blackstone, polished blackstone bricks, cracked polished blackstone bricks, chiseled polished blackstone;
- tuff, polished tuff, tuff bricks, chiseled tuff, chiseled tuff bricks;
- mud bricks;
- nether bricks, red nether bricks, cracked and chiseled nether bricks;
- sandstone, cut, chiseled and smooth sandstone, and the same four red sandstone forms;
- quartz block, quartz bricks, quartz pillar, chiseled quartz block, smooth quartz;
- end stone, end stone bricks;
- prismarine, prismarine bricks, dark prismarine.

### D8 Textures from the particle sprite
`MaterialTextures.spriteOf` stays the particle sprite of the material block's default state model. For stone this gives the side texture of sandstone, quartz pillar and similar, which is what a wall of that block shows. No per kind texture table is needed.

### Assumptions
- The rack's component `servus_shuttles:rack_wood`, its `HeavyRackRecipe` and its `rack_wood` item model type keep their names; only its save key becomes `Material`, as asked.
- The lang key for wood stays `tooltip.servus_foundation.wood`; stone gets `tooltip.servus_foundation.stone`.
- `LayeredModels` keeps its board placeholder (wood kind); it gains no stone variant in this change.

### Pure logic and tests
- Pure: `MaterialPick`. The `WoodPickTest` cases move to `MaterialPickTest` unchanged, plus one case with stone ids to show the rule is kind independent.
- Minecraft, GameTest in Foundation (`material/MaterialGameTests`): the stone kind defaults to `minecraft:bricks`; Stone Bricks and Polished Deepslate are in `#servus_foundation:stones` and Stone Brick Slab is not; a stone kind `MaterialRecipe` built in code over a grid of two Stone Bricks and one Bricks assembles a result carrying Stone Bricks. Existing Foundation and Shuttles GameTests (block style, rail look, field look, elevator, pattern result, shuttle module, heavy rack, adapter) stay green after the rename.
- Manual: the play check that wood blocks look, drop and name their wood as before, and that JEI shows any planks.

### NeoForge and vanilla APIs used
All are in use in the current code and only renamed: `DeferredRegister.DataComponents.registerComponentType`, `RecipeSerializer` with `MapCodec` and `StreamCodec`, `ShapedRecipe`, `DynamicBlockStateModel`, `ModelEvent.ModifyBakingResult`, `RegisterItemModelsEvent`, `MutableQuad.setSpriteAndMoveUv`, `ModelData` and `ModelProperty`, `CopyComponentsFunction.copyComponentsFromBlockEntity`, `ItemTagsProvider`, `TagKey.create(Registries.ITEM, ...)` (as in `Upgrades.TAG`). `StringRepresentable.fromEnum` is the vanilla enum codec helper.

### PLAN.md
No open decision of PLAN.md is settled here; PLAN.md needs no edit.

## Risks / Trade-offs

- [The rename touches about 50 files in two mods] → Move the package first, then fix users mod by mod with a build after each step; a stale import fails the compile at once.
- [Dev worlds lose their wood: items with `servus_foundation:wood` and blocks with save key `Wood` show oak] → Accepted, the mod is unreleased. The play check uses freshly crafted blocks.
- [The item model `kind` field must match the registered block] → Written by datagen from `Material.kindOf`, never by hand.

## Migration Plan

None: unreleased, no data fixer. Datagen regenerates every affected JSON and `docs/src/data/gamedata.json`.
