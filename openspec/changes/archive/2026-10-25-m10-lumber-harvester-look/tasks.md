# Tasks

## 1. Preconditions

- [x] 1.1 Check that `m10-lumber` and `m10-lumber-felling-look` are archived (`openspec/specs/shuttles/lumber/spec.md` holds "Felling arm"). Check: `openspec validate m10-lumber-harvester-look --strict`.

## 2. Pure timeline and pose

- [x] 2.1 `shuttles/.../field/HarvesterTimeline`: the shares of `UNFOLD`, `SAW`, `TOPPLE`, `FOLD` from the design, `FOLD` as `UNFOLD` reversed. JUnit `HarvesterTimelineTest`: `sharesCoverEachPhaseInOrder`, `foldMirrorsUnfold`, `sawEndsWithBarOut`, `toppleStartsLettingGo`. Check: `gradlew :shuttles:test`.
- [x] 2.2 `shuttles/.../field/HarvesterPose` replacing `field/ArmPose` (delete it and `ArmPoseTest`): doors, walls, pillar lift, two link crane with the telescope, wrist, head yaw, tilt, roll and scale, arm angles, saw swing. JUnit `HarvesterPoseTest`: `gripCentreOnTrunkAxisForOneToFourPlots`, `stowedPartsInsideHold`, `restHasDoorsShutAndWallsUp`, `twoByTwoGripsNearestBaseLog`, `noNaNOverSweep`. Check: `gradlew :shuttles:test`.

## 3. Server phases

- [x] 3.1 `ShuttleView.Felling.Phase` gets `UNFOLD` and `FOLD`; `HarvestRun` gets `unfolded` and the after-fold action; `FieldRuns` unfolds before the first felling of a stop and folds before `leaveRail`, the full cargo turn and the run's end; `unfold_ticks` in `stats/ShuttlesStatsManager` and `resources/data/servus_shuttles/shuttles/field.json` (30). Check: `gradlew :shuttles:build`.
- [x] 3.2 `gametest/LumberGameTests`: a stop with two oaks unfolds once and does not fold between them; the shuttle folds before driving to the next rail and before unloading a full cargo; a stop that only plants does not unfold; the phases take `unfold_ticks`; a save during unfolding restores folded. Adjust the existing felling tests for the extra ticks. Check: `gradlew :shuttles:runGameTestServer`.

## 4. Models

- [x] 4.1 `datagen/ShuttleModels`: `lumber_hold_mk1..3` and `lumber_body_mk1..3`, the moving `lumber_side_mk*`, `lumber_end_mk*`, `lumber_door_mk*`, the crane parts and the head parts from the design, `drive_3_lumber` (spoiler on its end plates reaching to the chassis sides, no middle struts); `tool_lumber` empty; `lumberRig` and its parts removed. List every overlapping pair at rest, folded and gripping with whether faces share a plane and fix each. Check: `gradlew :shuttles:runData`, `gradlew :shuttles:build`.

## 5. Client

- [x] 5.1 `client/ShuttleItemModel`: with the Lumber tool use `lumber_hold_mk<tier>`, no `handling_<step>`, `drive_3_lumber` for the last Speed step; `ShuttleLook.openHold` false for Lumber shuttles. Check: `gradlew :shuttles:build`.
- [x] 5.2 `client/FellingArmLook` renamed `HarvesterLook`: register the standalone parts, draw `lumber_body_mk<tier>` and every part posed from `HarvesterPose` in all four phases, mirrored parts by negative x scale (check the faces still show from outside), sawdust while the bar is out; `ShuttleGarageRenderer` calls it for every felling phase. Check: `gradlew :shuttles:build`.

- [x] 5.3 Chainsaw sounds: `registry/ServusShuttlesSounds` (idle, cut, stop), `datagen/ServusShuttlesSoundsProvider` and subtitles in the language provider, the Ogg files under `assets/servus_shuttles/sounds/`, the pure `field/ChainsawCue` with JUnit `ChainsawCueTest`, `client/ChainsawSounds` played from `ShuttleGarageRenderer` at the saw. Check: `gradlew :shuttles:runData`, `gradlew :shuttles:test`, `gradlew :shuttles:build`.
- [x] 5.4 Play check fixes: the idle loops (`chainsaw_idle_loop.ogg`) from the crane's rise until folding and resumes after the cut and wind-down; hold and crane sounds for unfolding and folding (`hold.*`, `crane.*` in `registry/ServusShuttlesSounds`, `datagen/ServusShuttlesSoundsProvider`, subtitles); the pure `field/HarvesterSound` replaces `ChainsawCue` with JUnit `HarvesterSoundTest`: `idlesFromTheRiseUntilItFolds`, `unfoldingOpensLandsAndRises`, `cutsAndStopsWithTheTree`, `foldingSinksRaisesAndShuts`, `onlyTheChainsawCuesArePerTree`; `client/HarvesterSounds` replaces `ChainsawSounds`. Check: `gradlew :shuttles:runData`, `gradlew :shuttles:test`, `gradlew build`.

## 6. Plan and integration

- [x] 6.1 At archive: add the open point "Garage doorway lower than the spoiler and the loading arm" under Shuttles in `PLAN.md`, a line under `## Unreleased` in `shuttles/CHANGELOG.md` (Changed: the Lumber shuttle unfolds a harvester crane out of its hold to fell trees). Check: `openspec validate m10-lumber-harvester-look --strict`.
- [x] 6.2 Full `gradlew build`, `gradlew test`, `gradlew :shuttles:runGameTestServer` green, the game launches with all mods; then the user's play check from main: Mk1, Mk2 and Mk3 Lumber shuttles at rest and through the garage doorway, unfold, grip and cut an oak one and two plots out, a 2x2 dark oak, let go, wait, fold and drive on; no flicker on any part at rest, folded, gripping and while moving.
