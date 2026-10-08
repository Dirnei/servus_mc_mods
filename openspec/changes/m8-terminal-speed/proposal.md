# Proposal

## Why

Play test feedback on PLAN.md M8 (Counter Terminal and Production Terminal from `m8-item-terminal-cable`, `m8-production-terminal`, `m8-terminal-ui`): both terminals show "Loading..." every time they open. The specs already promise content "in the same tick the screen opens", but measurement shows the opposite:

- Opening a menu in 1.21.1 calls `AbstractContainerMenu.setSynchronizer`, which calls `sendAllDataToRemote`. It never calls `broadcastFullState`, where the terminals send their content. The first content therefore waits for the periodic sync, ten `broadcastChanges` ticks later. The GameTest from `m8-terminal-ui` called `broadcastFullState` by hand and passed anyway. A GameTest on the real open path reports "sent at -1".
- Both screens wipe the client store in their constructor, so nothing seen before can be shown while waiting.
- Each sync rebuilds the whole view from scratch: about 1.2 ms for the Counter Terminal on a line with one furnace adapter (0.4 ms of it the craftable list sorted from about 900 smelting recipes), every half second per open terminal, growing with every machine.

## What Changes

- Both terminals send their full content from `sendAllDataToRemote`, the call the game makes when the screen opens, and keep `broadcastFullState` for resyncs.
- The client keeps the last content of every terminal by position for the session and shows it at once when that terminal opens again; the fresh content replaces it without a flash. "Loading..." shows only the first time a terminal is opened in a session.
- The server keeps a ready view for the slow parts: the merged stock is cached per set of warehouses until one of their indexes really changes, and the craftable list is cached per data line until the producers' recipe lists change. Opening a terminal or a periodic sync only serializes what is ready.
- The warehouse index gets a version that only moves when its totals really change, and caches its sorted entries per version.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/counter-terminal`: "Live stock while open" says the last content is shown at once and the loading hint only appears the first time.
- `storage/production-terminal`: "Live view", the same for the Production Terminal.

## Non-goals

- The Production Terminal redesign (Crafting, Patterns, Machines status overview): a later change.
- Pushing changes to open terminals by event instead of the half second check.
- Caching the production rows: they cost about 0.15 ms and change with every step.

## Impact

- `storage/menu/{CounterTerminalMenu,ProductionTerminalMenu}.java`, `storage/client/{ClientStock,ClientProduction,CounterTerminalScreen,ProductionTerminalScreen}.java`, `storage/terminal/{StockSyncPayload,TerminalCraftables,TerminalWarehouses}.java`, `storage/production/ProductionPayload.java`, `storage/index/WarehouseIndex.java`, a client logout hook.
- GameTests: `TerminalSpeedGameTests` (real open path), the old `terminalSendsOnOpen` now uses the real open path.
