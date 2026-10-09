# Proposal

## Why

During the M8 play test the user asked for a crafting terminal: crafting by hand at the Counter Terminal with the warehouse as the item source, like the Refined Storage Crafting Grid. Today a player has to request every ingredient into the output slots, take them out and walk to a crafting table. This change belongs to PLAN.md M8 (on-demand autocrafting), as a play test follow up to `m8-terminal-ui`.

## What Changes

- The Counter Terminal screen gets a 3x3 crafting grid with a result slot in its own area; the panel grows so nothing overlaps and it still fits 1080p at GUI scale 4.
- Taking the result crafts with vanilla crafting recipes. After every craft each grid slot that lost an ingredient is topped up with the same item from the warehouse's instant stock (crates and foreign inventories), so shift clicking the result crafts until stock or inventory runs out.
- Alt click on a stock entry puts one stack of it from instant stock onto the cursor, so it can be placed into the grid like any item. Plain click, shift click and Ctrl or middle click keep their request and craft meanings.
- A clear button moves the grid contents into the warehouse; what does not fit goes to the player inventory.
- JEI's transfer button on a crafting recipe fills the grid, taking items from the warehouse first and then from the player inventory.
- Closing the terminal moves the grid contents into the warehouse, and what does not fit into the player inventory.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/counter-terminal`: new crafting grid requirements; the screen layout requirement includes the grid.

## Non-goals

- Taking ingredients from Heavy Duty Racks for the grid: racks are only served by shuttles, which are not instant. Racks are not used for refill or Alt click.
- A separate Crafting Terminal block or a pattern encoding grid in the Counter Terminal.
- Respecting reservations of running work orders when the grid takes stock (the grid takes what instant stock holds, like the existing pushes and the old instant pulls).
- Persisting the grid contents in the block: they live with the open screen only.

## Impact

- `storage`: `menu/CounterTerminalMenu`, `client/CounterTerminalScreen`, `terminal/TerminalLayout` and its test, new payloads in `terminal/` registered in `ServusStoragePayloads`, a JEI transfer handler for the Counter Terminal, lang, docs, GameTests.
- No Foundation or Manufacturing changes.
