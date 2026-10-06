# Design

## Context

See proposal.md for motivation. `HeavyRackBlock` recomputes `LEFT` and `RIGHT` from the bay's index in the row with `RackRow.leftPostHidden` and `rightPostHidden`, in `getStateForPlacement`, `updateShape` and a row refresh on place, remove and rotate. The flags drive the multipart: a set flag hides that side's post and adds the shelf and beam extensions.

## Goals / Non-Goals

**Goals:**
- Joins are stable: placing bays never changes existing joins.
- No row walk on every neighbour update; only the neighbour sections are measured once at placement.

**Non-Goals:**
- Re-balancing sections after removals.

## Decisions

### Joins are state, decided at placement
`LEFT` and `RIGHT` now mean "joined to that neighbour" and are persisted like any block state. `getStateForPlacement` measures the section on each side: starting at the neighbour, it walks away from the placed position while the current bay's flag towards the next one is set and the next block is a same facing rack, counting bays. The pure `RackRow.joinLeft(leftSize, max)` is `leftSize > 0 && leftSize < max`; `RackRow.joinRight(leftJoined, leftSize, rightSize, max)` is `rightSize > 0 && (leftJoined ? leftSize + 1 : 1) + rightSize <= max`. `RackRowTest` covers a lone bay, joining a section of two, refusing a full section of three, joining both sides of two single bays, and refusing the right side when left plus right would exceed the cap.

### The row below decides
Before the section rule runs, `joinFromBelow` looks at the rack under the placed bay and, failing that, under the neighbour on that side. If either is a same facing rack, its join flag on that side is the answer (and a neighbour must exist for the join to be set). Only when no rack stands under either does the section rule apply. Rows therefore copy the splits of the row directly below them, all the way down.

### Neighbours mirror the join
A join is mutual, so after the state is placed `onPlace` sets `RIGHT` on the left neighbour and `LEFT` on the right neighbour when the placed state has the matching flag, through `setBlock` with `UPDATE_CLIENTS`. `updateShape` only ever clears a flag: when the neighbour on that side is no longer a same facing rack, the flag goes false. It never sets one, so nothing shifts. The `UP` and `DOWN` flags stay geometric in `updateShape`.

### Rotation and removal
Rotating a bay places its rotated state with both horizontal flags false and lets the neighbours clear their flags through the normal shape update; the row refresh from the previous change is removed. Breaking a bay triggers the neighbours' `updateShape`, which clears their flags towards the gap.

### Data
`rack.json` key `bays_per_section`, `StorageStatsManager.rackBaysPerSection()`, default 3.

## Pure versus Minecraft logic

Pure: `RackRow` with `RackRowTest`. Minecraft: block placement and shape updates, and the GameTest `rackSectionsStayPut`: five racks placed one after another through the item, left to right then one at the far left; it asserts the first three are joined, the fourth and the far left one stand alone with the first three unchanged, and a fifth beside the fourth joins only the fourth.

## Risks / Trade-offs

- [Two single bays with a gap filled later] -> The filler joins both when left plus right plus one fits the cap, else the left side first.
- [World generated or command placed racks] -> `setBlock` without placement logic yields standalone bays, which is acceptable.
