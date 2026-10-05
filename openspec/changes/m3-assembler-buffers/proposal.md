# Proposal

## Why

Playtest feedback on the configurable assembler: nine input slots cannot buffer two recipes, and crafting remainders such as buckets must come out of the machine, not back into the inputs. Follow up to the assembler crafting change, in M3.

## What Changes

- Assembler Mk1 gets 18 input slots (three rows of six) and 4 output slots (two by two). Results and remainders go to the output slots; a craft waits until both fit.
- The assembler GUI grows to 256 pixels wide: inputs left, pattern grid centre, plus and clear buttons, outputs, energy bar right, player inventory centred below.
- `MachineMenu` and `MachineScreen` learn a configurable player inventory offset and energy bar position for wide GUIs.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `manufacturing/assembler`: buffer sizes and where remainders go.

## Non-goals

- Upgrades that add slots; higher tiers get their own numbers later.
- Output filtering per slot.

## Impact

- Existing assemblers keep their items (slot indices 0 to 8 stay inputs; the old output slot 9 becomes an input slot, so an item left there is now an input).
