# Proposal: m0-material-kinds

## Why

Foundation's wood material lets a block show the planks it was crafted from, but it only knows wood: the planks tag, oak, the `wood_board` placeholder and the "Wood:" tooltip are hardwired. The Sink and the Chimney should show the stone they were crafted from (change `m9-stone-sink-chimney`), and that needs the same mechanism with a second kind. PLAN.md lists no open point for this; the request comes from play. It is a pure refactor plus a new, still unused stone kind, so it carries the `m0` prefix (cleanup) and does not change anything a player sees.

## What Changes

- The wood material becomes a material mechanism with kinds. Classes, package, data component, save key, recipe serializer and item model type are renamed from wood to material: package `foundation.material`, component `servus_foundation:material`, block entity save key `Material` (also in the Heavy Duty Rack of Shuttles), recipe type `servus_foundation:material_shaped`, item model type `servus_foundation:material`.
- A kind names its ingredient tag, its default block, its placeholder texture and its tooltip:
  - **wood**: any `#minecraft:planks`, default Oak Planks, placeholder `servus_foundation:block/wood_board`, tooltip "Wood: <block name>". Every existing material block is wood, so nothing a player sees changes.
  - **stone**: any `#servus_foundation:stones`, default Bricks, placeholder vanilla `minecraft:block/bricks` (quads already drawn in bricks take the stone), tooltip "Stone: <block name>".
- Each material block and each material recipe names its kind.
- A new item tag `#servus_foundation:stones` with the vanilla full cube stone blocks (bricks, stone bricks, cobblestone, stone, deepslate, blackstone, tuff, sandstone, quartz, prismarine and their full cube variants), extendable by datapacks, written by datagen.
- No block uses the stone kind yet.
- The mod is unreleased: no migration and no data fixer for the old component, key or recipe type.

## Capabilities

### New Capabilities

- `foundation/materials`: blocks that show the material they were crafted from, with the kinds wood and stone; carries over every requirement of `foundation/wood-material` with wood as one kind and adds the stone kind and its tag.

### Modified Capabilities

- `foundation/wood-material`: retired. All requirements move to `foundation/materials`.

## Impact

- `foundation/.../wood/` (13 classes and `WoodPickTest`) moves to `foundation/.../material/`.
- Foundation users: `ServusFoundation`, `adapter/` (Machine Adapter), `block/ConnectableBlock`, datagen (`WoodRecipes`, `LayeredModels`, `AdapterBlockLoot`, `model/ModelProvider`, `ServusFoundationRecipeProvider`, `ServusFoundationLanguageProvider`, `ServusFoundationItemTagsProvider`).
- Shuttles users: about 30 classes (blocks, block entities, items, renderers, datagen, GameTests), the rack skin in `ServusShuttlesClient` and the hand written `Wood` key in `HeavyRackBlockEntity`.
- Regenerated resources: recipe JSON (`type` and `kind`), item model definitions (`servus_foundation:material`), loot tables (component id), the new tag file, lang, and `docs/src/data/gamedata.json`.
- The game data reader (`CraftingRecipeReader`) and the docs site go by the recipe shape, not its type, so they need no change; the test fixture `docs/src/lib/fixtures/tier1-gamedata.json` stays as it is.

## Non-goals

- Any block of the stone kind; the Sink and the Chimney follow in `m9-stone-sink-chimney`.
- Further kinds (metal, glass) or a kind registry open to other mods.
- Migration of worlds or items made with the old wood component.
- Changes to the pick rule, the look of any block, or the material swaps used by copper rails.
