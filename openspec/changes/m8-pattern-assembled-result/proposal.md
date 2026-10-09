# Proposal

## Why

Play test bug in PLAN.md M8 (on-demand autocrafting): a request for 2 Heavy Duty Racks was crafted by a Crafting Station but the order waited forever. A pattern keeps the recipe's plain result (`getResultItem`, no components), while the station crafts with `assemble`, which gives the rack the wood of its planks. The CPU collects outputs by item and components, so the crafted racks never match the expected result. Every block whose wood comes from the recipe (the racks and the iron and wood blocks of `m8-block-style`) is affected, and the Assembler reports crafting recipe results the same wrong way.

## What Changes

- A pattern's result is what its recipe really makes from its own template: the template is assembled with the recipe when the pattern is saved.
- Patterns saved before this change get their result assembled again the first time their Pattern Storage is used on a loaded level, so nobody has to save them again.
- Everything that reads a pattern's result (CPU planning through pattern recipes, craftables and requests in the Counter Terminal, the craft preview, the Production Terminal, chain handoffs) uses the assembled result, because they all read it from the pattern.
- The Assembler reports, for a configured crafting recipe, the result assembled from its template instead of the plain recipe result.
- Safety measure requested by the user: a Crafting Station whose outputs still hold items that are not its current job's is not idle. It refuses new jobs, the CPU gives the step to another station, and the Production Terminal shows it blocked ("outputs not collected").

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/patterns`: a pattern's result is the assembled result of its template; old patterns are refreshed.
- `manufacturing/assembler`: the reported result of a configured recipe is what it really crafts.
- `storage/crafting-station`: outputs that wait keep a station from taking other jobs.

## Non-goals

- Healing orders that are already stuck in a saved world: their steps and requests expect the old item without components. The player cancels the order in the Production Terminal, takes the items out of the station and requests again.
- The editor preview in the Production Terminal keeps showing the plain recipe result (display only).

## Impact

- `storage/.../pattern/{EncodedPattern,PatternEditing,PatternStorageBlockEntity}.java`
- `manufacturing/.../machine/assembler/{ConfiguredRecipe,AssemblerBlockEntity}.java`
- `storage/.../crafting/CraftingStationBlockEntity.java`, `storage/.../cpu/CpuBlockEntity.java`, `storage/.../production/ProductionView.java`
- GameTests in Storage and Manufacturing.
