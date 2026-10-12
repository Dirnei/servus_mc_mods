# Design

## Context

See proposal.md (Why). Current state on `main` (Minecraft 26.1.2, NeoForge 26.1.2.116), Foundation:

- `machine/loader/ChunkLoaderBlock extends MachineBlock` (`FACING`, `LIT`); `ChunkLoaderBlockEntity extends PoweredMachineBlockEntity`: `tick()` returns working only while the loader is allowed and powered, `MachineBlockEntity` sets `LIT` from that, so `LIT` means "holds its chunk" (status `STATUS_ACTIVE`).
- Model from `datagen/MachineDataParts.machine(provider, CHUNK_LOADER, "chunk_loader")`: `orientable` with `chunk_loader_front`, `chunk_loader_front_lit`, `machine_side`, `machine_top`.
- `client/MachineClient` registers `SideMarkerRenderer` for the loader (side markers with the carrot or goggles). `SideMarkerRenderer` is a public generic class with a final nested `State`.
- Concept sketches: `genloader.py` and `round2.py` in the explore scratchpad (`l1_side`, `l1_top`, `eye(stage)`).

## Goals / Non-Goals

**Goals:**
- A full block look with the eye drawn by a renderer, so the lid, the blink and the iris can move smoothly.
- Eye motion decided by pure, tested code.

**Non-Goals:**
- Behaviour, rename, shaped model, sounds (proposal Non-goals).

## Decisions

### D1. Static block, eye in a renderer

The block model stays a full `orientable` cube with own textures: `chunk_loader_front` (frame and obsidian with an empty dark socket in the middle, 6x6 pixels), `chunk_loader_side` (chunk grid), `chunk_loader_top` and `chunk_loader_top_lit` (purple accents, brighter while lit). Blockstates keep `FACING` and `LIT` (the top changes). The eye is drawn by a new `client/EnderAnchorRenderer` as three quads just in front of the socket (offset 1/256 like the side markers): the sclera, the iris (2x2 pixels plus pupil, moved by the iris offset) and the lid (covers the lens from top and bottom by the lid openness). Sprites come from one texture `chunk_loader_eye.png` on the block atlas, picked with UVs. The item shows the block model only, so its eye is shut (the socket reads as a closed lid: the front texture paints the closed lid into the socket).

Alternative: animated textures (`.mcmeta`) for open, blink and closed. Rejected: they cannot follow a player and cannot be out of step per block.

### D2. Pure eye motion

New pure `machine/loader/EyeMotion` (no Minecraft imports):
- `openness(previousOpenness, active, dt)`: moves toward 1 while active and 0 while not, at 1 per second.
- `blink(seed, timeSeconds)`: lid closure from 0 to 1 for a blink lasting about 0.15 s, blinks spaced 3 to 9 s apart, the schedule derived from `seed` (the block position hash), so neighbours differ.
- `iris(dx, dy, dz, facing)`: the iris offset in pixels inside the lens (clamped to a radius of 1.5) for a player at offset `(dx, dy, dz)` from the eye in block space; zero when the player is behind the front plane or farther than the range; `smooth(previous, target, dt)` eases toward the target.
The renderer combines openness and blink into the lid (`openness * (1 - blink)`).

JUnit: `EyeMotionTest`: opens in about a second and closes the same; blink spacing between 3 and 9 s over 100 blinks and blink length short; two different seeds blink at different times; iris points left for a player on the left, centre for a player behind or beyond range, never leaves the radius; smoothing converges.

### D3. Renderer state and nearest player

`EnderAnchorRenderer implements BlockEntityRenderer<ChunkLoaderBlockEntity, EnderAnchorRenderer.State>` wraps a `SideMarkerRenderer` for the markers: its `State` holds the marker state and the eye values; `extractRenderState` fills both, `submit` submits both. Eye values per block entity live on the client in a small `EyeView` object kept on `ChunkLoaderBlockEntity` (client only, not saved): last openness, iris offset and last time. The nearest player comes from `Level.getNearestPlayer(x, y, z, 8, false)` on the client in `extractRenderState`. `MachineClient` registers this renderer instead of `SideMarkerRenderer` for the loader.

API references: `BlockEntityRenderer#extractRenderState` and `submit` as used by `client/BurnerPumpRenderer` and `client/SideMarkerRenderer`; `Level#getNearestPlayer` (vanilla 26.1.2, checked with `javap` during apply).

### D4. Particles

`ChunkLoaderBlock.animateTick` while `LIT`: with a small chance per tick (example 1 in 3) one `ParticleTypes.PORTAL` particle at a random point around the block, like an ender chest. Look only.

### D5. Tests

JUnit for `EyeMotion` (D2). GameTests cannot see the renderer; the existing loader GameTests stay green and one new GameTest `chunkLoaderLitFollowsActive` checks that `LIT` is true while active and false over the team cap (the state the eye reads). The look is checked in game.

PLAN.md open decisions settled: none.

## Risks / Trade-offs

- [Renderer cost] One small renderer per loader; loaders are capped per team (default 4), so few exist.
- [Item looks shut] The item never shows an open eye. → Intended: an item holds no chunk.
- [Iris readability] A 2x2 iris on a 6x6 lens moves at most 1.5 px. → Tuned in the play check; constants in `EyeMotion`.
