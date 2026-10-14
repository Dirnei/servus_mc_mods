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
- [ ] 1.3 Add the face position stage to `DuctPose`:
  - per arm side and per perpendicular axis, the higher rank wins
  - on a tie, the duct on the negative side wins
  - endpoint arms always `MID`

  JUnit in `DuctPoseTest`: `anchoredBeatsFree`, `drapedBeatsFree`, `tieGoesToNegativeSide`, `bothSidesAgreeOnFace` (computed from either duct), `endpointFaceIsMiddle`.

  Verify: `gradlew :foundation:test` green.

## 2. Pose aware boxes and hits (pure Java)

- [ ] 2.1 In `foundation/.../logistics/duct/DuctShapes.java`:
  - add `LIFT` (0.05 px) and `center(radius, pose)`
  - add `arm(side, armRadius, centreRadius, pose, faces)` returning the straight segment, or step box plus segment B, per design.md D3
  - keep the old centred functions as the `MID` case

  JUnit in `DuctShapesPoseTest`: `middlePoseMatchesOldBoxes`, `floorCentreSitsOnLift`, `armStretchesToMovedCentre`, `endpointArmStepsToMiddle`, `stepBoxSpansBothCrossSections`, `twoAxisStepIsOneBox`, `segmentsTouchButDoNotOverlap` (no two boxes share a face plane with overlap), `thickCentreArmStepFitsRoom`.

  Verify: `gradlew :foundation:test` green.
- [ ] 2.2 In `foundation/.../logistics/duct/ArmHit.java`, test the pose aware arm box lists, then the pose aware centre. Update `ArmHitTest` and add `hitOnStepCountsAsArm`, `hitOnLowSegmentCountsAsArm`, `hitAtMovedCentreIsCentre`. Verify: `gradlew :foundation:test` green.

## 3. Block entity, block and sync

- [ ] 3.1 In `UtilityDuctBlockEntity`:
  - keep the base pose, the final pose and the faces
  - recompute the base pose with `isFaceSturdy` on the six neighbours whenever arms or panels change and in `neighbourChanged()`
  - add `neighbourPoseChanged(stage)` and call it on the connected neighbour ducts when the base or final pose changed, stopping when nothing changed
  - recompute in `onLoad`
  - save and load `Pose` and `PoseFaces`, and send them in the update tag
  - add `POSE` and `FACES` to `DuctModelData` and to `getModelData()`

  Verify: `gradlew build` green.
- [ ] 3.2 In `UtilityDuctBlock`:
  - key the shape cache by a record including pose and faces
  - build the shape from the pose aware boxes, panels unchanged
  - pass pose and faces into `hitArm`

  Verify: `gradlew build` and `gradlew :foundation:test` green.
- [ ] 3.3 Add GameTests to `foundation/.../logistics/gametest/ServusLogisticsGameTests.java`:
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

- [ ] 4.1 In `foundation/.../logistics/client/UtilityDuctModel.java`, per design.md D4:
  - translate centre and junction quads by the pose
  - remap arm body quads: stretch on the arm axis, move on the perpendicular axes, rebake UVs from position on plain faces
  - translate ring quads by the face position only
  - cache per part, pose and faces
  - keep the data tier reskin after the transform

  Verify: `gradlew build` green.
- [ ] 4.2 Build the step box quads in `UtilityDuctModel` (or a new `StepQuads` beside `PanelQuads`) with `MutableQuad.setCubeFace` and `bakeUvsFromPosition`: the line sprite for one line, `duct_junction` for several. Leave out the faces hidden against the centre and segment B. List the face check from design.md D3 for every box pair in the commit's task note. Verify: `gradlew build` green.
- [ ] 4.3 In `foundation/.../logistics/client/DuctOverlayRenderer.java`, draw the load box around `DuctShapes.center(radius, pose)` grown by 0.5 px. Verify: `gradlew build` green.

## 5. Texts

- [ ] 5.1 Docs and plan:
  - In `docs/src/content/docs/mods/foundation.md`, describe the resting pose: floor, climbing walls, draping, steps, centred endpoints, centred panelled ducts.
  - In `PLAN.md`, add the resting pose to the Foundation rendering bullet (2.2) and `m9-duct-resting-pose` to the M9 "what is left" list.

  Verify: docs tests green.

## 6. Integration

- [ ] 6.1 Full `gradlew build`, unit tests and the foundation GameTests green. The game launches with all three mods.
- [ ] 6.2 Play check in game by the user from main:
  - A floor run lies flat and straight.
  - A climb hugs the wall and sits in a corner when there are two walls.
  - A floor, climb, step top run has no steps in it.
  - A run off a ledge steps once.
  - A duct into a furnace from the floor steps up and its ring is centred.
  - Breaking the floor recentres it.
  - Facade ducts stay centred.
  - Carrot clicks work on the low part and on the step.
  - The goggles overlay sits on the duct.
  - Thin and four line ducts both look right.
  - Nothing flickers on the floor, on walls, at steps or at rings, from several angles.

  Verify: the user confirms in game.
