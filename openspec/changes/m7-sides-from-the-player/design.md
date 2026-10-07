# Design

## Context

`RelativeSide.toSide(facing)` maps LEFT to the counter clockwise neighbour of the facing and RIGHT to the clockwise one, which is the block's own left and right. The panel, the carrot message, the markers and all stations and machines go through this one mapping.

## Goals / Non-Goals

**Goals:**
- One line change in the mapping, everything else follows.

**Non-Goals:**
- See proposal.

## Decisions

### Settles no PLAN.md open decision

### Mapping
LEFT maps to the clockwise neighbour of the facing, RIGHT to the counter clockwise one. Facing north: left is east, right is west. `fromSide` is derived from `toSide` and needs no change.

### Minecraft side verification
`RelativeSideTest` asserts the new mapping; `machinePullsFromNeighbour` asserts west is RIGHT for a machine facing north and sets RIGHT to Pull. The existing GameTests that use `relativeSide(Direction)` dynamically keep passing.

## Risks / Trade-offs

- [Saved configurations] → nothing is released.

## Open Questions

None.
