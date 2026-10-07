# Design

## Context

`DuctPartModels` generates `center_both` (bands along Z) and `center_both_x`; `UtilityDuctModel.runAxis` picks X when the horizontal arms lie only east and west, else Z. `skinBox` can paint bands along any axis.

## Goals / Non-Goals

**Goals:**
- Bands only where they can run straight; one plain cube everywhere else.

**Non-Goals:**
- See proposal.

## Decisions

### Settles no PLAN.md open decision

### Parts
`DuctParts.centerName(linesMask, along)` names `center_both_x`, `center_both_y` and, for `along == null`, `center_both_junction`; the Z centre keeps the name `center_both`. The junction part is a cube with every face on a new `duct_junction` texture, a neutral grey between the two line colours. All four two line centres are generated and registered.

### Choice
`UtilityDuctModel.runAxis` collects the axes of every arm: no arm or one axis gives that axis (none means Z), more than one axis gives null and so the junction.

### Minecraft side verification
Client only: launch the client; a straight run in every direction keeps bands, a bend shows the plain cube.

## Risks / Trade-offs

- [Vertical runs now use the Y centre] → bands run up the side faces, which is what a vertical pipe should look like.

## Open Questions

None.
