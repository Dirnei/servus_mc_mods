# Proposal

## Why

Play test feedback (2026-10-07): left and right feel switched. Today they are the block's own left and right, so a machine that faces the player has its "left" on the player's right. Every other mod names sides as the player sees the front, and the side panel, the carrot message and the markers should agree with that.

## What Changes

- Left and right are the player's left and right when looking at the block's front. For a block facing north, left is east and right is west. Front, back, top and bottom are unchanged.
- The Lib mapping, its unit tests and the Manufacturing pull test follow.

## Capabilities

### Modified Capabilities
- `lib/side-config`: relative sides are named from the player's view of the front.

## Non-goals

- Changing the panel layout. The left cell already sits on the left.

## Impact

- `lib`: `RelativeSide.toSide` swaps the two rotations; `RelativeSideTest` updated.
- `manufacturing`: `machinePullsFromNeighbour` expects the chest on the west of a north facing machine to be its right.
- Nothing is released, so no placed block needs migrating.
