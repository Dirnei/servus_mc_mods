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
- [x] 6.2 Play check in game by the user from main:
  - A floor run lies flat and straight, in the middle across the run, also beside a wall.
  - A climb stays dead centre, beside a wall and in a corner.
  - A floor run turning up makes a clean 90 degree bend: the bend lies on the floor with the run, no step before it, and its arm up rises straight from the low centre into the centred duct above, with no gap, offset or flicker where they meet.
  - A floor, climb, step top run: the foot lies low with the floor run, the climb is centred, the duct at the top lies as low as the run on the step, and the run on the step has no step in it.
  - A duct on the floor feeding a machine beside it and rising straight up stays centred and goes straight up; a floor run joining it steps up in its own block, the junction's arms stay straight.
  - A run off a ledge steps once, up or down only, and every height change bends in the middle of a block, never near its edge.
  - A duct into a furnace from the floor steps up and its ring is centred.
  - Breaking the floor recentres it.
  - Facade ducts stay centred.
  - Carrot clicks work on the low part and on the step.
  - The goggles overlay sits on the duct.
  - A floor run that climbs one block, goes one duct through the air and then onto a block meets the run low, with no hump or blob at the top of the climb.
  - Thin and four line ducts both look right.
  - Nothing flickers on the floor, at steps or at rings, from several angles.

  Verify: the user confirms in game.

## 7. Play test rework: height only

The user's play test rejected the wall stage, and asked that a duct rising straight up never lies down first (design.md D0). Ducts move only down, never on X or Z, and a duct with an arm up stays centred.

- [x] 7.1 Artifacts: `proposal.md`, `specs/foundation/duct-appearance/spec.md` (wall requirement removed, floor, drape, centred and step wording on height only, arm up stays centred), `design.md` (D0, D1 to D3 on height only, D4 to D6 and risks), this section and the play check list in 6.2. Verify: `openspec validate m9-duct-resting-pose --strict` valid.
- [x] 7.2 Height only pose across the code. The pose API changes, so every caller moves in this one task:
  - `DuctPose.java`: the pose is a height (`MID`, `LOW`) and a rank; `base(arms, sturdyBelow, panels)` without the wall stage, no floor with an arm up; `resting(base, arms, ductArms, neighbourBase)` drapes only from horizontal neighbours and never with an arm up; faces one bit per horizontal side.
  - `DuctShapes.java`: only horizontal arms step, and only on Y; the segment always follows the step; `STEP_INSET` goes. `ArmHit.java` keeps its signature.
  - `UtilityDuctBlockEntity`: `sturdyBelow()` replaces the six side sturdy mask; save keys `RestBase`, `Rest`, `RestFaces`. `UtilityDuctBlock`: the shape cache key is a `long` again.
  - `foundation/.../logistics/client/DuctPoseQuads.java`: centre and ring shifted on Y only, cache keyed by height and face height, the step's outer face always drawn.
  - GameTests in `ServusLogisticsGameTests.java`: `climbHugsWall` becomes `climbInCornerStaysCentred`; `stepEdgeDrapes` checks the centred foot and climb and the draped height at the top; new `machineOnFloorPipeGoesStraightUp`; the others assert height and rank.

  JUnit: `DuctPoseTest` drops `climbHugsWall`, `climbInCornerHugsBoth`, `oppositeWallsStayMiddle`, `horizontalRunIgnoresWall`, `armIntoWallIsNotWall` and adds `footOfClimbStaysCentred`, `machineOnFloorPipeGoesStraightUp`, `armUpDoesNotDrape`, `armDownOnlyStillLiesLow`, `stepEdgeDrapesHeight`, `verticalNeighbourDoesNotRequest`, `verticalArmsHaveNoFace`; `DuctShapesPoseTest` drops `twoAxisStepIsOneBox`, `flushCentreStepFillsTheGap` and adds `verticalArmNeverSteps`, `segmentAlwaysFollowsStep`; `segmentsTouchButDoNotOverlap` runs every face mask; `ArmHitTest` on the new API.

  Face check, design.md D3, every pair of overlapping or touching boxes: centre and straight arm touch on the centre's face, opposite facing; centre and step: the step's inner face is opposite facing or left out where the centre covers it, the floor or top planes only touch along an edge; step and segment: the segment starts on the step's outer face, opposite facing, equal side planes only touch along an edge; step and ring: the step ends at least the ring depth before the edge; segment and ring: the 0.5 px lip as on a straight arm; steps on two horizontal arms touch at most along an edge; vertical arm and step: opposite facing where they share a plane; duct and floor: `LIFT`.

  Verify: `gradlew build`, `gradlew test` and `gradlew :foundation:runGameTestServer` green.
- [x] 7.3 Texts: `docs/src/content/docs/mods/foundation.md` (no walls, drape on height only, climbs and ducts with an arm up centred), `PLAN.md` "Open: Foundation" bullet. Verify: docs tests green.
- [x] 7.4 Full `gradlew build test runGameTestServerAll` green.

## 8. Bend rework

The second play test showed a floor run turning up stepping up before the bend, because every duct with an arm up stayed centred (design.md D0). Only a duct with an arm up and a horizontal arm into an endpoint stays centred now; every other duct with an arm up follows the floor and drape rules.

- [x] 8.1 Artifacts: `specs/foundation/duct-appearance/spec.md` (floor and drape requirements, scenarios "Floor run turning up", "Climb above a bend", "Machine and floor run on one duct turning up", "Up beside a machine in the air", step scenario), `design.md` (D0, D2, D3 face check), `proposal.md`, this section and the look list in 6.2, `docs/src/content/docs/mods/foundation.md`. Verify: `openspec validate m9-duct-resting-pose --strict` valid, docs tests green.
- [x] 8.2 `DuctPose.java`: `base(arms, endpoints, sturdyBelow, panels)` and `resting` keep a duct centred only when it rises from an endpoint (`risesFromEndpoint`). `UtilityDuctBlockEntity` passes its endpoints. JUnit in `DuctPoseTest`: `footOfClimbStaysCentred` becomes `floorRunTurningUpLiesLow` (and its face to the run is low), `machineOnFloorPipeGoesStraightUp` passes the endpoint, `armUpDoesNotDrape` becomes `armUpDrapes`, new `machineArmAndArmUpStaysMiddle`, `machineArmAndArmUpDoesNotDrape`, `armUpAloneOnFloorLiesLow`, `verticalEndpointDoesNotKeepMiddle`. `DuctShapesPoseTest`: new `lowBendArmUpJoinsCentredArmAbove` (the low bend's arm up and the centred arm down above meet on the block face with the same X and Z span); `segmentsTouchButDoNotOverlap` already checks every arm, the arm up included, at the low poses. GameTests in `ServusLogisticsGameTests.java`: new `floorRunTurningUpHasNoStep` and `floorRunIntoMachineTurningUpStaysMiddle`, `stepEdgeDrapes` expects a low foot, `machineOnFloorPipeGoesStraightUp` unchanged.

  Face check, low bend with an arm up: arm up and centre touch on the centre's top face, opposite facing, equal side planes only along an edge; arm up and horizontal arm touch at most along an edge; no step at the bend next to a low run; arm up and the arm down of the duct above meet on the block face, opposite facing.

  Verify: `gradlew build`, `gradlew test` and `gradlew :foundation:runGameTestServer` green.
- [x] 8.3 Full `gradlew build test runGameTestServerAll` green.

## 9. Placement flash

- [x] 9.1 Play test: a freshly placed Fluid Pipe showed an orange Item Pipe cube for a frame, because `UtilityDuctModel` drew its base model until the block entity's lines reached the client. Draw nothing while the lines are unknown (`foundation/.../logistics/client/UtilityDuctModel.java`). Verify: `gradlew build test runGameTestServerAll` green.
- [x] 9.2 Play test again: the flash stayed, because the client's new block entity has an empty line set, not an unknown one, and `DuctParts.normalize` turns an empty set into the Item Pipe. Draw nothing while the set is unknown or empty (`UtilityDuctModel`). Verify: `gradlew build test runGameTestServerAll` green.

## 10. Climb end follows a draped run

- [x] 10.1 Play test: a floor run climbing one block, one duct through the air and then onto a block made a hump and a thick blob at the top of the climb, because only the duct in the air draped. `DuctPose`: rank `FOLLOWED`; `resting(base, arms, ductArms, neighbourBase, neighbourResting)` lets a free duct with a vertical duct arm follow agreeing draped neighbours beside it. `UtilityDuctBlockEntity`: passes the neighbours' resting poses and makes neighbours recompute their resting pose on any pose change. Spec scenario "Climb, one duct in the air, then onto a block", design D2, docs. JUnit in `DuctPoseTest`: `climbTopFollowsDrapedRun`, `climbBottomFollowsDrapedRun`, `runWithoutClimbDoesNotFollow`, `followedIsNotFollowed`, `anchoredNeighbourBeatsDrapedOne`, `disagreeingDrapedNeighboursStayMiddle`, `machineClimbDoesNotFollow`, `bothSidesAgreeOnFace` with a followed pose. GameTest `climbThroughAirOntoBlockLiesLow`. Face check: no new boxes; the followed climb end is a low duct with an arm down or up, already checked in 8.2. Verify: `gradlew build test runGameTestServerAll` green, `openspec validate m9-duct-resting-pose --strict` valid, docs tests green.

## 11. Junction at a machine keeps straight arms

- [x] 11.1 Play test: the step from a floor run up into a centred junction (arm up and arm into a machine) sat inside the junction and looked cramped. `DuctPose`: rank `HELD` for a duct that rises from an endpoint, beating every other rank at a face, so the junction's arms stay straight and the neighbour steps in its own block; the rank takes 3 bits. Spec ("Arms step between positions", scenario "Machine and floor run on one duct turning up"), design D2. JUnit in `DuctPoseTest`: `machineOnFloorPipeGoesStraightUp` and `machineArmAndArmUpStaysMiddle` expect `HELD` and straight arms, new `heldBeatsEveryOtherRank`, `heldNeighbourDoesNotRequest`, `bothSidesAgreeOnFace` with a held pose. GameTests `machineOnFloorPipeGoesStraightUp`, `floorRunIntoMachineTurningUpStaysMiddle`. Face check: no new boxes; the neighbour's step is the step of a low duct meeting a middle face, as checked in 7.2. Verify: `gradlew build test runGameTestServerAll` green, `openspec validate m9-duct-resting-pose --strict` valid.

## 12. Bend in the middle of the block

- [x] 12.1 Play test: a height change bent next to the block edge (the step box ran from the centre's face toward the edge) and looked kinked. The user wants the 90 degree bend in the middle of the block, not rounded. `DuctShapes`: `center(radius, pose, faces, armRadius)` grows on Y to reach every horizontal arm's face height, `arm(side, armRadius, centre, faces)` is one straight box, new `armRadii(armMasks)`; the step boxes and `ArmBoxes` go. Callers: `UtilityDuctBlock` (shape and click), `ArmHit`, `DuctOverlayRenderer`, `UtilityDuctModel` and `DuctPoseQuads` (centre fitted onto the grown box with UVs from position, no step quads). Spec ("Arms step between positions", "Endpoint arms end centred", "Outline and clicks follow the pose"), design D3 and D4, docs. JUnit: `DuctShapesPoseTest` rewritten (`bendHappensInTheMiddle`, `centreDropsToALowFace`, `armsFollowTheLowCentre`, `verticalArmsStartAtTheCentre`, `armRadiiCountTheLinesPerSide`, `lowBendArmUpJoinsCentredArmAbove`, `segmentsTouchButDoNotOverlap` on the grown centre); `ArmHitTest` (`hitOnRaisedArmCountsAsArm`, `hitOnGrownCentreIsCentre`). Face check in design D3. Verify: `gradlew build test runGameTestServerAll` green, `openspec validate m9-duct-resting-pose --strict` valid, docs tests green.
- [x] 12.2 Play test: on a grown centre of a multi line duct the bands were swapped on the north and east faces, because the centre took the UVs of every face from the position, which Minecraft mirrors on those faces. `DuctPoseQuads`: like an arm, only plain line faces take their UVs from the position; band faces keep theirs. Design D4. Verify: `gradlew build` green, bands in order from every side in game.
