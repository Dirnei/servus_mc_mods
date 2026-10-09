# Design

## Context

`ElevatorBlock` returns an empty collision shape and relies on the `minecraft:climbable` tag for climbing. The elevator item is an `AssistedBlockItem` with `ElevatorAssist` (sneak click walks up the shaft). Vanilla `ScaffoldingBlock.getCollisionShape` (1.21.1) returns its stable shape when `context.isAbove(Shapes.block(), pos, true) && !context.isDescending()`, otherwise empty. In `LivingEntity.handleOnClimbable` (NeoForge 21.1.255, line 2418) a sneaking player on a climbable block is held in place unless `BlockState.isScaffolding(entity)` is true; NeoForge exposes this as `IBlockExtension.isScaffolding(BlockState, LevelReader, BlockPos, LivingEntity)`, default `state.is(Blocks.SCAFFOLDING)`. `ScaffoldingBlockItem.updatePlacementContext` walks the column to the first non scaffolding block. See proposal.md for why.

## Goals / Non-Goals

**Goals:** stand on every level, sneak to go down, plain click grows the shaft upward.

**Non-Goals:** sideways growth, any lift car or network change.

## Decisions

### Collision
`ElevatorBlock.getCollisionShape` returns a top plate `Block.box(0, 14, 0, 16, 16, 16)` when the pure `ShaftFooting.stands(above, descending)` says so (`above = context.isAbove(Shapes.block(), pos, true)`, `descending = context.isDescending()`), otherwise `Shapes.empty()`. Only a plate, no legs: entering from the side and climbing inside stay free. Why a plate and not the full block: an entity inside the segment is never "above" it, so the plate only matters for entities at the top; the plate matches vanilla scaffolding's top thickness. The deck rendering is unchanged; the plate is invisible and sits where the next floor's deck plate already is, so standing looks right on rail floors and between posts elsewhere. A query without an entity (vanilla `CollisionContext.empty()`, whose `isAbove` returns true) gets the plate too, exactly like scaffolding.

### Sneaking descends
`ElevatorBlock` overrides `isScaffolding(...)` to return true, so a sneaking player is not held on the climbable segment and sinks while the plate is gone (descending). This changes the old "sneaking holds the player in place" rule; the requirement is updated.

### Plain click places on top
A new `ElevatorItem extends AssistedBlockItem` overrides `updatePlacementContext`, like `ScaffoldingBlockItem`: when the player is not sneaking and the clicked block is a segment, it walks up the column with the pure `ShaftTop.firstFree` (no reach limit) and returns `BlockPlaceContext.at(context, top, Direction.UP)`, or null (nothing placed, stack kept) when the first block above the shaft is not replaceable or is outside the build height (with the vanilla "build too high" message for a server player). Clicks on other blocks keep the default context. Sneak clicks keep going through `PlacementAssists.place` (assist with ghost preview, reach limited); both end on top of the shaft. `ServusStorageItems.ELEVATOR` uses the new class.

### Pure logic and tests
- `rail/ShaftFooting` (stands or not) and `rail/ShaftTop` (first free index above a column given a segment predicate and a height limit): JUnit `ShaftFootingTest`, `ShaftTopTest`.
- GameTests: `elevatorStandsFromAbove` (collision shape for a context above vs a descending context using `CollisionContext.of(entity)` with a mock player positioned above), the existing `elevatorHasNoCollision` reworked to check an entity inside gets no collision, `plainElevatorBuildsUp`, `plainElevatorBlockedTop`, existing sneak tests unchanged.

### PLAN.md open decisions
None; PLAN.md's elevator text ("Segments block nobody and are climbed like a ladder") gets "and can be stood on like scaffolding".

## Risks / Trade-offs

- [A player standing on the plate when the lift car arrives] -> the car is simulated and has no collision; nothing changes.
- [Entities like mobs standing in shafts] -> same as scaffolding; acceptable.
- [Plain click no longer places a segment against a side face] -> intended; segments could only survive there with support anyway.
