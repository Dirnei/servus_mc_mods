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
`ConnectableBlock.NORTH..WEST` are true for rails, segments, docks and garage backs alike, so the centre is picked from those four flags alone. The builder emits sixteen centre parts, one per combination, each with all four conditions so exactly one applies. No new block state property.

| Connections | Centre |
|---|---|
| none, N, S, N+S | `rail_center` |
| E, W, E+W | `rail_center` rotated y 90 |
| N+E, E+S, S+W, W+N | `rail_center_curve` rotated 0, 90, 180, 270 |
| three or four | `rail_center_cross` |

### Textures
`rail_curve.png` draws a north to east curve inside the centre square (pixels 5..10): the outer line from x 6 on the north edge to z 9 on the east edge, the inner line from x 9 to z 6. `rail_cross.png` draws lines at x 6 and 9 and at z 6 and 9. Model rotation without uvlock turns the texture with the plate.

### Minecraft side verification
Client only: lay an east west run, an L corner, a T and a cross.

## Risks / Trade-offs

- [Pixel curve on a 6 by 6 square looks steppy] → acceptable for a floor rail at this scale.

## Open Questions

None.
