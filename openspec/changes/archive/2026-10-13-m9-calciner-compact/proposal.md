# Proposal

## Why

The Calciner build is 5 blocks wide, made of plain red bricks and has two tank columns that eat floor space. Players want a smaller, darker furnace that still reads as one machine with the still on top. This change reworks the look and footprint of the M9 Alchemy item "Calciner build" (PLAN.md M9: Tria Prima, Calciner, Purifier, Coagulator and Athanor builds). The machine's work, recipes and balance stay as they are.

## What Changes

- The Calciner structure becomes 3 wide and 5 tall instead of 5 by 5. Seen from the front, top to bottom: `D H .`, `D L R`, `S C S`, `T F T`, `E A E` (D Copper Duct, H Sulfur Hood, L Alembic, R Receiver, S Stone Bricks, C Calciner, T Fluid Tank, F Firebox, E Deepslate Tiles, A Ash Pan).
- Vanilla Bricks are no longer used. The bottom row is Deepslate Tiles on both sides of the Ash Pan, and the third row is Stone Bricks on both sides of the Calciner. Breaking a shell block drops the block it replaced.
- There is one Fluid Tank on each side of the Firebox instead of a column of three: Sulfur on the left, Mercury on the right.
- The Receiver sits directly right of the Alembic. The spout's air cell is gone.
- The Copper Duct runs from the Hood across and down into the left Stone Bricks. The Receiver stands over the right Stone Bricks. Neither touches its tank anymore, so the Calciner fills its Sulfur and Mercury tanks itself.
- The formed look changes:
  - The Ash Pan is drawn in Deepslate Tiles, the Firebox in Polished Deepslate and the formed mouth in Stone Bricks.
  - The smooth stone cornice and plinth bands are gone.
  - The Duct enters the left Stone Bricks through a raised cut copper collar.
  - The right Stone Bricks hold a shallow stone basin with a grated drain. The Receiver stands over it on copper legs.
  - Mercury drips from the Receiver into the basin while the Calciner works, and a Mercury puddle shows in the basin while the Mercury tank holds Mercury.
- The Calciner keeps having no GUI. Hand use, hoppers, pipes, buckets, upkeep and Jade work as before.
- The Copper Duct's formed pieces are renumbered. The Calciner now uses two pieces (elbow and down into the stone), and the Athanor pieces follow them. This changes nothing visible for the Athanor.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `manufacturing/calciner-structure`: layout, parts that make the shell, breaking drops, the formed look, and how Sulfur and Mercury reach their tanks.

## Impact

- `manufacturing` alchemy code:
  - `CalcinerLayout`: new cells and roles. `BRICK` is split into stone bricks and deepslate tiles, and `AIR` goes away.
  - `CalcinerStructure`: expected blocks, form and unform, and the neighbour check.
  - `CalcinerShellBlock`: cell range and loot by cell.
  - `CalcinerPorts`: push into the structure's own tanks.
  - `CalcinerPartBlock`: the Receiver lights up while working.
  - `CopperDuctBlock`: piece numbers.
  - `AthanorStructure` and `AthanorBlockEntity`: Athanor duct piece constants.
- `CalcinerRenderer`: the Receiver fill box, the basin puddle and the falling drop.
- Datagen:
  - `AlchemyModels`: mouth, Firebox, Ash Pan, Receiver, duct pieces and the shell models per cell.
  - `ServusManufacturingBlockLoot`: shell drops by cell.
  - `ServusManufacturingMultiblockProvider`: the Journal schematic symbols.
  - Lang, if any text names the old layout.
- Tests: `CalcinerLayoutTest` (JUnit) and `CalcinerGameTests`. The spout test is removed, and a test is added for the tanks being filled directly.
- PLAN.md: the M9 table row and the "Machines are built" paragraph describe the Calciner as a brick furnace with two tank columns. Both are updated.

## Non-goals

- No change to calcination recipes, fuel use, buffer capacity or any datapack balance.
- No GUI for the Calciner.
- No change to the Purifier, Coagulator or Athanor builds beyond the duct piece renumbering.
- No migration of already formed Calciners in existing worlds. The project is unreleased, so old builds simply unform or are rebuilt.
