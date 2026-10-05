# Proposal

## Why

The first two machines turn deposits into ingots and establish the machine framework (block, block entity, inventory, GUI, output push, datagen) that every later machine copies. PLAN.md milestone M2, item "Burner Extractor, Burner Smelter (fuel handling, fixed rates)".

## What Changes

- Machine framework in `servus_manufacturing`:
  - `MachineBlock`: horizontal facing, lit state, opens the machine GUI, drops contents when broken, wrench rotates and sneak wrench dismantles.
  - `MachineBlockEntity`: item inventory with per slot roles, item handler capability with side rules (inputs insert only, outputs extract only), output push into adjacent inventories, client sync of progress through the menu.
  - `BurnerMachineBlockEntity`: fuel slot using vanilla burn times, burns only while there is work.
  - `MachineMenu` and `MachineScreen` base with fuel flame and progress arrow.
  - Machine stats (work time in ticks, later energy numbers) loaded from datapack JSON under `data/<ns>/machine/`.
- **Burner Extractor**: placed on top of a deposit block, produces that deposit's yield every work period while fueled.
- **Burner Smelter**: smelts its input through vanilla smelting recipes at a fixed rate while fueled.
- Datagen: blockstates and models (orientable with lit front), loot tables, crafting recipes, lang. Placeholder textures and GUI backgrounds.

## Capabilities

### New Capabilities
- `manufacturing/machines`: behavior shared by all Servus machines: facing, GUI, inventory sides, output push, wrench, dismantling, data driven stats.
- `manufacturing/burner-extractor`: what the extractor needs and produces.
- `manufacturing/burner-smelter`: what the smelter accepts and produces.

### Modified Capabilities
None.

## Non-goals

Deferred to later changes:
- Electric machines, energy buffers and cables (next change: coal generator and power cable).
- Assembler, research lab, item duct.
- JEI or EMI integration (M3).
- Final art.

## Impact

- New blocks, block entities, menu types, screens and recipes in Manufacturing.
- First `RegisterCapabilitiesEvent` and `RegisterMenuScreensEvent` usage in the family.
- Datapack files `data/servus_manufacturing/machine/burner_extractor.json` and `burner_smelter.json` carry the work times.
