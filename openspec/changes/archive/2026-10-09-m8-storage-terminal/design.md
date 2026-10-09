# Design

## Context

The terminal is `CounterTerminalBlock`/`CounterTerminalBlockEntity` with `CounterTerminalMenu` and `CounterTerminalScreen`, laid out by `TerminalLayout.Counter` and checked by `TerminalLayoutTest`. Its output (`terminal_output_slots`, 9) is shown as a 3x3 next to the crafting grid; the block entity exposes an insert only handler over the output (`OutputInsertHandler`) that packets and hoppers use. See proposal.md.

## Decisions

### Rename
A script replaces `CounterTerminal` with `StorageTerminal`, `COUNTER_TERMINAL` with `STORAGE_TERMINAL`, `counter_terminal` with `storage_terminal`, `counter-terminal` with `storage-terminal` and "Counter Terminal" with "Storage Terminal" in code, data, docs and specs, and `git mv` renames the files (classes, textures, the docs icon, the main spec dir). Lang keys follow the id. Main specs of other capabilities that only name the terminal are edited directly: the rename changes no behaviour there. `TerminalLayout.Counter` becomes `TerminalLayout.Storage`.

### Layout
Rows below the grid sit at fixed offsets from the base top (slots never move; extra grid rows grow upwards as before): message 92, Input row 104, Output row 124, inventory label 146, inventory 157, hotbar 215, base height 240. The Craft button sits on the title row left of the search box. The popup is a fixed rectangle over the four base grid rows (y 18 to 90): 3x3 grid, arrow, result, clear button. Output shows at most 9 slots in one row; a JSON value above 9 is clamped (recorded, the default is 9).

### Popup slots
The crafting slots always exist in the menu (fixed positions inside the popup) but report `isActive()` false while the client's popup flag is off, so they neither render nor take clicks. Closing the popup sends the existing grid clear payload. JEI's transfer handler opens the popup through the screen before sending the fill payload. Shift click from the inventory goes to the input row, never into the grid.

### Input row and automation
The block entity gets an `input` handler (9 slots, saved, dropped on break). On a change it is marked to store; the server tick stores marked input through the same push as clicking the grid (`WarehouseNetwork.insert` over the reachable warehouses) every 10 ticks while something is left. The capability handler routes inserts: if an open request still misses the item, into the output (credited as today), otherwise into the input. Packets and hoppers use the same handler; they cannot be told apart and do not need to be.

## Testing
JUnit: layout for every height at 1080p scales 2, 3, 4, including the popup inside the grid area. GameTests: input stores and keeps leftovers, automation routing, output deliveries, crafting through the popup slots, the new registry id.

## Risks / Trade-offs
- [A hopper inserting an item a request waits for credits that request] -> intended; it is the same item the player asked for.
- [Old worlds lose placed Counter Terminals] -> unreleased.
