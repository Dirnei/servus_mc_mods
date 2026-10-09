# Proposal

## Why

Play test bug: items placed in the Storage Terminal's Input row stay there. The terminal stores through `WarehouseNetwork.insert`, which only pushes into crates and foreign inventories and never into Heavy Duty Racks, so a rack warehouse never takes them. The terminal sits on the data line (PLAN.md M8), so its input belongs on the same path as every other intake: packets to an Access Point, which puts the items away with shuttles. This is a follow up of M8 item `m8-storage-terminal`.

## What Changes

- Everything the terminal stores goes into its Input row, and the Input row sends its stacks as packets over the data line to an Access Point of a reachable warehouse, which puts them away like any intake (racks by shuttle, zones, crates). This covers the Input row, clicking the grid with a carried stack, shift clicks from the inventory, automation inserts, clearing the crafting grid and closing the terminal.
- The Access Point is the one on the terminal's data line nearest to the terminal.
- Without an Access Point on the line the items stay in the Input row and the message line says "No Access Point on this data line".
- The old instant push into crates is gone for the terminal.
- The row arrows are inverted: the Input arrow points up into the warehouse, the Output arrow points down to the player.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/storage-terminal`: Push into the warehouse, Input row, Clear the grid, Grid empties on close and Screen layout (arrows).

## Non-goals

- Stock rules, requests and crafting are unchanged.
- Tracking input packets in the production ledger: packets in flight are settled at server stop as everywhere, a crash loses them.
- Other blocks that push into the warehouse (Inbound Station, Access Point intake) are unchanged.

## Impact

- `storage/.../terminal/StorageTerminalBlockEntity.java`, `storage/.../menu/StorageTerminalMenu.java`, `storage/.../client/StorageTerminalScreen.java`, lang provider, GameTests, docs page.
- Putting items away is no longer instant: packet travel plus the shuttle trip.
