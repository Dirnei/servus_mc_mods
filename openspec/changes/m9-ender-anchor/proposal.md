# Proposal

## Why

The Chunk Loader is a grey placeholder cube like the Coal Generator before it: the shared machine casing and a front with a green bar and a window that turns yellow while active. Whether a loader holds its chunk matters (no power, team cap, FTB Chunks), yet the block shows it only through a small window colour. An ender pearl goes into its recipe, so it can look like what it does: an anchor with an ender eye that watches over the chunk.

## What Changes

This change implements a follow-up to PLAN.md milestone **M9 Alchemy** for the Foundation machines (section 2.1, "Chunk Loader: keeps production and warehouses running overnight"), as a new item `m9-ender-anchor`. It follows the decisions taken in `/opsx:explore` on 2026-10-12 (memory note "generator-anchor-look-decisions", look "L1 Ender Anchor").

- **Ender Anchor look.** The block stays one full block: obsidian panels in the dark steel frame, an ender eye lens on the front, a chunk grid engraved on the sides and purple accents on the top that brighten while active.
- **The eye shows the state.** Shut while the loader does not hold its chunk (no power, team cap reached, FTB Chunks in charge, no owner); a short opening animation when it becomes active; while active it blinks now and then, at irregular intervals.
- **It looks at you.** While active, the iris turns toward the nearest player within a few blocks in front of it, and back to the centre when nobody is near.
- **Portal particles.** While active, a few faint purple portal particles drift around the block.
- **Renamed to Ender Anchor** (decided in the play check on 2026-10-13). The in game name, the block and item id `servus_foundation:ender_anchor`, the machine data file `machine/ender_anchor.json`, the config keys `maxEnderAnchorsPerTeam` and `enderAnchorRespectsFtbChunks`, the classes, textures and saved data, and the capability `foundation/chunk-loader`, which becomes `foundation/ender-anchor`. No migration: Chunk Loaders placed in dev worlds disappear (nothing is released). The Storage Chunk Loader Upgrade keeps its name and id; its recipe takes an Ender Anchor.
- **Obsidian in the recipe.** The two bottom corners take obsidian instead of iron: iron, copper, iron on top, redstone, ender pearl, redstone in the middle, obsidian, copper, obsidian at the bottom.
- `PLAN.md`, the README and the docs describe the look and use the new name.

## Capabilities

### New Capabilities
- `foundation/ender-anchor`: everything `foundation/chunk-loader` required, under the new name and with the obsidian recipe, plus the Ender Anchor look, the eye that shows the state, blinks and follows the nearest player, and the particles while active.

### Modified Capabilities
- `foundation/chunk-loader`: removed, every requirement moves to `foundation/ender-anchor`.
- `storage/site-wake`: the team cap requirement names Ender Anchors and the new config keys.
- `storage/warehouse-controller`: the Chunk Loader Upgrade is crafted from an Ender Anchor.

## Non-goals

- Renaming the Storage Chunk Loader Upgrade.
- Loading behaviour, FE upkeep, the team cap, FTB Chunks handling and GUI (unchanged apart from the names).
- A shaped model or moving parts besides the eye.
- Sounds.

## Impact

- Code, Foundation: `client/` new `EnderAnchorRenderer` (eye lid and iris, wrapping the side markers of `SideMarkerRenderer`), registration in `client/MachineClient`, particles in the block's `animateTick`, a pure eye motion helper with JUnit tests, datagen (`MachineDataParts`: own textures and an eye socket in the model, item model).
- Rename, Foundation: package `machine/loader` becomes `machine/anchor` with `EnderAnchorBlock`, `EnderAnchorBlockEntity`, `EnderAnchorTickets`, `EnderAnchorData`, `EyeMotion`, `EyeView`; `client/ChunkLoaderScreen` becomes `EnderAnchorScreen`; the registry constant `CHUNK_LOADER` becomes `ENDER_ANCHOR`; `config/ServusFoundationConfig` keys; datagen lang, loot, recipe, models, blockstates and machine data; the loader GameTests.
- Rename, Storage: the upgrade recipe in `datagen/ServusStorageRecipeProvider`, the lang provider, and the site code that reads the team cap and the FTB Chunks key (`site/`, `controller/`, `command/`, `stats/`).
- Assets: `ender_anchor_front.png` (socket with the shut lid), `ender_anchor_side.png`, `ender_anchor_top.png`, `ender_anchor_top_lit.png`, `ender_anchor_eye.png` (sclera, iris and lid sprites); the old `chunk_loader_front_lit.png` goes.
- Docs: `docs/src/content/docs/mods/foundation.md`, `manufacturing.md`, `reference/datapack/machines.md`, `docs/src/data/gamedata.json`; `README.md`; `PLAN.md` section 2.1 and the module table.
