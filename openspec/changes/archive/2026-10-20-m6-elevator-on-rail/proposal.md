# Proposal

## Why

A Rail Elevator can only be placed on bare ground. A player who has laid a rail where the shaft should go has to break the rail first, and the elevator then draws its own rail in its floor anyway. The Fluid Gantry gets the same rule in its play check: a rail the block brings belongs to it, a rail that was there before stays.

## What Changes

- Using an elevator on a Warehouse Rail puts the segment in the rail's place. The segment remembers that rail, with its wood.
- The segment's own floor rail, which the elevator always draws, is part of the segment as before.
- When that segment goes (broken, dismantled with the carrot, or dropped because its support went), the remembered rail comes back where it was, unless another block took the space.
- A segment placed on bare ground remembers no rail and leaves nothing behind, as today.
- Sneak clicks on a rail place the segment on the rail the same way. Clicks on a segment keep building the shaft up.
- Segments have a wood, like rails: the sleepers of every rail the shaft draws (its floor rail, decks and the car's rail) show it. A segment placed on a rail takes that rail's wood; a crafted elevator takes the wood of the rail in its recipe; a broken segment drops an elevator item that keeps its wood, and the item shows it.
- Foundation's wood recipe also reads the wood of an ingredient that carries one (a rail), not only of planks.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `shuttles/rail-elevator`: new requirements for placing a segment on a rail and getting the rail back, and for the wood of segments.

## Impact

- `shuttles/.../rail/ElevatorItem.java` (placement on a rail), `rail/ElevatorBlockEntity.java` (the remembered rail and a `WoodSlot`, saved), `rail/ElevatorBlock.java` (puts the rail back after removal), `client/ElevatorRenderer.java` (sleepers in the segment's wood), GameTests.
- `foundation/.../wood/WoodRecipe.java` (wood from ingredients that carry it), `foundation/.../wood/client/WoodTextures.java` (sprite lookup made public for renderers).
- Data: the elevator recipe becomes `servus_foundation:wood_shaped`; the elevator item model becomes a wood item model.
- Farm Rails are not taken: a field rail is not a warehouse aisle.
