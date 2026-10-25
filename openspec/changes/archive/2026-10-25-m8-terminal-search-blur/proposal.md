# Proposal

## Why

Once the Storage Terminal's search box has focus, nothing but the box itself can take it away: clicks on the grid, the slots or the empty panel leave the cursor in the box. While it has focus the box takes every key, so the inventory key does not close the terminal and the number keys do not move items to the hotbar.

Milestone M8 (Storage Terminal). PLAN.md has no item for it: it is a bug found in play.

## What Changes

- A click anywhere in the Storage Terminal screen that is not on the search box takes the focus from the box. The click still does what it does there (request an item, move a stack, press a button), and the search text stays.
- The box still has focus when the terminal opens, a click on it focuses it again, and a right click on it still clears it.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `shuttles/storage-terminal`: a click outside the search box takes its focus.

## Non-goals

- The text boxes of the Field Head, Shuttle Workshop, Warehouse Controller and Zone Label screens stay as they are.
- No setting for the focus on open.
- The amount and Craft dialogs keep their own focus handling.

## Impact

- `shuttles/client/StorageTerminalScreen` drops the search box's focus on a mouse press outside it. No server, network or data change.
- Touches the same `mouseClicked` as `m8-terminal-scrollbar-drag`: apply after that change.
