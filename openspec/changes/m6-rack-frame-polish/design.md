# Design

## Context

See proposal.md for motivation. `HeavyRackBlock` has `FACING`, `LEFT`, `RIGHT`, `UP`, `DOWN`; the horizontal flags mean "a same facing rack sits on that side" and hide that side's post. `HeavyRackModels` writes a shelves model with a back panel `0,0,14 to 16,16,16` that the posts, the top and bottom beams overlap, which is the flicker. `WarehouseConnectionRule.accepts(kind, side, kind)` lets any stock node link to a rail on any side, and `RailBlock.docksTo` docks onto any stock holding node. `StorageNodeBlockEntity` has `kind()`. `NetworkBlockEntity.refreshNetworkConnections()` re-evaluates a node's edges.

## Goals / Non-Goals

**Goals:**
- Disjoint boxes everywhere, continuous boards and beams across joined bays, posts on a fixed rhythm.
- Front only rail links without touching the pure rule table for crates and terminals.

**Non-Goals:**
- Diagonal or mixed facing rows. Only same facing neighbours join.

## Decisions

### Post rhythm from the bay index
The pure helper `RackRow` in the `rack` package: `leftPostHidden(int bayIndex, int postsEvery)` is `bayIndex > 0 && bayIndex % postsEvery != 0`, and `rightPostHidden(int bayIndex, int bayCount, int postsEvery)` is `bayIndex < bayCount - 1 && (bayIndex + 1) % postsEvery != 0`. `RackRowTest` covers a single bay, a row of three, a row of five with posts every three, and `postsEvery` of one. `HeavyRackBlock` counts same facing racks to the left (`facing.getCounterClockWise()`) and to the right to get `bayIndex` and `bayCount`, and sets `LEFT` and `RIGHT` from the helper. The flags now mean "no post on this side, shelves run through". `UP` and `DOWN` keep their meaning.

### Row refresh
Adding or removing a bay changes the index of every bay to its right. `onPlace` and `onRemove` on the server walk the row in both directions and, for every rack whose recomputed state differs, call `setBlock` with `UPDATE_CLIENTS`. `updateShape` keeps handling direct neighbours. Rows are short, so the walk is cheap and only runs on placement, removal and rotation.

### Disjoint model
Coordinates in model space, front at z 0. Boxes:
- back panel `0,0,15 to 16,16,16`, always; full size so it continues across joins and stacks.
- floor board `2,0,2 to 14,1,13`, middle shelf `2,8,2 to 14,9,13`, front mid beam `2,8,0 to 14,9,2`, always.
- top beams `2,15,0 to 14,16,2` and `2,15,13 to 14,16,15` unless `UP`; bottom beams `2,0,0 to 14,1,2` and `2,0,13 to 14,1,15` unless `DOWN`.
- left post `0,0,0 to 2,16,2`, `0,0,13 to 2,16,15` and side beam `0,8,2 to 2,9,13` unless `LEFT`; right post mirrored at x 14 to 16 unless `RIGHT`.
- left extension when `LEFT`: floor `0,0,2 to 2,1,13`, shelf `0,8,2 to 2,9,13`, front mid beam `0,8,0 to 2,9,2`; plus top beam stubs `0,15,0 to 2,16,2` and `0,15,13 to 2,16,15` when also not `UP`, bottom stubs likewise when not `DOWN`. Right extensions mirrored.
Posts sit at z 0 to 2 and 13 to 15, in front of the panel, so nothing overlaps. Eleven parts per facing, rotated like before. The item model is the core plus both posts and the top and bottom beams.

### Front only rail links
`StorageNodeBlockEntity.acceptsRail(Side side)` defaults to true; `HeavyRackBlockEntity` returns `side == Conversions.toSide(facing)`. The network type's rule becomes `accepts(kinds) && railSideOk(from, side, to)`, where the check applies when exactly one of the two nodes is a rail: the stock node must accept the side that faces the rail. `RailBlock.docksTo` asks the same method with the direction from the rail to the block, mapped to the block's view as the opposite side. Rotating a rack calls `refreshNetworkConnections()` through a public `facingChanged()` on the block entity after the state change, and the rails beside it update through the normal block update.

## Pure versus Minecraft logic

Pure: `RackRow` with `RackRowTest`. Minecraft: block, models, block entity, rule lambda, rail dock, GameTest `rackLinksRailOnlyFromFront` (a rack facing north with a rail on its east side and a controller on that rail stays offline and undocked; a rail on its north side links it and the terminal lists its contents).

## Risks / Trade-offs

- [Row walk on every placement] -> Bounded by row length, event driven.
- [Rack rotated inside a row] -> It leaves the row visually and the neighbours recompute through the row refresh on rotation.
