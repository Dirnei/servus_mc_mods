# Proposal

## Why

Requesting a few items from the Storage Terminal takes too many steps: a click opens the amount prompt, which starts at a full stack, while early players mostly want one or a handful. Open requests also sit as entries at the top of the item grid, which pushes the stock down in a screen that is already crowded, and the Output row below shows the deliveries apart from the requests that brought them.

## What Changes

PLAN.md milestone M8 (On-demand autocrafting, Storage Terminal). This is a new point from play testing, added to PLAN.md "Open: Shuttles" by this change.

- Each open request of a terminal takes one slot of the Output row. The slot shows the request: a ghost of the item while nothing has arrived, the delivered and requested amounts, and a mark for the state. Deliveries for the request land in its slot.
- Open requests are no longer entries in the item grid; the grid lists stock and craftable items only.
- A click on a stocked grid entry requests one item. A second click on the same item adds one to its open request instead of making another one.
- Shift click keeps requesting one stack, or all that is requestable when less.
- Right click on a grid entry opens the amount prompt as it is today (amounts, Rule mode, crafted rest).
- Ctrl click and middle click on a craftable entry keep opening the craft dialog; a "Craft" entry with nothing in stock still opens it on any click.
- On a request slot, left and shift click take the items like any slot; middle click or Ctrl click cancels the request under today's cancel rule. Its tooltip names the item, the amounts, the state and how to cancel.
- A request larger than one stack keeps its single slot and refills it: what does not fit waits at the Packing Station, shown as waiting for room, until the player takes items out.
- A slot that still holds items of a finished or cancelled request stays taken until it is emptied; a new request for the same item may use it.
- With every output slot taken, a new request is refused with "Output row full, take your items". At most `terminal_output_slots` requests (example 9) are open at once.
- Transfers from another site and craft requests (craft dialog, order grid) take a slot the same way.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `shuttles/storage-terminal`: request clicks, output slots bound to requests, open requests shown in the Output row instead of the grid, cancel on the slot, refusal when the row is full, craft and transfer requests taking a slot, layout scenario wording.

## Non-goals

- More than one Output row, or a scrolling list of requests beyond `terminal_output_slots`.
- Merging repeated clicks into a request that is producing on a work order or into a transfer from another site; those clicks make a new request in a new slot.
- Changing the amount prompt itself, the craft dialog, the order grid or stock rules.
- Showing other terminals' requests or a request history.

## Impact

- `shuttles/terminal`: `StorageTerminalBlockEntity` (slot of each request, saved; packet routing into the request's slot), `TerminalRequests` (merging clicks, refusal when the row is full, slot for transfers and craft requests), `RemoteTransfers`, `RequestRow` (slot index), a new pure slot assignment helper with JUnit tests.
- `shuttles/menu/StorageTerminalMenu` and `shuttles/client/StorageTerminalScreen`: grid click mapping, request drawing on the Output row, cancel by middle or Ctrl click, no request cells in the grid.
- Lang keys through `ServusShuttlesLanguageProvider` (refusal, tooltip lines).
- GameTests in `TerminalRequestGameTests` and `StorageTerminalRowsGameTests`.
