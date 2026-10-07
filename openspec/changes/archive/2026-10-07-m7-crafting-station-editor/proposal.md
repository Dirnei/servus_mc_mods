# Proposal

## Why

Play test (2026-10-07): the Crafting Station GUI is not intuitive. Its pattern grid, Save button and recipe cells sit side by side on the main screen, so it is unclear what the grid is for, and a click on a recipe cell deletes the recipe without asking. The Assembler solved the same task with a plus button and a recipe editor popup with a live preview, and the user wants the station to work the same way.

Milestone: M7 item "Storage only Crafting Station fallback", a follow up to the archived `m7-crafting-station`.

## What Changes

- The Crafting Station GUI follows the Assembler: inputs on the left, a column of recipe cells with a plus button, the outputs with a progress arrow on the right, the recipe being crafted marked.
- Plus opens a recipe editor popup with an empty ghost grid; clicking a recipe cell opens the editor prefilled with that recipe. The editor shows a live preview of the result and has Save, Delete (existing recipe only) and Cancel. Errors keep the editor open and show in the action bar.
- A click on a cell no longer deletes; Delete in the editor does.
- With JEI installed, the transfer button on a crafting recipe opens the station's editor prefilled with that recipe, like the Assembler.
- The popup drawing and hit testing move to Lib so the Assembler and the station share one editor look. The Assembler's behaviour does not change.
- The stale rule "the carrot without sneaking turns it" is corrected: the carrot cycles the clicked side.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/crafting-station`: recipe configuration through the editor popup, JEI transfer, carrot wording.

## Non-goals

- More recipe slots, NBT aware templates, or recipe types other than vanilla crafting.
- Any change to the Assembler's behaviour or layout beyond using the shared Lib popup.

## Impact

- `lib`: client `RecipeEditorPopup` (popup frame, dimming, ghost grid frame, preview slot, Save, Delete and Cancel buttons, hit testing) and shared editor button ids.
- `manufacturing`: `AssemblerScreen` draws its popup through the Lib widget, no behaviour change.
- `storage`: `CraftingStationMenu` gets the editor state (open new, open edit, save, delete, cancel, pattern slots active only while editing), `CraftingStationScreen` and its texture are redone, JEI `compileOnly` API plus a soft dependency plugin with a transfer handler, lang, GameTests.
