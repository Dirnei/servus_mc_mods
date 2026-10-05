# Proposal

## Why

Production must run overnight while nobody is near. The plan asks for a chunk loader with FE upkeep and a config cap per team that respects FTB Chunks. PLAN.md milestone M3, item "Chunk loader upgrade with FE upkeep and config caps".

## What Changes

- **Chunk Loader** block: a powered machine that keeps its own chunk loaded and ticking while it has energy, spending an upkeep per tick (10 FE/t by default, in stats). Owned by the placer's team.
- Per team cap from the new Manufacturing common config (`maxChunkLoadersPerTeam`, default 4). Loaders beyond the cap stay inactive and say so.
- FTB Chunks respect: when FTB Chunks is installed and the config says so, Servus chunk loaders stay inactive and point the player to FTB Chunks.
- Tickets through a NeoForge ticket controller, so they survive restarts and are released when the loader is removed or unpowered.
- GUI with the energy bar and a status line. Assembling recipe without tech: iron gears, copper wire and an ender pearl.

## Capabilities

### New Capabilities
- `manufacturing/chunk-loader`: when a chunk stays loaded, the cap and the FTB Chunks rule.

### Modified Capabilities
None.

## Non-goals

- Loading more than the loader's own chunk (range upgrades later).
- Per player caps separate from team caps.

## Impact

- First Manufacturing config file, `servus_manufacturing-common.toml`.
- New saved data `servus_chunk_loaders` for the cap.
