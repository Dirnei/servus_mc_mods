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

### D0: Play test: height only, no walls

The first build also had a wall stage: a duct with an arm up or down moved against a sturdy block beside it, into the corner with two walls, and draping copied X and Z sides too. The user's play test rejected it: hugging a wall only looked right where the run went on straight, it looked wrong wherever the run met a machine or another connection, and corners looked odd. Decision:

- The wall stage is gone. No duct ever moves on X or Z; vertical runs are dead centre.
- The floor stage and draping only set the height (Y).
- Everything else stays as built: centred cases, steps, centred endpoints, pose updates, outline, collision, carrot hits and goggles.

With only one axis able to move, the model below drops what existed for X and Z: the pose is one height instead of a side per axis, a face holds one bit per side, only horizontal arms can step and only up or down, the step inset per arm axis is gone, and the shape cache key fits a `long` again.

### D1: The pose is a height, not a pixel offset

A duct is `MID` or `LOW` in its block: centred, or flush with the floor. On X and Z it is always centred.

- Every box (centre, junction, arm segment) is placed by its own radius. A part of radius `r` that is `LOW` spans `LIFT .. LIFT + 2r` on Y.
- `LIFT` is 0.05 px, so no face shares a plane with the floor.
- A thick centre and a thin arm of the same duct both lie on the floor. The arm then rests on the floor rather than being coaxial with the centre, and that is what makes it match a thin neighbour duct at the face.

Alternative considered: a pixel offset of the centre. It breaks down with mixed arm thicknesses and needs a per radius table at every face.

The height also carries a **rank**: `ANCHORED` (from floor or panel), `DRAPED` (copied from neighbours) or `FREE`. The pose packs into 2 bits for the height plus 2 bits for the rank.

### D2: Three stages, each depending only on direct neighbours

Pure logic in `foundation/.../logistics/duct/DuctPose.java`, no Minecraft imports.

1. **Base pose**, from the duct, its arms and the block below.
   - Panel: any panel makes it `MID`, `ANCHORED`.
   - Floor: below is sturdy and is not an arm side. Then it is `LOW`, `ANCHORED`.
   - Otherwise it is `MID`, `FREE`.
2. **Resting pose**, from the base pose and the base poses of the ducts across its horizontal duct arms.
   - When `FREE`, collect the heights of horizontal neighbours that are `ANCHORED`. Ducts above or below do not ask, because their height says nothing about where a horizontal run lies.
   - If all requests agree, the duct takes that height as `DRAPED`. If they disagree, it stays `MID`, `FREE`.
3. **Faces**, per horizontal arm side, from the duct's own resting pose and the resting pose of the duct across that face.
   - The higher rank wins (`ANCHORED` > `DRAPED` > `FREE`).
   - On a tie with different heights, the duct on the negative side of the arm axis wins (north, west). Both ducts compute the same answer.
   - Endpoint arms always meet their face `MID`.
   - Vertical arms have no face height: across them a duct is always centred.

Each stage reads only the duct itself and its direct neighbours, which keeps updates local:

- A world neighbour change or an arm or panel change recomputes the base pose.
- A changed base pose recomputes the own resting pose and calls `neighbourPoseChanged(BASE)` on the connected neighbour ducts. They recompute their resting pose.
- A changed resting pose recomputes the own faces and calls `neighbourPoseChanged(RESTING)` on the connected neighbours. They recompute their faces only.
- Every stage stops when nothing changed, so the chain ends after two hops.

All of this runs on the server thread inside `neighborChanged` and the existing arm and panel update paths. There is no per tick scan and no mailbox, because none of it is network state.

Alternative considered: computing the pose on the client from the `BlockAndTintGetter` in `collectParts`. It would avoid saving the pose. But the shape and the carrot hits are needed on the server too, and a block change two away does not reliably rebuild the section. Rejected.

### D3: Arms stretch, steps are built in code

An arm runs from the centre's edge on its axis (which moves with the centre for a vertical arm) to the block edge. A vertical arm is always straight and centred. A horizontal arm sits at the own height near the centre and at the face height at the face.

- When those differ, the arm becomes a step box, then a segment at the face height.
- The step box sits against the centre, its length along the arm is the arm diameter clamped to the room left minus the ring depth (1 px), and it spans both heights. Keeping the ring depth free means the step never reaches the block edge where an endpoint ring lies. A horizontal arm always has at least 3 px of room (the centre is centred on X and Z and at most 5 px in radius), so the segment always follows.
- Across the arm on the other horizontal axis, step and segment are as wide as the arm, like a straight arm.

`DuctShapes` has pose aware box functions: `center(radius, pose)`, `armBoxes(side, armRadius, centreRadius, pose, faces)` (step or none, plus the segment) and `arm(...)` as a list. The shape, `ArmHit` and the renderer all use them, which settles the "what you see is what you click" goal in one tested place.

Z-fighting check:

- The segment starts exactly at the step's outer face, opposite facing. Its top or bottom face and its side faces lie in planes of the step's faces, but the two only touch along an edge.
- The step ends exactly at the centre's face. Its face toward the centre is left out when the centre covers it; otherwise it stays, opposite facing to the centre's face. Its floor face or top face may share a plane with the centre's, again touching only along an edge.
- The step ends at least the ring depth before the block edge, so it never reaches the ring's outer face. The ring keeps its 0.5 px lip around the segment, exactly like on a straight arm.
- Steps on two horizontal arms of one duct never cross: a step lies outside the centre on its own axis and within the centre's width on the other, so two steps touch at most along an edge.
- A vertical arm lies within the centre's width on X and Z, outside the steps. Where a step's face toward the centre lies in the plane of a vertical arm's side, the two faces point opposite ways.
- The floor contact is kept off the plane by `LIFT`.

JUnit `segmentsTouchButDoNotOverlap` checks every pose, face mask and radius pair for same facing faces in one plane.

### D4: Rendering transforms baked quads

`UtilityDuctModel` keeps the datagen parts and changes how it uses them:

- **Centre and junction quads** are translated by the pose with `MutableQuad.setFrom(quad).setPosition(i, ...)`. UVs stay, so the texture moves with the tube.
- **Arm body quads** (tint index < 0) are remapped vertex by vertex onto the segment box from D3:
  - On the arm axis, coordinates at the baked inner end move to the new inner end (the centre's face, or the step's outer face).
  - On Y, a horizontal arm's coordinates move to the face height.
  - Faces with the plain line texture rebake UVs from position (`MutableQuad.bakeUvsFromPosition()`), so stretching does not smear the texel size.
  - Band faces keep their full face UVs, as datagen already gives them.
- **Ring quads** (tint index >= 0) are translated by the face height only.
- **Step boxes** are built with `MutableQuad.setCubeFace(Direction, from, to)` plus `bakeUvsFromPosition()`. Single line arms use the line's sprite. Multi line arms use the `duct_junction` sprite, the neutral fitting colour the spec already uses where axes meet, so no band orientation is needed. The sprites come from the existing part quads (`materialInfo().sprite()`), which keeps the data tier reskin working because it runs after.
- **Cache**: results per part name, height and face height, in a `ConcurrentHashMap` like `skinned`. It is cleared on resource reload together with the model.

`DuctModelData` gains `POSE` (int: height and rank of the resting pose) and `FACES` (int: one bit per side whose arm meets its face low).

APIs checked in `neoforge-26.1.2.116-sources.jar`: `MutableQuad.setFrom`, `setPosition`, `setCubeFace`, `bakeUvsFromPosition` and `toBakedQuad` exist. The sturdy test uses `BlockStateBase.isFaceSturdy(BlockGetter, BlockPos, Direction)` (`SupportType.FULL`) in `minecraft-patched-26.1.2.116-sources.jar`, `BlockBehaviour.java` line 876. Ducts never pass it: their state shape cache is computed without a block entity, so they are small, which is why "ducts are not floor" needs no special case.

### D5: Save and sync

- The block entity saves the base pose, the resting pose and the faces (`RestBase`, `Rest`, `RestFaces` ints) and sends them in the update tag, which already goes through `loadAdditional` and `requestModelDataUpdate()` on the client.
- After a load on the server, the pose is recomputed on the first `neighbourChanged` or in `onLoad` (which already reconciles memberships). The saved value is only a start.

### D6: Shape cache and click test

- The shape cache key stays a `long`: lines, arms and panels as before, plus the height bit and the six face bits.
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
- [Update chain cost when many ducts change at once (explosion, piston, `/fill`)] → Each stage stops when nothing changed and reaches at most two hops. No world scan.
- [Hits and drawing drift apart] → One box source (`DuctShapes`) for shape, hit and step quads, with JUnit tests on the boxes.
- [Hollow panel holes stay centred] → Panelled ducts are forced `MID`, so their arms always pass the hole.
- [Player expectations differ, for example cables along a corridor wall] → The play test rejected wall hugging (D0). Ducts only move down.
