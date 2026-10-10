# Proposal

## Why

The Calciner is now built in the world; the Coagulator, its counterpart that joins the principles again, should be built the same way so the lab reads as one craft. The user asked for it to be done autonomously as a first proposal, with the structure itself to be discussed afterwards.

PLAN.md milestone: M9 (Manufacturing becomes alchemy), item 6c `m9-coagulator-multiblock`.

## What Changes

- The Coagulator becomes a build, one block deep, 3 wide and 2 tall: three Principle Vats on top (Sulfur left, Salt in the middle, Mercury right, the same sides as the Calciner's tanks) above a row of Stone Bricks, the Coagulator (the casting table) and Stone Bricks. The Stone Bricks stay vanilla blocks.
- It forms with Paracelsus' Journal on the Coagulator, names a missing block when it cannot, and unforms when any block of it is broken. The carrot plays no part.
- Fixed ports instead of the side configuration: each Vat takes only its own principle from pipes and buckets; the Coagulator block takes the ingredient and gives the product through the item capability.
- The Coagulator keeps its GUI for picking the recipe, opened with an empty hand on the formed Coagulator; the side panel goes, and the gauges stay for reading and for pouring buckets. Picking a recipe without a GUI is left for the structure discussion.
- Formed, the Vats pour through copper spouts into the mold, and each Vat shows its level.
- The Journal's Coagulator page gets the schematic with Visualize.
- Shared, pure structure geometry (`Facing`, `Offset`) moves out of the Calciner into `alchemy/structure`, used by both.

## Capabilities

### New Capabilities
- `manufacturing/coagulator-structure`: the Coagulator build: layout, Vats, forming, unforming, ports by position, look and schematic.

### Modified Capabilities
- `manufacturing/coagulator`: the principles now enter through the Vats instead of sides with a Fluids layer, buckets pour into the Vats, the GUI loses its side panel, it works only when formed, and its owner is the player who formed it.

## Impact

- Manufacturing: `alchemy/CoagulatorBlock`, `CoagulatorBlockEntity`, new `alchemy/coagulator` package (layout, structure, vat block, ports), `alchemy/structure` (shared geometry), the Calciner classes that used its own geometry, client screen layout and a vat renderer, capability registration, datagen (models, recipe of the Vat, lang, multiblock, journal page), GameTests.
- PLAN.md M9 item 6c text.

## Non-goals

- Recipe choice without a GUI (to be discussed).
- A formed shell for the Stone Bricks: they stay visible as they are.
- Changes to coagulation recipes or numbers.
