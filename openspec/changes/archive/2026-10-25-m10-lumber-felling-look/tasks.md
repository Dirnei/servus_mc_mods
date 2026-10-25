## 1. Precondition

- [x] 1.1 Check that `m10-lumber` is implemented on main and archived, so `openspec/specs/shuttles/lumber/spec.md` exists with "Tree field run" and "Drops of a felled tree"; if not, stop. Check: `openspec list --specs`, `gradlew :shuttles:test`.

## 2. Pure logic

- [x] 2.1 `field/FellDirection`, `field/ToppleCurve`, `field/ArmPose`. Add `FellDirectionTest` (both sides of a north to south rail, an east to west rail, a corner plot, a tie), `ToppleCurveTest` (0, a quarter, half, 1, beyond 1), `ArmPoseTest` (folded at 0, head at the trunk at 1 for distances 1 to 4, unreachable distance clamps). Check: `gradlew :shuttles:test`.

## 3. Server phases and payload

- [x] 3.1 `shuttle/HarvestRun` felling state, `shuttle/FieldRuns` saw and topple phases (walk again at the saw's end, fold at once when it fails, plant after topple), `topple_ticks` in `stats/ShuttlesStatsManager` and `resources/data/servus_shuttles/shuttles/field.json`. Check: `gradlew :shuttles:build`.
- [x] 3.2 `shuttle/ShuttleView.Felling` with its stream codec; `field/TreeFallPayload` (offsets, states by `Block.BLOCK_STATE_REGISTRY`, merged drops capped at 16) sent to players tracking the chunk before `field/HarvesterWork.fell` removes the blocks; remove the base break sound and particles. Check: `gradlew :shuttles:build`, the payload registered.

## 4. Models and drawing

- [x] 4.1 `datagen/ShuttleModels`: `tool_lumber_base` and the standalone parts `lumber_turret`, `lumber_arm_lower`, `lumber_arm_upper`, `lumber_head`, `lumber_claw`, `lumber_saw`, each around its pivot, `STEEL` and `DARK_IRON` on every face; list every overlapping pair in the resting pose and at full reach with whether faces share a plane and fix it. Check: `gradlew :shuttles:runData`, `gradlew :shuttles:build`.
- [x] 4.2 `client/ServusShuttlesClient` registers the standalone models; `client/ShuttleItemModel` knows `tool_lumber_base`; `client/ShuttleGarageRenderer` draws the posed arm, the spinning saw and the sawdust while a view carries `Felling`. Check: `gradlew :shuttles:build`.
- [x] 4.3 `client/ClientFallingTrees`: payload handler, extract and submit through `ExtractLevelRenderStateEvent` and `SubmitCustomGeometryEvent`, rigid fall by `ToppleCurve` around the away edge, leaf bursts and logs vanishing, drops on arcs to the shuttle, cleared on level change. Check: `gradlew :shuttles:build`.

## 5. GameTests

- [x] 5.1 `gametest/LumberGameTests`: the tree's blocks are gone as the topple part starts and the drops are in the cargo; the spot is planted only after `topple_ticks`; Handling shortens the saw part and not the topple part; a tree broken by a player during the saw part ends the felling without a fall; a run saved during the topple part goes on after a reload. Check: `gradlew :shuttles:runGameTestServer`.

## 6. Plan and integration

- [x] 6.1 At archive: nothing left in `PLAN.md` for `m10-lumber`; add a line under `## Unreleased` in `shuttles/CHANGELOG.md` (Added: Lumber shuttles fell trees with an arm and a saw, and the tree topples away from the rail). Check: `openspec validate m10-lumber-felling-look --strict`.
- [x] 6.2 Full `gradlew build`, `gradlew test`, `gradlew :shuttles:runGameTestServer` green, the game launches with all mods; then the user's play check from main: watch felling from several angles (oak, birch, a 2x2 dark oak, a mangrove), the tree falls outward from the rail on both sides, no flicker on the arm, head, claws and saw at rest, while reaching and while sawing.
