# Design

## Context

See proposal.md for the why and `specs/foundation/duct-appearance/spec.md` for the rules.

Today every duct part is centred on (8, 8, 8):

- `DuctPartModels` (datagen) writes a centre per line set and run axis, a junction cube, and an arm per side and line set. Each arm runs from the centre face to the block edge and ends in a ring whose faces carry tint indices.
- `UtilityDuctModel` (a `DynamicBlockStateModel`) picks parts by the model data from `UtilityDuctBlockEntity.getModelData()` and caches reskinned quads per quad.
- `UtilityDuctBlock.getShape` builds a cached `VoxelShape` from `DuctShapes` boxes keyed by a `long`.
- `ArmHit` finds the clicked arm from the same boxes.
- `DuctOverlayRenderer` draws the goggles load box at a fixed 3.5..12.5.

The block entity already re-evaluates its arms in `neighbourChanged()` (fired from `UtilityDuctBlock.neighborChanged`), saves arms and endpoints, and syncs through `getUpdateTag`/`loadAdditional`. On the client it calls `requestModelDataUpdate()`.

This change settles none of the open decisions in PLAN.md section 5. It is a look follow up under M9.

## Goals / Non-Goals

**Goals:**
- One pure Java pose solver that the server, the shape, the click test and the renderer all share, so what you see is what you click.
- Pose work only on placement, arm, panel and neighbour changes.
- No new datagen models. The existing parts are moved and stretched at runtime, and the step pieces are built in code.

**Non-Goals:**
- Changing the network, connection or endpoint logic. The pose never decides whether an arm exists.
- Poses for anything other than the Utility Duct.

## Decisions

### D1: The pose is a side per axis, not a pixel offset

On each axis a duct is `NEG`, `MID` or `POS`: flush with the low side, centred, or flush with the high side.

- Every box (centre, junction, arm segment) is placed by its own radius. A part of radius `r` on `NEG` spans `LIFT .. LIFT + 2r`, on `POS` it spans `16 - LIFT - 2r .. 16 - LIFT`.
- `LIFT` is 0.05 px, so no face shares a plane with the floor or wall.
- A thick centre and a thin arm of the same duct both lie on the floor. The arm then rests on the floor rather than being coaxial with the centre, and that is what makes it match a thin neighbour duct at the face.

Alternative considered: a pixel offset of the centre. It breaks down with mixed arm thicknesses and needs a per radius table at every face.

Each axis value also carries a **rank**: `ANCHORED` (from floor, wall or panel), `DRAPED` (copied from neighbours) or `FREE`. The pose packs into 6 bits for the sides plus 6 bits for the ranks.

### D2: Three stages, each depending only on direct neighbours

Pure logic in `foundation/.../logistics/duct/DuctPose.java`, no Minecraft imports.

1. **Base pose**, from the duct and its six neighbour blocks.
   - Floor: below is sturdy and is not an arm side. Then Y is `NEG`, `ANCHORED`.
   - Wall: the duct has an up or down arm, and the X or Z neighbour face is sturdy and is not an arm side. Then that axis is `NEG` or `POS`, `ANCHORED`. Walls on both sides of one axis give `MID`, `ANCHORED`.
   - Panel: any panel makes all three axes `MID`, `ANCHORED`.
   - Otherwise the axis is `MID`, `FREE`.
2. **Final pose**, from the base pose and the base poses of the ducts across its duct arms.
   - For each `FREE` axis, collect the requests of neighbours that are `ANCHORED` on that axis and that connect across a face whose arm axis is not that axis.
   - If all requests agree, the duct takes that side as `DRAPED`. If they disagree, it stays `MID`, `FREE`.
3. **Face positions**, per arm side and per perpendicular axis, from the duct's own final pose and the final pose of the duct across that face.
   - The higher rank wins (`ANCHORED` > `DRAPED` > `FREE`).
   - On a tie with different sides, the duct on the negative side of the arm axis wins (down, north, west). Both ducts compute the same answer.
   - Endpoint arms always face `MID` on both perpendicular axes.

Each stage reads only the duct itself and its direct neighbours, which keeps updates local:

- A world neighbour change or an arm or panel change recomputes the base pose.
- A changed base pose recomputes the own final pose and calls `neighbourPoseChanged(BASE)` on the connected neighbour ducts. They recompute their final pose.
- A changed final pose recomputes the own faces and calls `neighbourPoseChanged(FINAL)` on the connected neighbours. They recompute their faces only.
- Every stage stops when nothing changed, so the chain ends after two hops.

All of this runs on the server thread inside `neighborChanged` and the existing arm and panel update paths. There is no per tick scan and no mailbox, because none of it is network state.

Alternative considered: computing the pose on the client from the `BlockAndTintGetter` in `collectParts`. It would avoid saving the pose. But the shape and the carrot hits are needed on the server too, and a block change two away does not reliably rebuild the section. Rejected.

### D3: Arms stretch, steps are built in code

An arm runs from the centre's edge on its axis (which moves with the centre) to the block edge. On its perpendicular axes it sits at the own pose near the centre and at the face position at the face.

- When those differ, the arm becomes segment A, a step box, then segment B.
- The step box sits against the centre, its length along the arm is the arm diameter clamped to the room left, and it spans the union of both cross sections.
- When both perpendicular axes differ, one step box covers both (an elbow fitting look), not two steps.
- Segment A has length zero in that layout, so in practice an arm is step plus B, or one straight segment.

`DuctShapes` gets pose aware box functions returning `List<Box>`: `center(radius, pose)` and `arm(side, armRadius, centreRadius, pose, face)`. The shape, `ArmHit` and the renderer all use them, which settles the "what you see is what you click" goal in one tested place.

Z-fighting check:

- Segment B ends exactly at the step box's face.
- The step box ends exactly at the centre's face.
- Side faces on an axis that does not step are coplanar but only touch along an edge, they do not overlap.
- This is the same contact the arms make with the centre today.
- The floor and wall contact is kept off the plane by `LIFT`.
- Rings keep their 0.5 px lip and only move with the face position.

### D4: Rendering transforms baked quads

`UtilityDuctModel` keeps the datagen parts and changes how it uses them:

- **Centre and junction quads** are translated by the pose with `MutableQuad.setFrom(quad).setPosition(i, ...)`. UVs stay, so the texture moves with the tube.
- **Arm body quads** (tint index < 0) are remapped vertex by vertex:
  - On the arm axis, coordinates at the baked inner end move to the new inner end.
  - On the perpendicular axes, coordinates move by the face position. The pieces near the centre move by the own pose when a step is present, because the arm is then rebuilt as the B segment between step and face.
  - Faces with the plain line texture rebake UVs from position (`MutableQuad.bakeUvsFromPosition()`), so stretching does not smear the texel size.
  - Band faces keep their full face UVs, as datagen already gives them.
- **Ring quads** (tint index >= 0) are translated by the face position only.
- **Step boxes** are built with `MutableQuad.setCubeFace(Direction, from, to)` plus `bakeUvsFromPosition()`. Single line arms use the line's sprite. Multi line arms use the `duct_junction` sprite, the neutral fitting colour the spec already uses where axes meet, so no band orientation is needed. The sprites come from the existing part quads (`materialInfo().sprite()`), which keeps the data tier reskin working because it runs after.
- **Cache**: results per part name and pose and face key, in a `ConcurrentHashMap` like `skinned`. It is cleared on resource reload together with the model.

`DuctModelData` gains `POSE` (int: sides and ranks of the final pose) and `FACES` (int: 2 bits per side per perpendicular axis, 24 bits).

APIs checked in `neoforge-26.1.2.116-sources.jar`: `MutableQuad.setFrom`, `setPosition`, `setCubeFace`, `bakeUvsFromPosition` and `toBakedQuad` exist. The sturdy test uses `BlockStateBase.isFaceSturdy(BlockGetter, BlockPos, Direction)` (`SupportType.FULL`) in `minecraft-patched-26.1.2.116-sources.jar`, `BlockBehaviour.java` line 876. Ducts never pass it: their state shape cache is computed without a block entity, so they are small, which is why "ducts are not floor" needs no special case.

### D5: Save and sync

- The block entity saves the base pose, the final pose and the faces (`Pose`, `PoseFaces` ints) and sends them in the update tag, which already goes through `loadAdditional` and `requestModelDataUpdate()` on the client.
- After a load on the server, the pose is recomputed on the first `neighbourChanged` or in `onLoad` (which already reconciles memberships). The saved value is only a start.

### D6: Shape cache and click test

- The shape cache key becomes a record (lines, arms, panels, pose, faces), since the extra 30 bits no longer fit the `long`.
- `UtilityDuctBlock.hitArm` passes the pose and faces to `ArmHit.hit`, which tests the arm box lists from D3.
- The click order stays the same: arms first, then the centre.

### D7: Goggles overlay

`DuctOverlayRenderer` draws the load box as `DuctShapes.center(radius, pose)` grown by 0.5 px. It reads the pose from the client block entity, which gets the pose from the sync.

### Pure Java vs Minecraft

| Logic | Where | Checked by |
|---|---|---|
| Base pose, drape, face rank, step boxes, arm hit | `DuctPose`, `DuctShapes`, `ArmHit` | JUnit |
| Sturdy tests, neighbour notification, save and sync | `UtilityDuctBlockEntity`, `UtilityDuctBlock` | manual play check |
| Quad transforms, step quads, overlay | client | manual play check from several angles |

## Risks / Trade-offs

- [Stretched arm textures look different from today] → Plain faces rebake UVs from position. Band faces were already full face mapped. Check in the play check.
- [A double step (two axes at once) reads as a fat block] → Rare: it needs two disagreeing anchors at one face. Accepted, can turn into two steps later.
- [Update chain cost when many ducts change at once (explosion, piston, `/fill`)] → Each stage stops when nothing changed and reaches at most two hops. No world scan.
- [Hits and drawing drift apart] → One box source (`DuctShapes`) for shape, hit and step quads, with JUnit tests on the boxes.
- [Hollow panel holes stay centred] → Panelled ducts are forced `MID`, so their arms always pass the hole.
- [Player expectations differ, for example cables along a corridor wall] → Decided in explore: only climbing ducts hug walls. Revisit after the play check.
