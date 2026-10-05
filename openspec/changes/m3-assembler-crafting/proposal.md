# Proposal

## Why

The playtest verdict: the assembler is good, but it should make every recipe out there, not only the three Servus ones, and a machine should be set up with a limited number of recipes. This turns the assembler into a general purpose crafter with a cap that higher tiers raise, which is the Factorio feel the plan asks for. Pulled into M3 at the user's request.

## What Changes

- Pure Java `IngredientGatherer` (take required counts out of slots) and `RoundRobin` in Servus Lib, tested.
- The Assembler Mk1 holds up to `recipe_slots` configured recipes (default 2, in the machine stats JSON). It crafts any vanilla or modded crafting recipe and any assembling recipe. Configured recipes run round robin whenever their inputs are present.
- Nine input slots, one output slot. Crafting remainders such as buckets return to the inputs.
- Configuration in the GUI: a 3 by 3 ghost pattern grid where the player lays out a recipe with items from the cursor (nothing is consumed) and presses plus; a clear button; a panel on the right listing the configured recipes with the active one highlighted; clicking one removes it. Feedback for refusals appears on the action bar.
- JEI: the plus button on any crafting or assembling recipe adds it to the open assembler. The assembler is also listed as a crafting catalyst.
- Research gating moves from selection to configuration: a locked assembling recipe cannot be added.
- Crafting recipes take `work_ticks` (default 20); assembling recipes keep their own time.

## Capabilities

### New Capabilities
- `lib/crafting-helpers`: ingredient gathering and round robin selection shared by crafting machines.

### Modified Capabilities
- `manufacturing/assembler`: recipe selection becomes recipe configuration with a cap; any crafting recipe is allowed.

## Non-goals

- Assembler Mk2 and the higher caps themselves (only the stat exists).
- Alternative ingredients per cell (the template fixes one concrete item per ingredient).
- Smelting, stonecutting or other non crafting recipe types in the assembler.

## Impact

- Breaking for existing assemblers: the single selected recipe is dropped on world load; the player configures recipes again. Worlds from the playtest are test worlds.
- `MachineStats` gains `recipe_slots`.
- Second client to server payload.
