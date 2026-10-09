# Proposal

## Why

Play test feedback on PLAN.md M8: the player wants to hand items in at the terminal, not only take them out, and wants more room for that. The name "Counter Terminal" no longer fits what the block does. The crafting grid takes a whole row that is rarely needed, so it moves into a popup. This is a play test follow-up to the M8 terminal items (`m8-item-terminal-cable`, `m8-crafting-grid`, `m8-terminal-like-rs`).

## What Changes

- The Counter Terminal is renamed Storage Terminal everywhere: display name, block and item id `servus_storage:storage_terminal`, menu, classes, lang keys, textures, recipe, loot, docs, PLAN.md and specs. The capability `storage/counter-terminal` becomes `storage/storage-terminal`. Unreleased, so no aliases.
- New screen layout: title row with a Craft button and the search box, the item grid (requests first) with the freed space, then an Input row of 9 slots, an Output row of 9 slots, the player inventory and the hotbar. The side column stays.
- Input row: items placed there are stored into the warehouse right away along the usual push path; what does not fit stays in the slot. Shift clicking a player inventory slot moves it into the input row.
- Output row: deliveries and crafted request items land there, as in the old 3x3 output.
- Automation: a hopper or duct inserting into the terminal puts items an open request waits for into the output, everything else into the input, which stores it.
- Crafting: the 3x3 grid, arrow, result and clear button move into a popup opened by the Craft button and drawn over the item grid. Refill, shift craft, clear, Alt pull and JEI transfer keep working; JEI transfer opens the popup; closing the popup or the screen stores the grid.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/storage-terminal` (moved from `storage/counter-terminal`): input row, output row, automation inserts, crafting popup, screen layout, recipe name.

## Non-goals

- Extraction from the terminal by hoppers or ducts (stays insert only).
- Saving the crafting grid across closes.
- Changing the Production Terminal.

## Impact

- `storage`: terminal block, block entity, menu, screen, layout, JEI handlers, registries, datagen, GameTests; textures renamed.
- Every spec, doc and PLAN.md line that names the Counter Terminal.
- Dev worlds lose placed Counter Terminals (new id).
