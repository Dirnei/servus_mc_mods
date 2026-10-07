# Design

## Context

See proposal.md for motivation. The Counter Terminal shows how a view block works here: a `StorageNodeBlockEntity` of the default stock kind with no item capability, a menu without slots of its own that builds a payload every ten ticks and sends it when its signature changed, a static client holder, and an `AbstractContainerScreen`. `WarehouseNetwork` has `orders()` (the `OrderBook`), `producers()` (dock, machine name, recipes, active) and `OrderBook.cancel(id)`.

## Goals / Non-Goals

**Goals:**
- Reuse the terminal's sync pattern unchanged.
- Everything the commands print, on one screen, readable at a glance.

**Non-Goals:**
- See proposal.

## Decisions

### Settles no PLAN.md open decision

### Block and node
`OrderBoardBlock` and `OrderBoardBlockEntity` in `io.github.dirnei.mc.servus.storage.board`, copied from the terminal: `FACING` and `ONLINE`, default node kind (stock), no capability so the index finds nothing, `MenuProvider`. Datagen uses the `oriented` helper with textures `order_board_side`, `order_board_top`, `order_board_front` and `order_board_front_online`.

### Rows and payloads
- `StepRow(BlockPos dock, String machine, ItemStack output, int batches, String state, String reason)`; `MachineRow(BlockPos dock, String machine, ItemStack working, String state)`; `OrderRow(int id, ItemStack item, int amount, String state, String reason, int requestId, List<StepRow> steps)` with an explicit stream codec (seven fields). Stacks that can be empty use `ItemStack.OPTIONAL_STREAM_CODEC`.
- `OrderBoardPayload(boolean online, String reason, List<MachineRow> machines, List<OrderRow> orders)` server to client; `CancelOrderPayload(int id)` client to server. Both registered in `ServusStoragePayloads`.
- `WarehouseNetwork.machineRows()` maps `producers()`: state `no_producer`, `no_recipes`, `working` with the active recipe's first output, else `idle`. `orderRows()` maps the order book, machine names from the producers by dock. `cancelOrder(id)` delegates to the book.

### Menu and screen
`OrderBoardMenu` mirrors `CounterTerminalMenu` without slots: `board()` builds the payload from the network status, `broadcastChanges` syncs every ten ticks on signature change, `cancel(id)`. `ClientOrders` holds the last payload. `OrderBoardScreen` is 240 by 200: title, Machines label and rows, Orders label and a scrollable list of lines built from the payload (an order line with icon, "amount x name", state and reason; step lines indented with machine, output, batches, state). Hovering an open order highlights it with the hint "Click to cancel"; clicking sends `CancelOrderPayload`. Mouse wheel scrolls.

### Minecraft side verification
GameTests: `orderBoardListsMachinesAndOrders` (board beside the controller of the work order setup; after an order is created the menu payload lists the dock's station as a machine row and the order with its step and state), `orderBoardCancelsOrder` (cancel through the menu marks the order cancelled and releases its reservation).

## Risks / Trade-offs

- [Long lists] → scrolling; done orders drop out after 200 ticks anyway.
- [Machine names for docks without a producer] → the faced block's name, like the producers command.

## Open Questions

None.
