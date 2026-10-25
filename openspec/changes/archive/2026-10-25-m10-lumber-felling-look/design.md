# Design

## Context

After `m10-lumber`, `shuttle/FieldRuns.fellAndPlant` counts down `fell_ticks` at the rail, then `field/HarvesterWork.fell` walks the tree (`TreeWalk`), rolls the drops, removes the blocks with `Block.UPDATE_CLIENTS` and plays a break sound and particles at the base, and the spot is planted. Shuttles are data on the server; clients draw them in `client/ShuttleGarageRenderer` from `shuttle/ShuttleView` (path, segment, progress, drive, cargo, item, glass, lead), sent in `ShuttleSyncPayload` when the fleet calls `markSync`. The Lumber tool is a static `tool_lumber` part (turret, folded arm, felling head) composed into the shuttle's item model by `client/ShuttleItemModel`. Moving parts elsewhere use standalone models (`ModelEvent.RegisterStandalone`, see `foundation/client/ModelKeys`) and whole block states are drawn with `foundation/client/BlockSubmit.block`.

This change needs `m10-lumber` implemented and archived: its `shuttles/lumber` deltas modify that capability, and `openspec validate` already notes that archive refuses them until `openspec/specs/shuttles/lumber/spec.md` exists.

The look follows style A of `temp/lumber-felling.html`: reach, grip, saw, topple with the angle growing as the square of time, land, strip with leaf bursts and items flying to the shuttle, fold back, replant.

## Goals / Non-Goals

**Goals:**
- The felling reads as one motion from reach to replant, seen by every player near the field.
- The server stays the only one that changes the world, once per tree.

**Non-Goals:**
- Collision of the falling tree, landing lane checks, the fall for players who arrive late.

## Decisions

### PLAN.md
No open decision is settled here. At archive nothing is left of `m10-lumber` in PLAN.md (the first change removed it).

### Phases on the server
`HarvestRun` (the run Lumber shuttles share with harvesters) gets `felling`: the trunk base, the phase (`SAW`, `TOPPLE`) and its countdown. Felling starts with `SAW` for `fell_ticks` shortened by Handling and `markSync`; the drops are rolled and the walk is done at the start, as today, only to decide whether the cargo has room. When the countdown ends the walk runs again on the world as it is now, the drops are rolled for real, the payload is sent, the blocks are removed, the drops go into the cargo, and the phase turns `TOPPLE` for `topple_ticks` (`ShuttlesStatsManager`, `field.json` example 80) with `markSync`. When `TOPPLE` ends the spot is planted and `felling` is cleared. A walk that now finds `TooLarge` or `NotATree` skips `TOPPLE` and folds the arm at once. Felling state is not saved: a shuttle restored mid felling goes on with its run from its rail and fells the tree again if it still stands, or plants the empty spot.

### Shuttle view
`ShuttleView` gets `Optional<Felling> felling`, `Felling(BlockPos trunk, Phase phase, int ticks)` with its own `StreamCodec` (the record then has ten fields, within `StreamCodec.composite`'s twelve in Minecraft 26.1.2). The client takes the phase start from the fleet's `receivedTick`, as it does for the glide, and runs the arm's timeline over `ticks`. Inside `SAW` the share is split like the mockup: 40 percent reach, 15 percent grip, 45 percent saw; inside `TOPPLE` 40 percent fall, 40 percent break apart, 20 percent fold back. The reach and saw scale with Handling because `ticks` comes from the server; the fall does not.

### Fall payload
New `field/TreeFallPayload` (server to client): `BlockPos base` (smaller corner of the trunk), `Direction away`, `BlockPos rail`, `int ticks` (the fall and break apart share of `topple_ticks`), `List<FallingBlock>` with `FallingBlock(int dx, int dy, int dz, BlockState state)` relative to the base (offsets as `VAR_INT`, states with `ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY)`), and `List<ItemStack> drops` merged by item and capped at 16 entries (`ItemStack.OPTIONAL_STREAM_CODEC`). A tree at the data limit of 1000 blocks is about 11 KB, far below the clientbound custom payload limit. It is sent with `PacketDistributor.sendToPlayersTrackingChunk(level, ChunkPos.containing(base), payload)` before the blocks are removed in the same tick: the payload goes out at once while block changes are broadcast with the chunk's next change flush, so a client never has a frame without both the world tree and the falling copy. Registered in the existing payload registrar, client handler only.

### Fall direction, curve and arm pose (pure)
- `field/FellDirection.away(rail, trunk, railAxis)`: the horizontal direction from the rail to the trunk along the larger of the two offsets; on a tie, the axis across the rail's run. JUnit: both sides of a north to south rail, an east to west rail, a corner plot, a tie.
- `field/ToppleCurve.angle(t)` = 90 degrees times `t` squared for `t` from 0 to 1, clamped. JUnit: 0, a quarter, half, 1, beyond 1.
- `field/ArmPose.of(distance, progress, grip)`: turret yaw, the two segment angles for a reach of `distance` blocks (two link inverse kinematics with fixed segment lengths, the head ending at plot level), and the claw opening. JUnit: folded at progress 0, head at the trunk at progress 1 for distances 1 to 4, unreachable distance clamps.
All three without Minecraft imports; the client maps `Direction` and `BlockPos` to plain numbers.

### Client drawing
- **Arm**: standalone models `servus_shuttles:shuttle/lumber_turret`, `lumber_arm_lower`, `lumber_arm_upper`, `lumber_head`, `lumber_claw`, `lumber_saw`, registered with `ModelEvent.RegisterStandalone` like Foundation's `ModelKeys`. While a view carries `Felling`, `ShuttleGarageRenderer` draws the shuttle with the `tool_lumber_base` part (the tool part without arm and head; `ShuttleItemModel` gets the extra part name) and the arm parts posed by `ArmPose`, the saw turned a fixed angle per tick while sawing. Sawdust: `ParticleTypes.FALLING_DUST` with the trunk's log state, a few per tick at the cut, client side only.
- **Falling trees**: `client/ClientFallingTrees` keeps the received falls with their client start tick, extracts the ones to draw in `ExtractLevelRenderStateEvent` and submits them in `SubmitCustomGeometryEvent` (NeoForge 26.1.2.116, verified in the sources jar; the javadoc of `SubmitCustomGeometryEvent` requires the extract step). Each block is drawn with `BlockSubmit.block` around the pivot on the base's away edge at ground level, rotated by `ToppleCurve.angle`, lit by the light at the base. Landed, leaves vanish first in a random order with `ClientLevel.addDestroyBlockEffect` for each, then logs, while the drops are drawn as items on arcs to the shuttle's rail. A fall is dropped when it ends or the player changes level.

### Models
The arm and head are real boxes in `STEEL` and `DARK_IRON` on every face. Each standalone part is built around its own pivot so the renderer only rotates it. The task lists, for the resting pose, full reach and every overlapping pair of parts (turret and lower arm, lower and upper arm, upper arm and head, head and both claws, head and saw), whether any faces share a plane, fixed by ending exactly at the neighbour's face, insetting the inner part 0.05 px, or giving the two claws a 0.005 px step.

### APIs
- `SubmitCustomGeometryEvent`, `ExtractLevelRenderStateEvent`, `ModelEvent.RegisterStandalone`, `PacketDistributor.sendToPlayersTrackingChunk` (NeoForge 26.1.2.116 sources).
- `ByteBufCodecs.idMapper(IdMap)`, `Block.BLOCK_STATE_REGISTRY`, `StreamCodec.composite` up to twelve fields, `ParticleTypes.FALLING_DUST`, `ClientLevel.addDestroyBlockEffect` (Minecraft 26.1.2 sources).

Pure Java with JUnit: `FellDirection`, `ToppleCurve`, `ArmPose`. Minecraft side: the phases by GameTest, the drawing by the play check.

## Risks / Trade-offs

- [The fall clips into hills and other trees] → Accepted by the user; it lasts about two seconds.
- [Many trees falling at once cost frame time] → One fall per Lumber shuttle at a time; each tree is at most `max_tree_blocks` block models for a few seconds.
- [A player sees the tree vanish without a fall when the payload was lost or arrived late] → The world is right either way; the look is decoration.
- [Handling makes the reach and saw fast] → Only the saw part scales; the fall keeps its weight.

## Migration Plan

None; nothing new is saved.
