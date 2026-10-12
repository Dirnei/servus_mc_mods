# Proposal

## Why

The Chunk Loader is a grey placeholder cube like the Coal Generator before it: the shared machine casing and a front with a green bar and a window that turns yellow while active. Whether a loader holds its chunk matters (no power, team cap, FTB Chunks), yet the block shows it only through a small window colour. An ender pearl goes into its recipe, so it can look like what it does: an anchor with an ender eye that watches over the chunk.

## What Changes

This change implements a follow-up to PLAN.md milestone **M9 Alchemy** for the Foundation machines (section 2.1, "Chunk Loader: keeps production and warehouses running overnight"), as a new item `m9-ender-anchor`. It follows the decisions taken in `/opsx:explore` on 2026-10-12 (memory note "generator-anchor-look-decisions", look "L1 Ender Anchor").

- **Ender Anchor look.** The Chunk Loader stays one full block: obsidian panels in the dark steel frame, an ender eye lens on the front, a chunk grid engraved on the sides and purple accents on the top that brighten while active. The block keeps its name "Chunk Loader".
- **The eye shows the state.** Shut while the loader does not hold its chunk (no power, team cap reached, FTB Chunks in charge, no owner); a short opening animation when it becomes active; while active it blinks now and then, at irregular intervals.
- **It looks at you.** While active, the iris turns toward the nearest player within a few blocks in front of it, and back to the centre when nobody is near.
- **Portal particles.** While active, a few faint purple portal particles drift around the block.
- `PLAN.md` and the docs describe the look.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- `foundation/chunk-loader`: new requirements for the Ender Anchor look and for the eye that shows the loader's state, blinks and follows the nearest player, and the particles while active.

## Non-goals

- Renaming the block to "Ender Anchor" (it stays "Chunk Loader"; can follow later).
- Loading behaviour, FE upkeep, the team cap, FTB Chunks handling, recipe and GUI (unchanged).
- A shaped model or moving parts besides the eye.
- Sounds.

## Impact

- Code, Foundation: `client/` new `EnderAnchorRenderer` (eye lid and iris, wrapping the side markers of `SideMarkerRenderer`), registration in `client/MachineClient`, `machine/loader/ChunkLoaderBlock` (particles in `animateTick`), a pure eye motion helper with JUnit tests, datagen (`MachineDataParts`: own textures and an eye socket in the model, item model).
- Assets: new `chunk_loader_front.png` (socket without the eye), `chunk_loader_side.png`, `chunk_loader_top.png`, `chunk_loader_top_lit.png`, `chunk_loader_eye.png` (sclera, iris and lid sprites); the old `chunk_loader_front_lit.png` goes.
- Docs: `docs/src/content/docs/mods/foundation.md` machines list; `PLAN.md` section 2.1.
