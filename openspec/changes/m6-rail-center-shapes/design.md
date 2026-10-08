# Design

## Context

`RailModels.rail` builds a multipart blockstate: one `rail_center` plate (5..11) with no conditions, one `rail_arm` per connected side unless that side docks, and a `rail_dock` per dock. Faces use automatic UVs, so the centre always shows the middle of `rail.png`, whose lines run north to south.

## Goals / Non-Goals

**Goals:**
- No crossing on a straight east west run, curves on corners, a `#` on T and cross junctions.

**Non-Goals:**
- See proposal.

## Decisions

### Settles no PLAN.md open decision

### Centre by multipart condition
`ConnectableBlock.NORTH..WEST` are true for rails, segments, docks and garage backs alike. A side carries rail when its flag is true and its `DOCK_*` flag is false; docks are drawn on their own and do not shape the centre. The builder emits sixteen centre parts, one per rail mask, each an `AND` of four nested condition groups (`MultiPartBlockStateBuilder.PartBuilder.nestedGroup`): a rail side is `side=true, dock_side=false`, a free side is the `OR` of `side=false` and `dock_side=true`. Exactly one part applies. No new block state property.

| Rail sides | Centre |
|---|---|
| none, N, S, N+S | `rail_center` |
| E, W, E+W | `rail_center` rotated y 90 |
| N+E, E+S, S+W, W+N | `rail_center_curve` rotated 0, 90, 180, 270 |
| three or four | `rail_center_cross` |

### Shared rule
The table lives in pure `RailCenter.of(north, east, south, west)` (shape and clockwise rotation) in `storage.rail`, covered by `RailCenterTest`. Datagen picks model and rotation from it.

### Elevator rails
`ElevatorRenderer` draws rail pieces itself. Each piece carries its shape and rotation: the centre from `RailCenter`, arms straight with rotation 0 (north, south) or 90 (west, east). The top face maps the texture by position through the same turn a rotated block model applies (90: u = z, v = 16 - x), so pieces cut at the car and deck edges still line up. On the ground floor the stubs outside the posts (2..14) are always drawn and the part between the posts only while the car is away; before, the whole ground rail disappeared under the parked car and left the edge strips empty.

### Textures
`rail_curve.png` draws a north to east curve inside the centre square (pixels 5..10): the outer line from x 6 on the north edge to z 9 on the east edge, the inner line from x 9 to z 6. `rail_cross.png` draws lines at x 6 and 9 and at z 6 and 9. Model rotation without uvlock turns the texture with the plate.

### Minecraft side verification
Client only: lay an east west run, an L corner, a T and a cross.

## Risks / Trade-offs

- [Pixel curve on a 6 by 6 square looks steppy] → acceptable for a floor rail at this scale.

## Open Questions

None.
