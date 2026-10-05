# Design

## Context

The crate established the Storage patterns: `StorageNodeBlockEntity`, update tag sync with a throttled server ticker, a block entity renderer, wrench handling. See proposal.md.

## Decisions

### D1: Storage model
`RackBlockEntity extends StorageNodeBlockEntity` with an `ItemStackHandler(4)` whose `onContentsChanged` marks dirty and schedules a sync (same 10 tick throttle as the crate, same pattern as the crate). Slots are numbered 0 top left, 1 top right, 2 bottom left, 3 bottom right as seen from the front.

### D2: Compartment from the hit
`RackBlock.compartmentAt(state, hit)` takes the hit location relative to the block. Vertical: y above 0.5 is the top row. Horizontal: the coordinate along the facing's clockwise axis, mirrored so that "left" is the viewer's left when looking at the front. A hit on a face other than the front still maps through the same coordinates.

### D3: Interaction
`useItemOn` with a non wrench item inserts into the hit compartment via the handler (`insertItem(slot, stack, false)`), returns the remainder to the hand. `useWithoutItem` takes the stack (or one when sneaking) from the hit compartment into the player's inventory. Wrench: rotate, sneak dismantle with `saveToItem` after moving the handler contents to the item through the block entity's save.

### D4: Renderer
`RackRenderer` draws each non empty slot as a `GUI` display context item scaled to a quarter block at the compartment's centre, 0.004 off the front face, and the count with `Font.drawInBatch` at the lower right of the quarter when above one.

### D5: Recipe and assets
Crafting: sticks on the sides, planks top and bottom, rows `SPS / S S / SPS`? Keep it simple: `PPP / S S / PPP` gives 2 racks. Textures: side and top share the crate's wood, front is a dark open shelf with a lighter back board.

## Pure Java vs Minecraft

All Minecraft. Verified over RCON: hopper fills two compartments 64 and 36; `data merge` of mixed slots and `data get` after a hopper below pulled one item; client launch.

## Risks / Trade-offs

- [Hit mapping off by one on east and west faces] -> Mapping covered by a JUnit test of the pure index function (`RackCompartments`), no Minecraft imports.
