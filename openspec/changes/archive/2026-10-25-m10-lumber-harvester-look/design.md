# Design

## Context

`m10-lumber-felling-look` draws felling from `ShuttleView.Felling(trunk, phase, ticks, elapsed)` with phases `SAW` and `TOPPLE`, set by `shuttle/FieldRuns` on `HarvestRun.felling` and counted down at the rail. `client/ShuttleGarageRenderer` draws a felling shuttle with its tool removed (`withoutTool`) and `client/FellingArmLook` adds the posed standalone parts `lumber_base`, `lumber_turret`, `lumber_arm_lower`, `lumber_arm_upper`, `lumber_head`, `lumber_claw`, `lumber_saw` from `datagen/ShuttleModels.lumberRig`, posed by the pure `field/ArmPose`. The shuttle item model is composed by `client/ShuttleItemModel` from `body_mk<tier>`, `drive_<step>`, `handling_<step>` and `tool_<id>`. The shuttle is drawn at 0.8 scale with its bottom 2 pixels above the rail block's floor, so model height `m` lands at `2 + 0.8 m` block pixels; the garage doorway (`garage/GarageDoorLook.DOOR_TOP`) is 12 pixels, so the hold may reach model height 12.5.

The look is the mockup `temp/lumber-tool.html` (artifact https://claude.ai/artifact/5TfzDf4CyumKHRX2D4a2Ny, version 8); its numbers below are model pixels, the shuttle body spans x and z 2 to 14, one block is 20 model pixels at the shuttle's scale.

This change needs `m10-lumber` and `m10-lumber-felling-look` archived first; it modifies and removes requirements they add.

## Goals / Non-Goals

**Goals:**
- A Lumber shuttle reads as a plain shuttle on the rail and as a heavy forestry machine at the tree.
- Server work stays as it is apart from two timed parts; the look is client side.

**Non-Goals:**
- Following the topple with the head, collision of any moving part, a garage taller than 12 pixels (see proposal).

## Decisions

### PLAN.md
No open decision is settled. At archive nothing is removed from PLAN.md (the item went with `m10-lumber`); the garage doorway being lower than the spoiler and the loading arm is added as an open point under Shuttles.

### Server phases
`ShuttleView.Felling.Phase` gets `UNFOLD` and `FOLD` beside `SAW` and `TOPPLE`; `HarvestRun` gets `unfolded`. In `FieldRuns.stepTicks`, when the next plot is a tree to fell and the run is not unfolded, the felling is `UNFOLD` for `unfold_ticks` with the tree as trunk; when it ends `unfolded` is set and the same tree goes to `SAW`. Every place that ends the stop (`leaveRail`, `turn` for full cargo or run end) first checks `unfolded`: if set, the felling becomes `FOLD` for `unfold_ticks` with the last trunk, the shuttle waits at the rail, and when it ends `unfolded` clears and the original leave goes on (a small `HarvestRun.AfterFold` enum: on to the next rail, or the run decides again, which also covers unloading a full cargo and the run's end). Planting stays where it is, after `TOPPLE`. `unfolded` is not saved; a restored shuttle is folded, and the felling look already restarts from the run (see the earlier change). `unfold_ticks` joins `ShuttlesStatsManager` and `field.json` (example 30), not shortened by Handling.

Alternative considered: unfold and fold inside every tree's saw and topple parts with no server change. Rejected: two seconds of saw part are too short for doors, walls, crane and grip, and the shuttle would fold between two trees at the same rail.

### Timeline (pure)
`field/HarvesterTimeline.of(phase, progress)` gives named shares:
- `UNFOLD`: doors 0 to 0.3 (0 to 270 degrees), walls 0.3 to 0.6 (0 to 135 degrees), crane rise 0.6 to 1 (pillar up, booms unfold, head turns upright and grows from 0.55 to 1).
- `SAW`: reach 0 to 0.35 (slew, telescope, head stands and opens), move in 0.35 to 0.5, grip 0.5 to 0.62 (both arm links close), cut 0.62 to 1 (bar swings out; sawdust while it is out).
- `TOPPLE`: let go 0 to 0.1 (arms open, bar parks, head backs off 7 toward the rail and up 2), wait 0.1 to 0.8 (the fall and break apart of `TreeFalls.FALL_SHARE`), swing home 0.8 to 1 (arms fold, head over the hold, upright).
- `FOLD`: `UNFOLD` backwards.
JUnit: shares sum and order, each phase's start and end pose, `FOLD` at `p` equal to `UNFOLD` at `1 - p`.

### Pose (pure)
`field/HarvesterPose.of(trunk, timeline)` replaces `ArmPose`. Input: the trunk's grip point in the shuttle's model space (x across, y up, z forward, from the renderer as today) and the timeline. Output: door angle, wall angle, pillar lift (top from y 8.5 to 17), main boom end (elbow), wrist, telescope length, head yaw, tilt and roll, head scale, arm angles `a1` and `a2`, saw swing.
- **Crane**: pillar top `P0` at (8, 8.5..17, 11.5); main boom 9 long; the wrist is solved as two links with the elbow up, the stick's length chosen as 0.8 of the distance, at least 9.5, longer when the trunk is far (the telescope runs out; no upper limit, so trees four plots out are reached). While rising, the elbow blends from its stowed point (8, 9, 3.5) to the solved one.
- **Head at the trunk**: the rotator stands 2 short of the base log's near face, centred on it across, at 13.8 above the plot floor, so the head's bottom is at the cut; for a 2x2 tree the base log nearest the rail.
- **Stowed**: the head lies on its side in the hold (yaw 90, roll 90 degrees), scaled 0.55, wrist at (8, 8.7, 12.6), arms folded; it fits inside the walls and under the doors. The scale is the one deliberate cheat, agreed with the user.
- **Arms**: each arm is an inner link from the frame's hinge (3.5, 1.5 in head space) to an elbow and an outer link along the log; `a1` and `a2` are 0 when hugging, (20, 35) degrees open, (90, -90) folded flat along the frame.
JUnit: grip centre on the trunk axis for one to four plots, stowed parts inside the hold box, rest pose with doors shut and walls up, no NaN over a sweep of progress and trunk positions.

### Models
All boxes on every face, `STEEL`, `DARK_IRON` and `ARM` (yellow terracotta, as the loading mast) plus the tier's hold texture and `CHASSIS`. Parts as in the mockup:
- `lumber_body_mk1..3`: chassis, tracks under it stay with the drive part, hold floor 2.75..13.25 at y 4..4.75, nothing else; drawn when unfolding. `lumber_hold_mk1..3`: the closed hold for rest (walls 0.75 thick to y 11.5, rim to 12.5, doors in the rim, Mk3 gold corners inset in the rim), used by `ShuttleItemModel` in place of `body_mk<tier>` when the Lumber module is the tool. Moving parts per tier: `lumber_side_mk*` (side wall with its rim strip and two dark stabilizer strips), `lumber_end_mk*` (front or back wall with the rim's corners, drawn twice), `lumber_door_mk*` (one door with its dark panel, drawn twice, mirrored).
- Crane: `lumber_ring`, `lumber_pillar` (stretched to its lift), `lumber_boom` (3 by 3, stretched to 9), `lumber_cylinder`, `lumber_knuckle`, `lumber_stick` (2.5 by 2.5), `lumber_telescope` (1.8 by 1.8, stretched).
- Head: `lumber_rotator`, `lumber_frame`, `lumber_saw_box`, `lumber_roller_inner`, `lumber_roller_outer` (with the roller), `lumber_knife_inner`, `lumber_knife_outer`, `lumber_bar` (bar and chain), mirrored for the left side by a negative x scale in the pose stack.
- `tool_lumber` becomes empty (the hold carries the look) and the `lumber_*` arm parts of `lumberRig` go.
The task for models lists every pair of overlapping boxes at rest, folded and gripping (walls and rim, doors and rim, wall and end corners, pillar and ring, boom and knuckle, stick and telescope, frame and rotator, frame and saw box, each arm's inner and outer link, roller and outer link, the two mirrored arm sets) and fixes shared planes by ending at the neighbour's face, a 0.05 inset, or a 0.005 step for parts drawn twice.

### Client drawing
`FellingArmLook` becomes `HarvesterLook`: it draws a felling view's shuttle with `withoutTool` and the Lumber hold swapped for `lumber_body_mk<tier>`, then every part posed from `HarvesterPose` with `poseStack` translate, rotate and scale (stretched parts scale along their length; mirrored parts scale x by -1, which the item renderer draws with flipped winding handled by `RenderSubmit` as for other mirrored parts, verified in the task). Sawdust stays as today while the bar is out. `ShuttleItemModel` adds the rule: Lumber tool means `lumber_hold_mk<tier>` instead of `body_mk<tier>`, no `handling_<step>`, and `drive_3` without its middle struts (`drive_3_lumber`, the spoiler on its end plates reaching down to the chassis sides). `ShuttleLook.openHold` is false for a Lumber shuttle, so no cargo is drawn.

### Chainsaw, hold and crane sounds
Sound events in `registry/ServusShuttlesSounds`, made with `SoundEvent.createVariableRangeEvent` in a `DeferredRegister<SoundEvent>` as Alchemy's `hint/HintSounds`: `chainsaw.idle`, `chainsaw.cut`, `chainsaw.stop` from the Ogg files under `assets/servus_shuttles/sounds/` (mono, 44.1 kHz, cut from the user's recording; the idle is a 1.25 second seamless loop, its tail crossfaded into its head, with more bass and the tinny 2.8 and 5.5 kHz bands cut), and `hold.doors_open`, `hold.walls_down`, `crane.up`, `crane.down`, `hold.walls_up`, `hold.doors_shut`, each a vanilla iron trapdoor, iron door or piston sound event at a lower pitch (0.55 to 0.7). `sounds.json` comes from `datagen/ServusShuttlesSoundsProvider` (NeoForge `SoundDefinitionsProvider`, event type entries with a pitch), subtitles from the language provider.

The pure `field/HarvesterSound` says whether the idle runs at a timeline moment (from the rise of unfolding through ready, saw outside its cut share and topple, not while folding) and which cue a moment calls for: doors open at the doors stage, walls down near the end of the walls stage, crane up at the rise, cut in the cut share, stop in let go, and crane down, walls up and doors shut through folding. `client/HarvesterSounds` keeps one entry per rail the felling shuttle stands on: the cues it played (the chainsaw's per tree, by the gripped log; a new doors open after doors shut starts a new round), the looping idle as an `AbstractTickableSoundInstance` moved to the saw on each drawing, and the chainsaw sound with the tick it ends (cut 64, stop 32), until which the idle stays quiet. Hold and crane sounds play at the hold, chainsaw sounds at the bar's pivot, category `SoundSource.BLOCKS`. A `ClientTickEvent.Post` handler silences a shuttle not drawn for 3 ticks and forgets it after a minute.

### Decisions made while applying
- **Hold height.** The rim tops out at 12.45 and the door panels and Mk3 gold corners at 12.5, so the closed hold stays under the doorway; the mockup's panels reached 12.7 and its gold corners 12.55. Door leaves sit at 11.6 to 12.3 with a 0.2 panel on them, which also hides the parked bar's tip of the stowed head.
- **Mirroring.** No negative scale: the left side wall, the back wall and the left door are the right and front ones turned half round the hold's vertical centre line (they are symmetric), and the left arms of the head are their own models (`lumber_*_left`) with mirrored boxes and negated angles in the pose. So every face keeps its winding and shows from outside.
- **Geometry in pure Java.** `field/HarvesterParts` holds every box of the hold and the crane, `field/HarvesterPose.parts()` the frame of each drawn part (`field/Frame`, a pure affine transform); datagen writes the boxes, the renderer applies the frames, and the JUnit `HarvesterFlickerTest` checks every pair of faces for a shared plane where they overlap, for each tier, five trunk positions and each stage at three progress points, plus the closed hold.
- **Standing ready.** A phase `READY` in the view shows the shuttle unfolded between two trees at one rail, with the crane raised over the hold; without it the look would snap shut while it plants a spot between trees.
- **Grip log.** The server sends the base log the head grips (for a 2x2 tree the one nearest the rail, picked with the pure `HarvesterPose.nearestLog`) as the felling view's trunk.
- **Near trees.** For a log right beside the rail the head backs off (and approaches) only as far as keeps its back clear of the slewing ring (wrist at least 6 out from the centre line), instead of 7 and 6 into the hold.
- **Unfolded drawing.** While unfolded the renderer draws no item: the drive part and `lumber_body_mk<tier>` are standalone models with the posed parts. `ShuttleLook.parts` names the item model's parts (Lumber hold, `drive_3_lumber`, no Handling) as pure Java with tests; `ShuttleLook.openHold` takes the tool.
- **Small changes to the mockup's numbers** (for the flicker rules or to stay in the hold): the pillar is 0.05 narrower on each side than the boom; the slewing ring ends at z 13.2 inside the front wall; the stowed knuckle sits at z 4.55 instead of 3.5 so it stays inside the back wall; outer arm links are 0.05 shorter than the inner ones at top and bottom; the second rubber box of the roller is 0.05 shorter at each end; the roller's end plate ends at its bar; the left arms sit 0.005 higher and forward; the gold corners start 0.05 below the rim; the strutless spoiler's end plates reach down to y 2 with a foot into the chassis side and end at z 2.7. The pillar carries a separate dark collar part (`lumber_collar`).

### APIs
`ModelEvent.RegisterStandalone` and `StandaloneModelKey` (as `FellingArmLook` today, NeoForge 26.1.2.116), `PoseStack.mulPose(Matrix4fc)` for each part's frame (it recomputes the normal matrix for a stretched part), `PoseStack` transforms, `SoundEvent.createVariableRangeEvent`, `SoundDefinitionsProvider`, `SimpleSoundInstance`, `SoundManager.play` and `stop`, `ClientTickEvent.Post`, the existing `RenderSubmit.item` and the item model composition of `ShuttleItemModel`. No new NeoForge API.

Pure Java with JUnit: `HarvesterTimeline`, `HarvesterPose`. Minecraft side: the phases by GameTest (unfold before the first tree, no fold between two trees at one rail, fold before leaving and before unloading, no unfold to plant only), the drawing by the play check.

## Risks / Trade-offs

- [Unfold and fold add a second and a half per stop with trees] → Balance number in `field.json`; trees at one rail share one unfold.
- [The 0.55 head scale reads as magic] → Agreed in the mockup; it grows while it turns and rises, which hides most of it.
- [Folded walls and the crane pass through neighbouring rails, shuttles or blocks] → Decoration only, like the topple.
- [Many standalone parts per shuttle] → Only felling shuttles draw them, one Lumber shuttle per field at a time in practice.

## Migration Plan

None; `unfolded` is not saved and nothing else new is.
