# Proposal

## Why

In the play check of `m10-lumber` and `m10-lumber-felling-look` the Lumber tool read as too flimsy: a 1 pixel post, 2 pixel arms and a small flat disc beside a one-block trunk. The user worked out a new look in the mockup `temp/lumber-tool.html` (artifact https://claude.ai/artifact/5TfzDf4CyumKHRX2D4a2Ny), after real forestry harvesters and in transformer style, and signed it off.

Milestone M10, the felling look of the Lumber module (the `m10-lumber` item, already taken off PLAN.md by `m10-lumber`).

## What Changes

- **Lumber hold.** A Lumber shuttle gets its own hold in its tier's material (oak crate, iron, diamond), closed by a lid of two doors and a dark rim, as tall as the garage doorway allows (model height 12.5, the 12 pixel opening). At rest it looks like a plain shuttle; the old turret, folded arm and felling head are gone.
- **Transform.** When the shuttle stops at a rail with a tree to fell, the doors swing up, over and down the outside of the side walls, all four walls fold out and down on their bottom edge until their tops rest on the ground as stabilizers, and a knuckle-boom crane rises out of the hold: a pillar on a slewing ring, a yellow main boom with its lift cylinder, a knuckle, a yellow stick with a steel telescope, a rotator and tilt frame, and a harvester head. Before the shuttle drives on, all of it folds back in reverse.
- **Harvester head.** A yellow frame with two roller arms (spiked rubber feed rollers) and two knife pairs, each arm of two links that fold flat along the frame and open wide round the log, and a chain saw bar parked under the frame.
- **Felling motion.** For each tree the crane swings out, the telescope runs out, the head stands up and opens, goes round the log at its base, closes both links of every arm, and the bar swings out across the base. As the tree starts to tip the head lets go at once, backs off and waits open while the tree falls and breaks apart, then swings back over the hold. The topple and break apart stay as `m10-lumber-felling-look` built them.
- **Server phases.** Felling gets an unfold part before the first tree of a stop and a fold part before the shuttle leaves the rail, each `unfold_ticks` long (in `field.json`, example 30). Handling does not shorten them.
- **Sounds.** Players near the shuttle hear a chainsaw at the head, from the user's own recording: it idles in a loop from the crane's rise until it folds, revs up and cuts while the bar swings across, and winds down as the head lets go, then idles again. Unfolding and folding sound step by step: doors, walls landing or rising, crane rising or sinking, as lower pitched vanilla door and piston sounds. Nine sound events with subtitles.
- **Handling part.** A Lumber shuttle draws no loading arm; on a Lumber shuttle with the last Speed step the spoiler stands on its end plates without the two middle struts, so the back wall folds out under the wing.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `shuttles/lumber`: the felling arm is replaced by the harvester crane, its unfold and fold parts join the tree field run's timing, and the pure pose logic now covers the crane.
- `shuttles/shuttle-looks`: the Lumber module's look becomes the Lumber hold, a Lumber shuttle shows no loading arm, and its spoiler drops the middle struts.

## Non-goals

- Raising the garage doorway: the Mk3 spoiler and the loading arm's crane already reach past its 12 pixel top today; that stays an open point.
- Following the toppling tree with the head: it lets go at the start of the topple.
- Collision of the crane, the walls or the falling tree with blocks, rails or other shuttles; parts pass through.
- Cargo drawn in a Mk1 Lumber hold: the hold is closed by its doors, so no cargo shows.
- A distinct look for 2x2 trees: the head grips the base log nearest the rail.

## Impact

- Apply after `m10-lumber` and `m10-lumber-felling-look` are archived: this change modifies requirements those two add (`shuttles/lumber` "Felling arm", "Fall logic is pure", "Tree field run", "Drops of a felled tree"; `shuttles/shuttle-looks` "A shuttle is drawn from its parts") and replaces code they built.
- `shuttles/datagen/ShuttleModels`: `tool_lumber` and the `lumber_*` arm parts give way to the Lumber hold per tier and the crane and head parts.
- `shuttles/client`: `ShuttleItemModel` picks the Lumber hold instead of the body and no Handling part; `ShuttleGarageRenderer` and `FellingArmLook` (renamed to the harvester look) draw the posed parts.
- `shuttles/field/ArmPose` is replaced by a pure crane pose and timeline with JUnit tests.
- `shuttles/registry` gains the chainsaw, hold and crane sound events, `shuttles/datagen` a sounds provider and their subtitles, `assets/servus_shuttles/sounds/` the chainsaw Ogg files; `shuttles/client` plays them.
- `shuttles/shuttle/FieldRuns`, `HarvestRun`, `ShuttleView.Felling` gain the unfold and fold phases; `ShuttlesStatsManager` and `field.json` gain `unfold_ticks`.
- No saved data changes.
