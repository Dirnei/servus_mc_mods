# Design

## Context

See proposal.md for motivation. `HeavyRackBlock.useItemOn` returns `SKIP_DEFAULT_BLOCK_INTERACTION` for the Configuration Carrot and `PASS_TO_DEFAULT_BLOCK_INTERACTION` otherwise, which runs `useWithoutItem` and opens the menu before the held item gets a chance. `BlockHitResult.getDirection()` is the clicked face.

## Goals / Non-Goals

**Goals:**
- Front face opens, every other face is a plain block.

**Non-Goals:**
- Sneak placement tricks; vanilla already lets sneaking skip block interaction.

## Decisions

### Face check in both interaction hooks
`useItemOn`: carrot keeps its skip; when the hit face is not `FACING`, return `SKIP_DEFAULT_BLOCK_INTERACTION` so the held item is used on the block, which places block items; otherwise pass to the default interaction. `useWithoutItem`: open the menu only when the hit face is `FACING`, else return `PASS`. Alternative considered: only skipping for block items. Rejected, a single rule for all faces is easier to learn and matches the front only rails.

## Pure versus Minecraft logic

No pure logic. The GameTest `rackPlacesAgainstRackSide` uses a rack item on the east face of a north facing rack and asserts a rack appeared there, then on the north face and asserts nothing was placed.

## Risks / Trade-offs

- [Players used to opening from any side] -> The front is where the shelves are; the open frame makes it obvious.
