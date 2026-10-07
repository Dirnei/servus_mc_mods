# Design

## Context

`DuctPartModels.center` paints the two line centre with `skinBox(..., Axis.Z, ...)`; `skinBox` splits the bands across the axis perpendicular to `along` in the horizontal plane, so a Z and a Y run share one split and an X run needs the other. `UtilityDuctModel.getQuads` picks `DuctParts.centerName(lines)` and the arms from the model data masks.

## Goals / Non-Goals

**Goals:**
- Bands continue through the centre on straight runs.

**Non-Goals:**
- See proposal.

## Decisions

### Settles no PLAN.md open decision

### Parts
`DuctParts.centerName(linesMask, Direction.Axis along)` returns `center_both_x` for the two line set on the X axis and the old names otherwise. `DuctPartModels.generate` builds that extra centre with `along = X`; the client registers it with the other parts.

### Choice
`UtilityDuctModel` merges the arm masks of all lines; with arms on east or west and none on north or south it takes the X centre, otherwise the Z centre, which also serves vertical runs.

### Minecraft side verification
Client only: launch the client and lay a two line duct east to west and north to south.

## Risks / Trade-offs

- [Junctions with arms on both horizontal axes] → one seam remains, as before.

## Open Questions

None.
