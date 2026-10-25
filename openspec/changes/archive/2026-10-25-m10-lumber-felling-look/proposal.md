## Why

`m10-lumber` makes tree fields work, but a tree just vanishes with a break sound. Felling is the moment players watch a tree farm for, and the user chose style A of the mockup `temp/lumber-felling.html`: the shuttle reaches out with an arm, saws the trunk, and the whole tree topples away from the rail, lands and breaks apart into items.

This change belongs to PLAN.md milestone M10 (Shuttles open point `m10-lumber`) and is the second of two stacked changes: it needs `m10-lumber` implemented and archived first, since its deltas modify the `shuttles/lumber` capability that change creates.

## What Changes

- **Felling arm**: while a Lumber shuttle fells, its turret turns toward the tree, the two part steel arm unfolds to the trunk, the felling head's claws grip it, the saw disc spins at the base with sawdust; afterwards the arm folds back and the sapling is planted.
- **Toppling tree**: the server removes the tree's blocks once, at the start of the fall, and sends the nearby players one payload with the trunk position, the fall direction, every removed block's state and position and the drops. The client draws the whole tree as one rigid group that tips over its base edge straight away from the shuttle (perpendicular to the rail, outward), faster and faster like falling, lands flat, and breaks apart with leaf particles while the drop items fly into the shuttle's cargo.
- **Felling time follows the look**: felling is split into the saw part (`fell_ticks`, shortened by Handling) and the topple part (`topple_ticks`, example 80, never shortened, so the animation fits); the shuttle replants after the arm has folded back.
- Clipping of the falling tree into terrain or other blocks is accepted; nothing checks a landing lane, and nothing the falling tree passes is changed.
- The arm, the felling head and the saw are real geometry in the family's steel, following the z-fighting rules.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `shuttles/lumber` (created by `m10-lumber`): "Tree field run" splits felling into a saw and a topple part; "Drops of a felled tree" leaves the sound and particles to the new look; new requirements for the felling arm, the toppling tree, the break apart and the pure fall logic.

## Impact

- Shuttles mod: `shuttle/ShuttleView` gains the felling state of a shuttle (trunk, phase, ticks); `shuttle/FieldRuns` runs the saw and topple phases and sends the new `field/TreeFallPayload`; `field/HarvesterWork.fell` returns what it removed for the payload; new pure `field/FellDirection`, `field/ToppleCurve`, `field/ArmPose`; `client/ShuttleGarageRenderer` draws the arm parts while a shuttle fells; new `client/ClientFallingTrees` drawn through `SubmitCustomGeometryEvent`; standalone models for the arm, head and saw in `datagen/ShuttleModels` registered in `client/ServusShuttlesClient`; `stats/ShuttlesStatsManager` and `field.json` `topple_ticks`.
- Network: one new clientbound payload, sent to players tracking the tree's chunk.
- Saves: nothing new is saved; a shuttle saved mid felling goes on with its run.

## Non-goals

- A landing lane check, or the falling tree pushing, breaking or hurting anything.
- The animation for players who join or come near while a tree is already falling.
- Other felling styles from the mockup (chopped from the top, gone at once).
- Sounds beyond the vanilla wood, saw like and leaf sounds already in the game.
