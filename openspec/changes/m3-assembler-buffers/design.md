# Design

## Context

The configurable assembler has nine inputs and one output and returns remainders to the inputs. See proposal.md.

## Decisions

### D1: Slot layout
Inputs 0 to 17, outputs 18 to 21. `fits` simulates result and remainders on a scratch copy of the four output stacks; `finishWork` inserts across the output slots in order. Output push and side rules treat all four as outputs.

### D2: Wide GUI
`AssemblerScreen` sets `imageWidth` 256 and the inventory label at x 48; `MachineMenu.playerInventoryX()` (default 8, assembler 48) places the player slots; `MachineScreen.energyX()` (default 152, assembler 240) places the energy bar. Positions: inputs 8/17 by 6 by 3, pattern 124/17, plus 182/17, progress bar 182/38, clear 182/55, outputs 202/17 by 2 by 2, energy 240/17. The background texture is regenerated at 256 by 166.

## Pure Java vs Minecraft

All Minecraft. Verified over RCON with the vanilla cake recipe (three milk buckets return three buckets) and the client launch.

## Risks / Trade-offs

- [Old output slot index becomes an input] -> Test worlds only; noted in the proposal.
