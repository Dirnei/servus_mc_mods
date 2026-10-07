# Proposal

## Why

Work orders and stock rules run on their own, but their state is only visible in chat through commands. A player who wonders why the gears are not coming needs one screen that shows the open orders, which machine works on what, and what is blocked and why. This change implements the M7 item "Order board screen" from `PLAN.md` and completes the milestone.

## What Changes

- **Order Board block.** A wall facing block that joins the warehouse like the Counter Terminal (beside stock blocks or the controller) and shows the online lamp. Right click opens the board screen.
- **Board screen.** Two sections. Machines: every dock with the name of the machine it faces and what it works on right now, or idle, no recipes, no machine. Orders: every work order with its item, amount, state and reason, and under it each step with its dock's machine, the output, the batches and the step state. Rule orders and request orders are told apart. The list scrolls.
- **Cancel from the board.** Clicking an open order cancels it, with the same effect as cancelling its request: unfed steps are dropped and their reservations released.
- **Live updates.** The board syncs to the open screen every half second when something changed, like the terminal.

## Capabilities

### New Capabilities
- `storage/order-board`: block, screen content, cancel, sync.

### Modified Capabilities
None.

## Non-goals

- Editing rules or creating orders from the board. The terminal and commands do that.
- Historic orders or statistics. Done orders show for a short while and disappear, as the order book keeps them.

## Impact

- `storage`: a new package `io.github.dirnei.mc.servus.storage.board` with block, block entity, rows and payloads; `OrderBoardMenu`, `ClientOrders`, `OrderBoardScreen`; `WarehouseNetwork.orderRows`, `machineRows` and `cancelOrder`; registrations, datagen, textures, lang, a recipe.
- `PLAN.md` needs no change. With this change every M7 item is implemented.
