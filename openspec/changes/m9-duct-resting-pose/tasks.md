# Tasks

## 1. Pose solver (pure Java)

- [x] 1.1 Add `foundation/.../logistics/duct/DuctPose.java` with sides `NEG`/`MID`/`POS` and ranks `ANCHORED`/`DRAPED`/`FREE` per axis, packing into an int, and the base pose stage from design.md D2:
  - floor from a sturdy below that is not an arm side
  - walls only with an up or down arm, ignoring arm sides
  - opposite walls give `MID`
  - any panel makes every axis `MID`, `ANCHORED`

  JUnit in `foundation/src/test/.../logistics/duct/DuctPoseTest.java`: `floorLiesLow`, `noFloorStaysMiddle`, `armIntoBelowIsNotFloor`, `climbHugsWall`, `climbInCornerHugsBoth`, `oppositeWallsStayMiddle`, `horizontalRunIgnoresWall`, `armIntoWallIsNotWall`, `panelForcesMiddle`, `packRoundTrip`.

  Verify: `gradlew :foundation:test` green.
- [x] 1.2 Add the final pose (drape) stage to `DuctPose`:
  - copy the agreeing `ANCHORED` requests of the connected neighbour ducts on each `FREE` axis
  - ignore neighbours across a face on that axis
  - stay `MID` on disagreement

  JUnit in `DuctPoseTest`: `stepEdgeDrapesBothAxes` (the step scenario from the spec), `disagreeingNeighboursStayMiddle`, `drapedNeighbourDoesNotRequest`, `anchoredAxisIsNotDraped`, `longSpanMiddleStaysCentred`.

  Verify: `gradlew :foundation:test` green.
- [x] 1.3 Add the face position stage to `DuctPose`:
  - per arm side and per perpendicular axis, the higher rank wins
  - on a tie, the duct on the negative side wins
  - endpoint arms always `MID`

  JUnit in `DuctPoseTest`: `anchoredBeatsFree`, `drapedBeatsFree`, `tieGoesToNegativeSide`, `bothSidesAgreeOnFace` (computed from either duct), `endpointFaceIsMiddle`.

  Verify: `gradlew :foundation:test` green.

## 2. Pose aware boxes and hits (pure Java)

- [x] 2.1 In `foundation/.../logistics/duct/DuctShapes.java`:
  - add `LIFT` (0.05 px) and `center(radius, pose)`
  - add `arm(side, armRadius, centreRadius, pose, faces)` returning the straight segment, or step box plus segment B, per design.md D3
  - keep the old centred functions as the `MID` case

  JUnit in `DuctShapesPoseTest`: `middlePoseMatchesOldBoxes`, `floorCentreSitsOnLift`, `armStretchesToMovedCentre`, `endpointArmStepsToMiddle`, `stepBoxSpansBothCrossSections`, `twoAxisStepIsOneBox`, `segmentsTouchButDoNotOverlap` (no two boxes share a face plane with overlap), `thickCentreArmStepFitsRoom`.

  Verify: `gradlew :foundation:test` green.
- [x] 2.2 In `foundation/.../logistics/duct/ArmHit.java`, test the pose aware arm box lists, then the pose aware centre. Update `ArmHitTest` and add `hitOnStepCountsAsArm`, `hitOnLowSegmentCountsAsArm`, `hitAtMovedCentreIsCentre`. Verify: `gradlew :foundation:test` green.

## 3. Block entity, block and sync

- [x] 3.1 In `UtilityDuctBlockEntity`:
  - keep the base pose, the final pose and the faces
  - recompute the base pose with `isFaceSturdy` on the six neighbours whenever arms or panels change and in `neighbourChanged()`
  - add `neighbourPoseChanged(stage)` and call it on the connected neighbour ducts when the base or final pose changed, stopping when nothing changed
  - recompute in `onLoad`
  - save and load `Pose` and `PoseFaces`, and send them in the update tag
  - add `POSE` and `FACES` to `DuctModelData` and to `getModelData()`

  Verify: `gradlew build` green.
- [x] 3.2 In `UtilityDuctBlock`:
  - key the shape cache by a record including pose and faces
  - build the shape from the pose aware boxes, panels unchanged
  - pass pose and faces into `hitArm`

  Verify: `gradlew build` and `gradlew :foundation:test` green.
- [x] 3.3 Add GameTests to `foundation/.../logistics/gametest/ServusLogisticsGameTests.java`:
  - `ductOnStoneLiesLow`
  - `ductAboveSlabStaysMiddle`
  - `climbHugsWall`
  - `stepEdgeDrapes`
  - `breakingFloorRecentres` (also the draped neighbour)
  - `panelRecentresDuct`
  - `ductOnFurnaceItFeedsStaysMiddle`

  Each test asserts the block entity's final pose.

  Verify: `gradlew :foundation:runGameTestServer` green.

## 4. Rendering

- [x] 4.1 In `foundation/.../logistics/client/UtilityDuctModel.java`, per design.md D4:
  - translate centre and junction quads by the pose
  - remap arm body quads: stretch on the arm axis, move on the perpendicular axes, rebake UVs from position on plain faces
  - translate ring quads by the face position only
  - cache per part, pose and faces
  - keep the data tier reskin after the transform

  Verify: `gradlew build` green.
- [x] 4.2 Build the step box quads in `UtilityDuctModel` (or a new `StepQuads` beside `PanelQuads`) with `MutableQuad.setCubeFace` and `bakeUvsFromPosition`: the line sprite for one line, `duct_junction` for several. Leave out the faces hidden against the centre and segment B. List the face check from design.md D3 for every box pair in the commit's task note. Verify: `gradlew build` green.

  Face check, every pair of overlapping or touching boxes:
  - centre and straight arm: the arm starts on the centre's face plane, end faces opposite facing; equal side planes only touch along an edge.
  - centre and step: the step's inner face lies on the centre's face plane, opposite facing, and is left out where the centre covers it; side faces are drawn in by the step inset.
  - step and segment B: B starts on the step's outer face, opposite facing; both carry the same inset, so equal side planes only touch along an edge.
  - step and ring: the step ends at least the ring depth before the block edge, so it never shares the ring's outer plane; only a step that fills a gap of at most the ring depth reaches the edge, and it leaves out its outer face.
  - segment B and ring: the ring lip stays on the face span, B is inset by at most 0.015 px: a hairline gap, no shared plane.
  - stepping arms on different axes: insets differ per arm axis, no shared plane where they cross (JUnit `segmentsTouchButDoNotOverlap`).
  - duct and floor or wall: every part keeps `LIFT` (0.05 px) off the block face.
- [x] 4.3 In `foundation/.../logistics/client/DuctOverlayRenderer.java`, draw the load box around `DuctShapes.center(radius, pose)` grown by 0.5 px. Verify: `gradlew build` green.

## 5. Texts

- [x] 5.1 Docs and plan:
  - In `docs/src/content/docs/mods/foundation.md`, describe the resting pose: floor, climbing walls, draping, steps, centred endpoints, centred panelled ducts.
  - In `PLAN.md`, add `m9-duct-resting-pose` to the "Open: Foundation" list. (PLAN.md was groomed into a list of open points after this change was proposed; it has no rendering bullet in 2.2 and no M9 "what is left" list any more.)

  Verify: docs tests green.

## 6. Integration

- [x] 6.1 Full `gradlew build`, unit tests and the foundation GameTests green. The game launches with all three mods.
- [ ] 6.2 Play check in game by the user from main:
  - A floor run lies flat and straight, in the middle across the run, also beside a wall.
  - A climb stays dead centre, beside a wall and in a corner.
  - A floor, climb, step top run: the climb is centred, the duct at the top lies as low as the run on the step, and the run on the step has no step in it.
  - A run off a ledge steps once, up or down only.
  - A duct into a furnace from the floor steps up and its ring is centred.
  - Breaking the floor recentres it.
  - Facade ducts stay centred.
  - Carrot clicks work on the low part and on the step.
  - The goggles overlay sits on the duct.
  - Thin and four line ducts both look right.
  - Nothing flickers on the floor, at steps or at rings, from several angles.

  Verify: the user confirms in game.

## 7. Play test rework: height only

The user's play test rejected the wall stage (design.md D0). Ducts move only down, never on X or Z.

- [x] 7.1 Artifacts: `proposal.md`, `specs/foundation/duct-appearance/spec.md` (wall requirement removed, floor, drape, centred and step wording on height only), `design.md` (D0, D1 to D3 on height only, D4 to D6 and risks), this section and the play check list in 6.2. Verify: `openspec validate m9-duct-resting-pose --strict` valid.
- [ ] 7.2 Pose solver and boxes on height only, pure Java:
  - `DuctPose.java`: the pose is a height (`MID`, `LOW`) and a rank; `base(arms, sturdyBelow, panels)` without the wall stage; draping only from horizontal neighbours; faces one bit per horizontal side.
  - `DuctShapes.java`: only horizontal arms step, and only on Y; the segment always follows the step; `STEP_INSET` goes.
  - `ArmHit.java` keeps its signature.
  - Callers in `UtilityDuctBlockEntity`, `UtilityDuctBlock`, `DuctPoseQuads` and `ServusLogisticsGameTests` adapted to the new API so the build stays green.

  JUnit: `DuctPoseTest` drops `climbHugsWall`, `climbInCornerHugsBoth`, `oppositeWallsStayMiddle`, `horizontalRunIgnoresWall`, `armIntoWallIsNotWall` and adds `footOfClimbLiesLow`, `stepEdgeDrapesHeight`, `verticalNeighbourDoesNotRequest`, `verticalArmsHaveNoFace`; `DuctShapesPoseTest` drops `twoAxisStepIsOneBox`, `flushCentreStepFillsTheGap` and adds `verticalArmNeverSteps`, `segmentAlwaysFollowsStep`; `segmentsTouchButDoNotOverlap` runs every face mask; `ArmHitTest` on the new API.

  Verify: `gradlew build` and `gradlew :foundation:test` green.
- [ ] 7.3 `UtilityDuctBlockEntity`: `sturdyBelow()` replaces the six side sturdy mask; save keys `RestBase`, `Rest`, `RestFaces`. `UtilityDuctBlock`: the shape cache key is a `long` again. Verify: `gradlew build` green.
- [ ] 7.4 `foundation/.../logistics/client/DuctPoseQuads.java`: centre and ring shifted on Y only, cache keyed by height and face height, the step's outer face always drawn. Face check from design.md D3 for every box pair. Verify: `gradlew build` green.
- [ ] 7.5 GameTests in `ServusLogisticsGameTests.java`: `climbHugsWall` becomes `climbInCornerStaysCentred`; `stepEdgeDrapes` checks the centred climb and the draped height at the top; the others assert height and rank. Verify: `gradlew :foundation:runGameTestServer` green.
- [ ] 7.6 Texts: `docs/src/content/docs/mods/foundation.md` (no walls, drape on height only, climbs centred), `PLAN.md` "Open: Foundation" bullet. Verify: docs tests green.
- [ ] 7.7 Full `gradlew build test runGameTestServerAll` green.
