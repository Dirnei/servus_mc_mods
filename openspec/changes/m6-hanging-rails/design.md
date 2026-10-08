# Design: m6-hanging-rails

## Context

`RailBlock` (storage/rail) is a `ConnectableBlock` with dock flags. Today `canSurvive` asks for `canSupportRigidBlock` below, and `neighborChanged` drops the rail when that fails. Rails link sideways to rails and Rail Elevator segments (any side) and dock to storage nodes; a Heavy Duty Rack accepts a rail only on its front (`HeavyRackBlockEntity.acceptsRail`). Balance values for the warehouse live in `data/servus_storage/storage/warehouse.json`, read by `StorageStatsManager`.

## Goals / Non-Goals

**Goals:**
- Rails hang on rack fronts and elevator segments without a floor.
- Short gaps between holds are bridged by free rails, up to `rail_max_span`.
- Too wide gaps and overhangs collapse at once, with the rails dropped as items.
- No per tick scanning: checks run only after a block change next to a rail.

**Non-Goals:**
- Anchoring on crates, controllers, terminals, stations or garages.
- New models, support beams or a distinct look for hanging rails.

## Decisions

### Three rail kinds
A rail is classified from its own surroundings only:
- **standing**: `canSupportRigidBlock(level, pos.below())`, as today.
- **hung**: not standing, and a horizontal neighbour is an anchor: an `ElevatorBlock`, or a `HeavyRackBlockEntity` whose `acceptsRail(side)` is true for the side facing the rail, or a standing rail.
- **free**: neither.

Standing and hung rails always hold. Only free rails are checked.

A standing rail is an anchor so that a floor bridge is built like a rack bridge: one rail off each floor edge (hung), then the middle. Found during apply: without it, the first free rail beyond a floor edge has a single hold and falls at once, so no floor bridge could ever be built.

### Gap rule (pure, `RailSpan`)
Starting at a free rail, walk the links to other free rails (rails link sideways only, so the walk follows the rail network on one level). The walk collects the free rails of the gap and the distinct hung rails linked to them (a free rail never touches a standing rail, since that would make it hung). Then:
- span = free rails + hung rails linked to the gap
- holds = hung rails linked to the gap
- the gap holds when `span <= maxSpan` and `holds >= 2`
- otherwise every free rail of the gap and every hung rail linked to it is dropped.

This gives the cases the user asked for with a max span of 3: elevator, hung, free, hung, elevator holds (span 3); rack, hung, free, hung, rack holds; a fourth rail beside the middle of such a bridge makes the span 4 and drops all four; standing, hung, free, hung, standing holds; a hung rail with one free rail beside it collapses (one hold). A four block gap can never be bridged: its first free rail touches one hung rail only. A row of hung rails along an upper rack row has no free rail and is never checked, so it holds at any length.

Hung rails count towards the span so that the span equals the number of blocks between two anchors, which is how the user described the bridge ("rack, rail, rail, rail, rack"). Considered alternative: count only free rails. Rejected because a four block gap between two racks would hold, which contradicts the request.

The walk stops once the span passes `maxSpan`; the verdict is already "collapse" then. Rails of the gap that the walk did not reach are checked when their dropped neighbours notify them, so the cascade finishes in a few ticks without an unbounded search.

`RailSpan` is generic over the node type (`<N>`), takes a `Function<N, Kind>` and a `Function<N, List<N>>` of linked rails, and has no Minecraft imports. It is unit tested.

### When checks run
- `onPlace` and `updateShape` (any direction, so a removed floor block counts too) call `level.scheduleTick(pos, this, 1)`.
- `tick(BlockState, ServerLevel, BlockPos, RandomSource)` classifies the rail; if it is free, it runs `RailSpan.check` and destroys the drop set with `level.destroyBlock(pos, true)`.
- The tick also checks every free rail linked to the ticking rail, because a rail's kind depends on its neighbours: when a standing rail loses its floor but stays hung, the rail beside it turns free without being notified.
- Every change that can break a gap reaches a free rail of that gap: placing a rail notifies its neighbours, removing an anchor turns its hung rail free (that rail ticks), removing a floor block notifies the rail above, removing a rail notifies the free rails beside it.
- Duplicate scheduled ticks for the same position and block are merged by `LevelChunkTicks` (its `ticksPerPosition` set uses `ScheduledTick.UNIQUE_TICK_HASH`), so a burst of updates costs one check per rail.

The one tick delay means a rail that will collapse is visible for one tick. Accepted: it also avoids destroying blocks inside a neighbour update.

### Placement
`canSurvive` becomes: standing, or a horizontal neighbour is a rail or an anchor. `BlockItem.canPlace` checks `state.canSurvive` (`BlockItem.java` line 155 in the NeoForge 21.1.255 sources), so a rail in open air with nothing beside it is refused before placement. `neighborChanged` no longer drops the rail; the tick decides.

### Config
`warehouse.json` gets `"rail_max_span": 3`. `StorageStatsManager` reads it with `intField(..., DEFAULT_RAIL_MAX_SPAN)` and exposes `railMaxSpan()`.

### PLAN.md open decisions
Section 2.3 said "flat floor rail ... No slopes, no ceiling rails". This change settles that rails may hang on rack fronts and elevator segments and bridge gaps of up to `rail_max_span`; slopes and ceiling rails stay out. PLAN.md is updated in this change.

## Pure vs Minecraft

- Pure, JUnit: `RailSpan` (classification input, gap walk, span and hold counting, drop set, walk limit).
- Minecraft, GameTest: placement refusal in open air, hung rail on a rack front, three rail bridges between elevators and racks, fourth rail collapse, overhang collapse, anchor removal collapse, row along an upper rack row.
- Manual in game: building a bridge by hand from both ends.

## Risks / Trade-offs

- [A long hung row loses its end rail when a gap beside it collapses] Accepted: the bridge the player built falls with its abutments, as described in the request.
- [Shuttles on a rail that collapses] Covered by the existing rail removal handling; no new code.
- [Branching gaps (a free rail with hung rails on three sides) count every hung rail into the span] Rare; accepted for a simple rule. Revisit if play testing finds it annoying.
- [Holds are counted, not placed: a gap whose two holds sit at the same end (a T off two hung rails) carries a short overhang within the span] Rare and harmless; accepted for a rule without geometry.
- [GameTest placement] `helper.setBlock` skips placement checks; open air refusal is tested through `BlockItem` use with a mock player as in earlier storage tests.
