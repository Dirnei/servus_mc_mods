# Design

## Context

`PatternEditing.save` stores `recipe.getResultItem(...)`. `CraftingStationBlockEntity` crafts with `CraftingRecipe.assemble(CraftingInput, ...)`; `WoodRecipe` and `HeavyRackRecipe` override `assemble` to add the wood component of the planks. The CPU collects outputs whose `ItemKey` (item plus components) equals the expected output (`CpuBlockEntity.collect`, `PacketIo` uses `isSameItemSameComponents`), so wood blocks are never collected. The Assembler reports `ConfiguredRecipe.displayResult` (`getResultItem`) but crafts with `assemble`. See proposal.md.

## Decisions

### One helper for the real result
`EncodedPattern.assembledResult(Level, ResourceLocation recipe, List<ItemStack> template)`: look the recipe up, build `CraftingInput.of(3, 3, template)`, and if it is a `CraftingRecipe` that matches, return `assemble(input, level.registryAccess())`; otherwise fall back to `getResultItem`, and to the stored result when the recipe is gone. `EncodedPattern.assembled(Level)` returns the pattern with that result (same instance when nothing changes). `PatternEditing.save` stores the assembled pattern.

### Refreshing old patterns lazily
`PatternStorageBlockEntity` keeps a `refreshed` flag cleared by every restore (load, component apply). The read accessors (`pattern(slot)`, `usedSlots()` callers go through `pattern`) call `refresh()` when the flag is clear and the level is a server level: every kept pattern is replaced by its assembled form, `setChanged()` when anything changed. Doing it lazily avoids touching recipes during chunk loading (`loadAdditional` has no level) and covers the placed-item path, where components are applied after `onLoad`.

All consumers (PatternLibrary, PatternRunners, Counter Terminal craftables and requests, craft preview, ProductionView, chain handoffs, station jobs) read patterns through the Pattern Storage, so they see the assembled result without further changes.

### Assembler
`ConfiguredRecipe.producedResult(Level)`: for a crafting recipe the template assembled like the Assembler crafts it (`CraftingInput.of(3, 3, template)`, `assemble` when it matches); for an assembling recipe `getResultItem` (its `assemble` returns the same copy); `displayResult` remains for the GUI. `AssemblerBlockEntity.production()` reports `producedResult`.

### Audit
- Crafting Station: crafts with `assemble` (reference behaviour).
- Burner Smelter: reports `assemble` results already.
- Machine Adapter (`SingleInputRecipes`): vanilla cooking recipes return the same stack from `assemble` and `getResultItem`; no change.
- Crusher, assembling recipes: `assemble` returns `result.copy()`, identical; no change.
- Assembler with crafting recipes: was wrong, fixed here.

### Stations with outputs are not idle
Added at the user's request as a safety measure. `CraftingStationBlockEntity.hasOutputs()` and `blockedFor(owner)`: outputs that wait are closed to everyone but the owner of the current job; leftovers after a release (owner null) close the station to everyone. `assign` refuses when `blockedFor(owner)`. The CPU host's `busyElsewhere(producer)` reports such a station as busy unless its job belongs to this CPU (a `StepRef` with this CPU's position), so the planner sorts it last and a waiting step relocates to another free station through the existing `relocate`. The Machines tab shows a station with no job and items in its outputs as `blocked` with the reason "outputs not collected". The station's push without a job is unchanged, so leftovers usually leave on their own; a station without any neighbour to push into stays blocked until a player or automation empties it.

### Stuck orders
Orders already waiting in a saved world expect the old item in their step outputs, their order item and the linked terminal request. Healing them would mean rewriting saved steps, orders and requests by guessing which components belong together, which can collect unrelated stacks. The collector already retries on its normal interval, so new orders work; old stuck ones are cancelled by the player (X in the Production Terminal), the racks taken from the station, and requested again. Recorded as a non-goal.

## Testing
- Storage GameTests: 2 Heavy Duty Racks requested through a CPU from an oak rack pattern arrive in the terminal; a Pattern Storage requested from a birch pattern arrives with birch wood; a Pattern Storage loaded with an old result returns the assembled result.
- Manufacturing GameTest: an Assembler configured by id with the Machine Adapter recipe (wood shaped; the template takes the first planks of the tag) reports the adapter with that wood.
- Storage GameTest: a station holding leftovers is skipped and a second free station runs the step; the Machines tab shows it blocked.

## Risks / Trade-offs
- [Recipes whose `assemble` depends on more than the template (none among ours)] -> the stored result is what that template makes; the station assembles the same template.
- [Lazy refresh writes on a read] -> only once per load, and only `setChanged`.
