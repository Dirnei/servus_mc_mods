# Proposal

## Why

With a controller indexing the warehouse, the player needs a place to use it: search the stock and pull items while standing at the counter, and hand items over to be stored. PLAN.md milestone M4, item "Counter Terminal with search and pull".

## What Changes

- **Counter Terminal** block: a storage node with a GUI. The GUI shows the warehouse stock as an item grid with counts, a search box filtering by name, and the player inventory.
- Pull: click an entry to take a stack to the cursor, shift click to send a stack to the inventory. Push: click the grid with a carried stack to store it, shift click an inventory slot to store it.
- Stock sync from server to client while the menu is open, only when the index changed, at most every half second.
- Offline networks show the reason instead of the grid.
- Crafting recipe, textures, lang.
- GameTest for the Storage mod: terminal pull and push through the menu API, with the shared empty template.

## Capabilities

### New Capabilities
- `storage/counter-terminal`: GUI, search, pull, push, sync, offline state.

### Modified Capabilities
None.

## Non-goals

- Crafting from the terminal, request queues, wireless (later milestones).
- Sorting modes beyond count descending.

## Impact

- First Storage payloads (`ServusStoragePayloads`) and client screen with widgets.
- Storage GameTests enabled through the existing run config.
