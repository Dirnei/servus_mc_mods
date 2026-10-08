# Design

## Context

`OrderPlanner.Walk.need` (Foundation, pure Java) covers a need from surplus, then stock on hand, then produces the rest with `producerOf(key)`: the first recipe in the list that outputs the key. The CPU passes the recipes of every producer and pattern on the data line. A furnace adapter and the Burner Smelter list one smelting recipe per ingredient (via `SingleInputRecipes`), so for charcoal there are about ten alternatives and acacia happens to come first. See proposal.md.

## Goals / Non-Goals

**Goals:** choose alternatives by stock, split across them, keep single recipe plans byte for byte the same, stay deterministic.

**Non-Goals:** a preference setting; merging alternatives into one tag recipe.

## Decisions

### Single recipe path unchanged
When exactly one recipe makes the key, `need` runs the old code. Existing tests and orders keep their step order.

### Phase 1: alternatives covered by stock
While some of the need is left, compute for every alternative how many whole batches its inputs can cover from surplus plus unreserved stock, and take the alternative with the most covered batches (ties by recipe order). Run `min(needed batches, covered)` batches through the normal input path (which only reserves, since the inputs are on hand) and record the step. Every round takes at least one batch, so the loop ends.

### Phase 2: produce the rest
For what is still left, simulate each alternative on a copy of the walk state (reserved, surplus, missing, steps) and plan the remaining batches with it. Pick, in order: a simulation that adds nothing missing with the fewest new steps; otherwise the one adding the least missing; ties go to an alternative already used in phase 1 for this key, then to recipe order. The chosen simulation's state becomes the walk's state. Nested needs inside a simulation use the same rules, bounded by the depth limit.

Alternative considered: a global optimiser over all alternatives at once. Rejected: far more code, and greedy by stock answers the play test complaint.

### Step order
Steps stay leaves first: a phase 1 step needs no produced inputs; when phase 2 adds more batches to a recipe that phase 1 inserted in the same call, that step is moved behind the steps phase 2 added for its inputs. Recipes inserted before this call keep their place, as before.

### Preview
`OrderBook.preview` already reports `plan.reserved()` per item, so the craft preview lists 6 Oak Log and 4 Birch Log without UI work.

### Testing
JUnit in `OrderPlannerTest`: stocked alternative, split, produce inputs of an alternative, missing of the best alternative, tie by order, single recipe unchanged (existing tests). Storage GameTests: charcoal through a furnace adapter with only birch logs, and with oak plus birch split.

### PLAN.md open decisions
None.

## Risks / Trade-offs

- [Simulation cost grows with alternatives times subtree size] -> the depth limit bounds it; charcoal has about ten alternatives with one level each.
- [Greedy choice is not globally optimal for shared inputs] -> acceptable; the preview shows what will be used before Start.
