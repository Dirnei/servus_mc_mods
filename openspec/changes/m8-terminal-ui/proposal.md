# Proposal

## Why

The M8 play test showed three problems with the terminals built in `m8-item-terminal-cable` and `m8-production-terminal`: an item that is not in stock cannot be requested at all, so on-demand crafting has no entry point for the player; both screens overlap (labels over slots, the title under the tab bar); and their content appears only after about half a second. The Access Point GUI also closes the moment it opens. This change is a follow up of PLAN.md M8 items 4 (`m8-item-terminal-cable`) and 6 (`m8-production-terminal`).

## What Changes

- Counter Terminal crafting requests in the style of Refined Storage: the grid lists everything the terminal's data line can make (patterns and producer recipes), items that are not in stock show "Craft" instead of a count, stocked craftable items carry a marker. A craft dialog takes an amount, shows a server computed preview (taken from stock, to craft, missing) and starts a work order on a CPU whose results are delivered into the terminal's output slots. A filter switches the grid between All, Stock and Craftable.
- New layouts for the Counter Terminal and the Production Terminal: every area has its own rectangle, defined in one pure layout class that a JUnit test checks for overlaps and for fitting the panel. The panels are drawn from that layout instead of fixed textures.
- Both terminals send their full content as soon as the screen opens; periodic updates stay for changes only. The client shows "Loading" until the first content arrives.
- The Access Point GUI stays open.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/counter-terminal`: craftable entries, craft dialog with preview, filter, instant first content.
- `storage/production-terminal`: tab bar below the title, instant first content.
- `storage/access-point`: its intake GUI opens and stays open.

## Non-goals

- Removing or redesigning the Access Point GUI (the user has not decided yet).
- Crafting previews for nested amounts across several CPUs; the preview uses the CPU that would get the order.
- Showing patterns no producer of the line can run as craftable.
- New textures; panels are painted from the layout.

## Impact

- Storage: `menu/CounterTerminalMenu`, `menu/ProductionTerminalMenu`, `menu/InboundStationMenu`, `client/CounterTerminalScreen`, `client/ProductionTerminalScreen`, `client/ClientStock`, `client/ClientProduction`, a new pure `terminal/TerminalLayout`, `terminal/StockSyncPayload` and new craft payloads in `terminal/`, `terminal/TerminalRequests`, `cpu/CpuOrders`, `order/OrderBook`, lang, GameTests, JUnit, docs Storage page.
