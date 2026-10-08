# Proposal

## Why

During the M8 play test the user asked for the M8 blocks to match the black and woody look of the Heavy Duty Rack, for the wood to be chosen by the recipe like the rack's, for a formed Crafting CPU to read as one connected, animated block, and for the Pattern Storage to show how many patterns it holds. This is a follow up item of PLAN.md M8 (on-demand autocrafting) that polishes its blocks before the next play test.

## What Changes

- A shared wood material in Foundation: a `servus_foundation:wood` item component holding a planks block id, a shaped recipe type that records the wood of the planks in the grid, a retexturing model that swaps a board marker texture for the wood, and a tooltip line. Oak when no wood is set.
- Eleven blocks get the iron and wood look (dark riveted iron frame, wooden board panels, teal accents) and take their wood from the recipe: Crafting CPU, Access Point, Inbound Station, Outbound Station, Counter Terminal, Production Terminal, Pattern Storage, Crafting Station, Warehouse Controller, Shuttle Garage and Machine Adapter. Their recipes all contain planks; recipes that had none get a row of planks.
- A formed Crafting CPU shows every face of the cube as one 3x3 face with a continuous iron frame and teal data lights that are animated: running while the CPU has orders, a slow glow while idle. Unformed blocks show a riveted casing.
- The Pattern Storage front shows a 3x3 grid of lights, one lit per stored pattern.
- Crates and racks keep their look.

## Capabilities

### New Capabilities
- `foundation/wood-material`: wood chosen by the recipe, kept on the item and the block, shown as the board panels and in the tooltip, and the iron and wood look of the listed blocks.

### Modified Capabilities
- `foundation/machine-adapter`: the adapter recipe contains planks and records the wood.
- `storage/access-point`: the Access Point recipe contains planks.
- `storage/crafting-station`: the station recipe contains planks.
- `storage/inbound-station`: the station recipe contains planks.
- `storage/outbound-station`: the station recipe contains planks.
- `storage/crafting-cpu`: the formed cube look and the active lights.
- `storage/patterns`: the Pattern Storage indicator lights.

## Non-goals

- Moving the Heavy Duty Rack onto the shared wood material (it keeps its own component and model).
- One wood for a whole formed CPU: each block keeps its own wood, so a mixed cube shows mixed boards.
- Wood for crates, rails, elevators, ducts.
- Re-rendering the docs item icons; the user's icon export run does that.

## Impact

- Foundation: new `foundation.wood` package (component, pure pick, recipe and serializer, block entity helper, item, client model), datagen helper for layered models, the Machine Adapter.
- Storage: ten block entities store the wood, blockstate datagen, loot tables, recipes, dismantle drops, the CPU block state (position and active), Pattern Storage block state (lights).
- Textures: new overlay textures for all eleven blocks, CPU tiles with `.mcmeta` animations, a shared board marker texture.
- Existing placed blocks show oak boards (unreleased, no migration).
