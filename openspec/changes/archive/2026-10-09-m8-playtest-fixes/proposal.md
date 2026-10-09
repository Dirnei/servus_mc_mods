# Proposal

## Why

The play test after M8 found a bug and four rough edges: after creating a pattern the Production Terminal showed "Loading..." on every tab until it was reopened, the search box could not be cleared quickly, the CPU's animated lights touched the wooden frame of the formed cube, and the Crafting Station and Pattern Storage gave no sign whether they are online or (for the station) crafting. This change belongs to PLAN.md M8 as play test polish of `m8-terminal-speed`, `m8-block-style` and `m8-station-hands`.

## What Changes

- Terminal screens register with their client content store every time they are shown, not only when they are created, so returning to the same screen (after JEI's recipe view, the pattern editor's transfer or any other screen in between) shows the cached and fresh content instead of "Loading...". The store logic moves into a pure class with JUnit tests.
- Right clicking a search box clears it (Counter Terminal).
- The formed CPU's lights keep a margin of one pixel to the glass edge, so no light touches the wooden frame on edge and corner tiles.
- Crafting Station and Pattern Storage get an online lamp, lit while a formed CPU or an online warehouse is on their data line, refreshed once a second like the Production Terminal.
- The Crafting Station's front is animated while it holds a job and crafts.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/production-terminal`: content shows again when the screen is returned to.
- `storage/counter-terminal`: content shows again when the screen is returned to; right click clears the search.
- `storage/crafting-cpu`: lights keep a margin to the frame.
- `storage/crafting-station`: online lamp and animated working front.
- `storage/patterns`: Pattern Storage online lamp.

## Non-goals

- Delivering produced items straight to the terminal (`m8-direct-delivery`).
- Re-rendering the docs icons (the user's `iconExport` run).

## Impact

- `storage/.../client/{ClientStock,ClientProduction,CounterTerminalScreen,ProductionTerminalScreen}.java`, a new pure `client/TerminalStore`.
- `storage/.../crafting/{CraftingStationBlock,CraftingStationBlockEntity}.java`, `storage/.../pattern/{PatternStorageBlock,PatternStorageBlockEntity}.java`, blockstate datagen.
- Textures from `servus-helpers/gen_block_style.py`.
