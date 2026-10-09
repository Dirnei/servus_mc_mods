# Proposal: m9-foundation-utilities

PLAN.md milestone M9 (Manufacturing becomes alchemy), item 1: `m9-foundation-utilities`.

## Why

Manufacturing is being rebuilt as Paracelsus alchemy (PLAN.md 2.1). The Burner and Electric Pump, the Sink with its Chimney, the Chunk Loader and the Coal Generator are not alchemy, and Storage needs them as much as Manufacturing: a warehouse voids overflow and runs overnight, and Foundation's Fluid Pipe and Fluid Tank have no fluid source of their own. Moving them first keeps them safe from the rewrite that follows and gives every Servus mod an FE source.

## What Changes

- The Burner Pump, Electric Pump, Sink, Chimney, Chunk Loader and Coal Generator move from `servus_manufacturing` to `servus_foundation`. Their block and item ids become `servus_foundation:<name>`, their behavior stays the same.
- The shared machine base that these blocks stand on (machine block, block entity with its burner and powered variants, energy buffer, machine stats from data, owner, machine menu and screen) moves to Foundation too, so the moved blocks and the remaining Manufacturing machines use one base.
- All six blocks get plain crafting table recipes from vanilla materials. The Electric Pump and the Chunk Loader lose their assembler recipes and the research requirement, because Foundation has no assembler and no research, and the gear and wire items they used are going away with the old Manufacturing content.
- Machine stats files for the moved blocks move to `data/servus_foundation/machine/`, the Sink rate table to `data/servus_foundation/sink/`.
- The Chunk Loader config (`maxChunkLoadersPerTeam`, `chunkLoaderRespectsFtbChunks`) moves to the Foundation config.
- The GameTests of the moved blocks move to Foundation. The generator stays an upkeep only member of the data cable.
- The game data export and its docs pages are regenerated so the docs list the moved blocks under their new ids.

Nothing is released, so old worlds are not migrated: blocks placed with the old ids disappear.

## Capabilities

### New Capabilities
- `foundation/machines`: the shared rules every Servus machine follows (facing, inventory sides, output push, side configuration, wrench, stats as data, lit state, powered machines). Moved from `manufacturing/machines`, since the base now lives in Foundation and serves every mod.
- `foundation/pumps`: Burner and Electric Pump on an infinite water source, moved from `manufacturing/pumps`, both crafted by hand.
- `foundation/sink`: the Sink multiblock with its Chimney, moved from `manufacturing/sink`.
- `foundation/chunk-loader`: the Chunk Loader with team cap and FTB Chunks rule, moved from `manufacturing/chunk-loader`, crafted by hand.
- `foundation/coal-generator`: the Coal Generator, moved from `manufacturing/coal-generator`.

### Modified Capabilities
- `manufacturing/machines`: every requirement removed, now covered by `foundation/machines`.
- `manufacturing/pumps`: every requirement removed, now covered by `foundation/pumps`.
- `manufacturing/sink`: every requirement removed, now covered by `foundation/sink`.
- `manufacturing/chunk-loader`: every requirement removed, now covered by `foundation/chunk-loader`.
- `manufacturing/coal-generator`: every requirement removed, now covered by `foundation/coal-generator`.

## Non-goals

- No alchemy content. The Calciner, Coagulator, principles and the removal of deposits, extractors, smelter, assembler, research and packs belong to `m9-tria-prima` and `m9-journal`.
- No balance changes. Rates, buffers and costs keep their current values.
- No new behavior for the moved blocks, apart from the recipes.
- No change to the data cable, Machine Adapter or upkeep API.

## Impact

- Code: `manufacturing/.../machine` base classes, `machine/generator`, `machine/pump`, `machine/loader`, `machine/sink`, their menus and screens, the Sink renderer, registries, datagen providers and config move under `foundation/...`. Manufacturing machines (extractors, smelter, crusher, assembler, research lab) import the base from Foundation.
- Data: machine stats JSON, the Sink JSON, recipes, loot tables, blockstates, models, textures and lang entries move namespace.
- Tests: pump, sink and loader JUnit tests and GameTests move to Foundation; the native upkeep GameTest uses the new generator id.
- Docs: `docs/src/data/gamedata.json`, icons and the docs pages that name these blocks.
