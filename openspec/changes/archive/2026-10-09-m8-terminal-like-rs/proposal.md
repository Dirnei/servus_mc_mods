# Proposal

## Why

Play test feedback on the Counter Terminal: it should look and behave like the terminals of Refined Storage and AE2, with their usual view settings (sorting, direction, view filter, JEI search sync, height), the crafting grid left of the output, the open requests inside the item grid instead of a separate row (AE2 style), and an amount prompt whose +Stack fills the stack first (1 becomes 64, not 65) with matching minus buttons. This is a follow up of the PLAN.md M8 item `m8-item-terminal-cable` and of `m8-crafting-grid`.

## What Changes

- A column of side buttons left of the panel, each with an icon and a tooltip: sort by name, amount or mod; ascending or descending; view all, stock or craftable (the filter button moves here); search normal, JEI synced or JEI two way; height small, medium, tall or auto.
- The settings are saved per player on the client, shared by all Counter Terminals and kept across restarts.
- New panel layout: title with the search box on the right, the item grid 9 wide with its scrollbar and as many rows as the height setting allows on the screen, a message line, a row with the crafting grid, arrow and result on the left and the output slots on the right with the clear button, then the player inventory and hotbar.
- The separate request list is removed: open requests are the first entries of the item grid, before stock and ignoring the sort, drawn on a tinted background with their delivered and requested counts and a state mark; their tooltip names the state in words and clicking one cancels it, as the row did.
- Amount prompt and craft dialog: +Stack rounds up to the next full stack, and new -1, -10 and -Stack buttons; amounts stay between 1 and 99999.
- JEI search sync only when JEI is installed.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/counter-terminal`: stock view sorting, request entries in the grid, view filter in the side column, amount buttons, screen layout with heights, saved view settings and JEI search sync.

## Non-goals

- Production Terminal layout (unchanged).
- Server side sorting or paging: sorting stays on the client.
- Saving view settings per terminal or per world.
- New request or craft behaviour on the server.

## Impact

- `storage/.../terminal/TerminalLayout.java` (Counter part becomes height dependent), new pure helpers `AmountSteps`, `TerminalSort`, `TerminalSettings`.
- `storage/.../client/CounterTerminalScreen.java`, a client settings file, a JEI bridge in `client/` set by the JEI plugin.
- `storage/.../menu/CounterTerminalMenu.java` slot positions.
- `storage/.../integration/jei/ServusStorageJeiPlugin.java` (runtime for search sync, extra areas for the side buttons).
- Lang provider, docs page, JUnit tests.
