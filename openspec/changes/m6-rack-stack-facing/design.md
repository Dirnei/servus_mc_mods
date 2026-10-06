# Design

## Context

See proposal.md for motivation. `HeavyRackBlock.getStateForPlacement` sets `FACING` from the player's horizontal look direction and then computes the join flags with `withJoins`. The `UP` and `DOWN` flags only join racks with the same facing, so a rack placed on top with a different facing today renders as a separate bay.

## Goals / Non-Goals

**Goals:**
- Stacked racks join without the player having to match the facing.

**Non-Goals:**
- Inheriting the facing from a rack beside the placed one.

## Decisions

### Facing from the rack below
`getStateForPlacement` reads the block state below the clicked position. When it is a `HeavyRackBlock`, the new state takes its `FACING`; otherwise the player's direction is used as before. The join flags are computed after that, so `DOWN` is true for the stacked rack and the rack below gets `UP` through the normal block update. Alternative considered: also copying from a side neighbour. Rejected per the proposal non-goals; rows need the player's choice because a rack beside another may deliberately face the aisle on the other side.

### Test
`GameTestHelper.placeAt(player, stack, pos, Direction.UP)` uses the rack item on the top face of the rack below, which runs the real placement path with the mock player's look direction. The test asserts the placed rack's facing equals the lower rack's and that `DOWN` is set on it.

## Pure versus Minecraft logic

No pure logic. One placement branch and a GameTest.

## Risks / Trade-offs

- [Player wanted a different facing on top] -> They can rotate it with the Configuration Carrot, which recomputes the joins.
