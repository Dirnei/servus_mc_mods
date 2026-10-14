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
- A duct with an arm up and a horizontal arm into an endpoint (an inventory or machine, not a duct) never lies on the floor and never drapes. The user saw a duct on the floor feeding a machine and rising straight up lie down first; it should go straight up.
- Every other duct with an arm up follows the normal floor and drape rules. A first cut kept every duct with an arm up centred, and the second play test showed what that does to a plain bend: the floor run stepped up into the centred bend, so the run went up, straight, then up again. Now the bend lies low with the run, and its arm up rises from the low centre: a clean 90 degree bend.
- The duct above the bend has arms up and down and no floor (a duct below is never floor, and an arm down rules a floor out anyway). It has no horizontal duct neighbour to drape from, so it stays centred. The vertical arm between the two needs no step: the bend's arm up runs from its low centre's top to the block's top edge, the arm down above runs from the block's bottom edge to the centred centre, both centred on X and Z and as thick as the lines on that side, so they meet on the shared face as one straight arm with no gap and no offset.
- A duct with only an arm down (the top of a climb turning onto a floor or step) still lies low or drapes.
- Everything else stays as built: centred cases, steps, centred endpoints, pose updates, outline, collision, carrot hits and goggles.

With only one axis able to move, the model below drops what existed for X and Z: the pose is one height instead of a side per axis, a face holds one bit per side, only horizontal arms can step and only up or down, the step inset per arm axis is gone, and the shape cache key fits a `long` again.

### D1: The pose is a height, not a pixel offset

A duct is `MID` or `LOW` in its block: centred, or flush with the floor. On X and Z it is always centred.

- Every box (centre, junction, arm segment) is placed by its own radius. A part of radius `r` that is `LOW` spans `LIFT .. LIFT + 2r` on Y.
- `LIFT` is 0.05 px, so no face shares a plane with the floor.
- A thick centre and a thin arm of the same duct both lie on the floor. The arm then rests on the floor rather than being coaxial with the centre, and that is what makes it match a thin neighbour duct at the face.

Alternative considered: a pixel offset of the centre. It breaks down with mixed arm thicknesses and needs a per radius table at every face.

The height also carries a **rank**: `HELD` (rises from an endpoint), `ANCHORED` (from floor or panel), `DRAPED` (copied from neighbours), `FOLLOWED` (a climb end copied from a draped neighbour) or `FREE`. The pose packs into 1 bit for the height and 3 bits for the rank.

### D2: Three stages, each depending only on direct neighbours

Pure logic in `foundation/.../logistics/duct/DuctPose.java`, no Minecraft imports.

1. **Base pose**, from the duct, its arms, its endpoint arms and the block below (`base(arms, endpoints, sturdyBelow, panels)`).
   - Panel: any panel makes it `MID`, `ANCHORED`.
   - Floor: below is sturdy, is not an arm side, and the duct does not rise from an endpoint (an arm up plus a horizontal arm into an endpoint, `risesFromEndpoint`). Then it is `LOW`, `ANCHORED`.
   - Otherwise it is `MID`, `FREE`.
2. **Resting pose**, from the base pose, the duct's arms and the base poses of the ducts across its horizontal duct arms.
   - A duct that rises from an endpoint keeps its base pose. Its endpoint arms are its arms that lead to no duct.
   - Otherwise, when `FREE`, collect the heights of horizontal neighbours that are `ANCHORED`. Ducts above or below do not ask, because their height says nothing about where a horizontal run lies.
   - If all requests agree, the duct takes that height as `DRAPED`. If they disagree, it stays `MID`, `FREE`.
   - Without an anchored request, a duct with a vertical arm to a duct (the top or bottom of a climb) collects the heights of the horizontal neighbours whose **resting** pose is `DRAPED`, and takes an agreed height as `FOLLOWED`. The third play test showed why: a climb, one duct in the air, then a duct on a block. The duct in the air draped low from the one on the block, but the top of the climb stayed in the middle, so its arm went over and then down inside its own block: a hump and a thick blob. Following one more hop only at a climb end keeps a long span in the air centred. No duct reads a `FOLLOWED` pose, so no two ducts can hold each other low. Because the resting pose now depends on neighbours' resting poses, any change of a base or resting pose makes the neighbours recompute their resting pose and faces; it stops where nothing changes.
3. **Faces**, per horizontal arm side, from the duct's own resting pose and the resting pose of the duct across that face.
   - The higher rank wins (`HELD` > `ANCHORED` > `DRAPED` > `FOLLOWED` > `FREE`). `HELD` is the base of a duct that rises from an endpoint: the fourth play test found the step inside such a junction too cramped, so its arms stay straight and the neighbours beside it step in their own blocks. No duct drapes from a `HELD` one.
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

### D3: Straight arms, the centre grows to bend in the middle

An arm runs straight from the centre box's face on its axis to the block edge. A vertical arm is centred; a horizontal arm sits at the height where it meets its face.

- The centre box is centred on X and Z and starts at the duct's own height. On Y it grows up or down until it spans every horizontal arm at its face height (`center(radius, pose, faces, armRadius)`). So where a duct and its arm's face sit at different heights, the vertical part of the bend is the centre itself, in the middle of the block.
- The first cut built a step box against the centre and a segment from there to the edge. The fifth play test found the bend sat near the block edge (a thin duct's step ran from 10 to 14 px) and looked kinked; the user wants the 90 degree bend in the middle of the block, without rounding. Growing the centre puts it there and drops the step boxes and their sprite choice.
- A vertical arm starts on the grown centre's top or bottom.

`DuctShapes` has pose aware box functions: `center(radius, pose, faces, armRadius)`, `arm(side, armRadius, centre, faces)` and `armRadii(armMasks)`. The shape, `ArmHit`, the overlay and the renderer all use them, which settles the "what you see is what you click" goal in one tested place.

Z-fighting check:

- An arm starts exactly on the centre's face on its axis, opposite facing. Its side, top and bottom faces lie outside the centre on that axis, so where they share a plane with a centre face (equal radii, or the arm at the centre's top or bottom height) the two only touch along an edge.
- Arms on two horizontal axes lie outside the centre on their own axes and within the centre's width on the other, so they never overlap; they touch at most along an edge.
- A vertical arm starts on the centre's top or bottom face, opposite facing, within the centre's width.
- Two vertical arms of stacked ducts meet on the shared block face, opposite facing, with equal X and Z spans, so they read as one arm.
- The ring keeps its 0.5 px lip around the arm at the block edge, as on a straight arm.
- The floor contact is kept off the plane by `LIFT`: the centre never grows below the duct's own height, which is the lowest height any arm meets.

JUnit `segmentsTouchButDoNotOverlap` checks every pose, face mask and radius pair for same facing faces in one plane.

### D4: Rendering transforms baked quads

`UtilityDuctModel` keeps the datagen parts and changes how it uses them:

- **Centre and junction quads** are fitted onto the centre box like an arm onto its box. As on an arm, plain line faces take their UVs from the position and band faces keep theirs, stretched with the box: taking a band face's UVs from the position mirrors them on the north and east faces, which swapped red and white there (sixth play test).
- **Arm body quads** (tint index < 0) are remapped vertex by vertex onto the segment box from D3:
  - On the arm axis, coordinates at the baked inner end move to the new inner end (the centre's face).
  - On Y, a horizontal arm's coordinates move to the face height.
  - Faces with the plain line texture rebake UVs from position (`MutableQuad.bakeUvsFromPosition()`), so stretching does not smear the texel size.
  - Band faces keep their full face UVs, as datagen already gives them.
- **Ring quads** (tint index >= 0) are translated by the face height only.
- **Cache**: results per part name and box, in a `ConcurrentHashMap` like `skinned`. It is cleared on resource reload together with the model.

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
| Base pose, drape, face rank, centre and arm boxes, arm hit | `DuctPose`, `DuctShapes`, `ArmHit` | JUnit |
| Sturdy tests, neighbour notification, save and sync | `UtilityDuctBlockEntity`, `UtilityDuctBlock` | manual play check |
| Quad transforms, step quads, overlay | client | manual play check from several angles |

## Risks / Trade-offs

- [Stretched arm textures look different from today] → Plain faces rebake UVs from position. Band faces were already full face mapped. Check in the play check.
- [Update chain cost when many ducts change at once (explosion, piston, `/fill`)] → Each stage stops when nothing changed and reaches at most two hops. No world scan.
- [Hits and drawing drift apart] → One box source (`DuctShapes`) for shape, hit and step quads, with JUnit tests on the boxes.
- [Hollow panel holes stay centred] → Panelled ducts are forced `MID`, so their arms always pass the hole.
- [Player expectations differ, for example cables along a corridor wall] → The play test rejected wall hugging (D0). Ducts only move down.
