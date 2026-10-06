# Proposal

## Why

The Crate and the Shelving Rack are two unrelated ideas: bulk storage of one type, and four tiny stacks with a front display. The user decided on 2026-10-06 to make them one family: a crate has one, two or four compartments that split its capacity, and the old rack disappears, freeing the name "rack" for the Heavy Duty Rack of the next change. This reshapes the PLAN.md section 2.3 progression item "Crates and shelving racks" and refines the milestone M4 items "Crates" and "Shelving racks with visible contents".

## What Changes

- The crate becomes a family of three blocks: **Crate** with one compartment, **Double Crate** with two side by side, **Quad Crate** with four in a two by two grid. All share one block class, one block entity and one renderer, differing only in the compartment count.
- Compartments split the crate capacity from `crate.json`: one compartment holds the full capacity, two hold half each, four hold a quarter each. Each compartment locks to one item type while it holds items, like the crate does today.
- Hand interaction is per compartment, aimed at the front face: right click inserts the held stack into that compartment, a quick second right click moves every matching stack from the inventory into it, left click takes a stack out of it, sneak left click takes one. A crate that holds anything never starts breaking.
- Every compartment shows its item and count on the front face. Automation sees one slot per compartment on every side.
- The Shelving Rack block, its block entity, renderer, recipe, loot, tag, lang and spec are removed. Its two by two display lives on in the Quad Crate.
- Recipes: the crate as today, a Double Crate from a crate and a stick, a Quad Crate from a crate and two sticks.
- Connection lamp and rail docking keep working: all three are stock holding storage nodes.
- PLAN.md: section 2.3 and the M4 list name the crate family instead of crates plus shelving racks.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/crate`: every requirement changes to speak of compartments, plus a new requirement for the three variants.

### Removed Capabilities
- `storage/shelving-rack`: retired. Its compartment display becomes part of `storage/crate`.

## Non-goals

- The Heavy Duty Rack. It is the next change, `m6-heavy-duty-rack`.
- Different capacities per variant beyond the split. Capacity stays one number in `crate.json`.
- A GUI for crates. Crates stay hand and automation only.

## Impact

- `servus_storage`: `CrateBlock` takes a compartment count, `CrateBlockEntity` holds a compartment array, `CrateItemHandler` exposes one slot per compartment, a pure `CrateCompartments` helper with JUnit tests replaces `RackCompartments`, `CrateRenderer` draws per compartment, the `rack` package is deleted, registries drop the rack and add two crate blocks, datagen gains two blockstates, front textures, recipes, loot, tags and lang, `ServusStorageEvents` left click handling works per compartment, GameTests keep working with the single compartment crate.
- Generated resources: rack files go away, double and quad crate files appear.
- Specs: `storage/shelving-rack` is removed at archive time.
