# Proposal

## Why

The Assembler turns ingots into gears, wire and the first research pack, which is what the whole Tier 1 loop is about. PLAN.md milestone M2, items "Assembler Mk1 with recipe selection GUI" and "Items: iron gear, copper wire, Red Pack".

## What Changes

- Items **Iron Gear**, **Copper Wire** and **Red Pack** with placeholder textures.
- Recipe type `servus_manufacturing:assembling`: ingredients with counts, a result and a time in ticks, written as datapack JSON through datagen. An optional `tech` field is reserved for research gating.
- **Assembler Mk1**: a powered machine with four input slots and one output slot. The player selects one assembling recipe in the GUI; the machine then crafts that recipe whenever the inputs cover it, spending 20 FE/t, at the recipe's own time.
- Assembler GUI: inputs, output, progress arrow, energy bar and a recipe panel listing every assembling recipe; clicking one selects it. Selection travels through the vanilla menu button mechanism, so no custom packet is needed.
- The three Tier 1 assembling recipes from the plan and a crafting recipe for the assembler from vanilla materials.
- Energy capability on the assembler, so generators and cables can feed it. This is also the first end to end power test.

## Capabilities

### New Capabilities
- `manufacturing/assembler`: assembling recipes as data, recipe selection, crafting behavior and the Tier 1 items and recipes.

### Modified Capabilities
None.

## Non-goals

Deferred to later changes:
- Research gating of recipes (the `tech` field is parsed but ignored until the Research Lab change).
- Assembler Mk2 or speed upgrades.
- JEI or EMI display of assembling recipes (M3).
- Recipe book integration.

## Impact

- New registries for recipe types and serializers in Manufacturing.
- `MachineScreen` gets a per layout background texture.
- New datapack content: three assembling recipes, one crafting recipe, machine stats for the assembler.
