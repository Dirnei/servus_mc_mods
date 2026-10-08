# Design

## Context

`m8-crafting-cpu` moved work orders onto Crafting CPUs (`storage/cpu`: `CpuBlockEntity` with an `OrderBook`, `CpuOrders.cpus(level, line)`, `CpuOrders.network(level, pos)`) and deleted the Order Board (block, `OrderBoardMenu`, `OrderBoardScreen`, `ClientOrders`, `OrderBoardPayload`, `MachineRow`, `OrderRow`, `StepRow`, `CancelOrderPayload`). The board's menu synced a payload every 10 ticks when a hash of it changed; its screen drew machines at the top and a scrollable list of orders and steps, and a click on an open order sent a cancel. The Counter Terminal (`storage/terminal`) is the model for a facing block on the data line: `DataLink` capability, `StorageNodeBlocks.ONLINE` lamp set from a server ticker, `openMenu` on use. The GUI panel texture `textures/gui/order_board.png` (240 by 200) is still in the resources.

See proposal.md for why and specs/storage/production-terminal/spec.md for the behaviour.

## Goals / Non-Goals

**Goals:**
- One read only view of production on a line, live, with cancel.
- A screen structure that takes the Patterns tab later without a rewrite.

**Non-Goals:**
- Upkeep (later change), making orders, editing recipes.

## Decisions

### Block and block entity
`storage/production/ProductionTerminalBlock` mirrors `CounterTerminalBlock`: `FACING` and `ONLINE`, placement faces the player, carrot turns, sneak carrot dismantles, empty hand opens the menu. `ProductionTerminalBlockEntity implements MenuProvider, DataLink`; registered for `ServusFoundationCapabilities.DATA_LINK` in `ServusStorageEvents`. Its server ticker refreshes the lamp every 20 ticks: online when the block's data line (`DataNetworks.networkAt`) has a CPU (`CpuOrders.cpus`) or an online warehouse (`TerminalWarehouses.reachable`). No inventory, so nothing drops but the block.

### View model
`storage/production/ProductionView` builds, on the server, a `ProductionPayload(boolean online, String reason, List<CpuRow> cpus, List<MachineRow> machines)`:
- `MachineRow(BlockPos pos, String name, ItemStack working, String state, List<RecipeRow> recipes)`, with `RecipeRow(List<ItemStack> inputs, List<ItemStack> outputs, int ticks)`; state `working`, `idle` or `no_recipes`; `working` is the first output of the recipe whose id the provider reports as active.
- `CpuRow(BlockPos cpu, int slots, int inUse, int waiting, List<OrderRow> orders)`; `OrderRow(int id, ItemStack item, int amount, String state, String reason, int requestId, List<StepRow> steps)`; `StepRow(BlockPos producer, String machine, ItemStack output, int batches, String state, String reason)`. These are the old board rows with the CPU level added, revived in the new package.
- `reason` when offline: `no_line` (no data line) or `nothing` (a line without CPU and warehouse).

`ProductionTerminalMenu` (no slots) sends the payload every 10 ticks only when its signature changed, like the board. `view()` is public so GameTests can read the server side snapshot.

### Cancel
`CancelProductionOrderPayload(BlockPos cpu, int id)` because order ids are per `OrderBook`, not global. The server handler checks that the player's open menu is a `ProductionTerminalMenu` and that the CPU is on the terminal's line, then calls `cpu.book().cancel(id)` (the same path `/servus_warehouse` and requests use) and forces a resync.

### Screen
`ProductionTerminalScreen` reuses the 240 by 200 panel (texture copied to `textures/gui/production_terminal.png`). The title row carries the tab bar on the right: tabs are an ordered list of `Tab(key, renderer)` so the Patterns tab is one more entry. Each tab has its own scroll offset. Machines tab: a header line per machine (name, state, coloured) and an indented line per recipe; Orders tab: a CPU line, then order lines (item icon, id, amount, state, origin) and step lines, drawn with the board's logic. Hover tooltips show positions and reasons; clicking an open order sends the cancel. `ClientProduction` holds the last payload with a version counter.

### Datagen and assets
Block state `oriented` like the Counter Terminal with `production_terminal` front textures (offline and online) made with the PNG helper; loot drops self; pickaxe tag; recipe: iron ingots, redstone, glass pane and a copper ingot around a comparator (shaped, gives 1); lang for the block, tabs, states, origins and hints (reusing the wording of the board).

### Testing
GameTests in Storage (`ProductionTerminalGameTests`): link membership and lamp with a CPU on the line; dark lamp and offline reason alone; the view lists a Crafting Station as idle with its sticks recipe; the view lists a CPU with an order for sticks made through `CpuOrders.create`, its slot use and its step; cancelling through the menu's server path cancels the order and releases reservations; a line without CPU gives no CPU rows. The screen itself is checked in game.

### PLAN.md
No open decision in section 5 is touched. PLAN.md already describes the terminal; the Machines tab notes that upkeep comes later.

## Risks / Trade-offs

- [Payload size with many producers and recipes] -> recipes are short lists; the payload is only sent on change. A cap of 64 recipes per producer keeps it bounded.
- [Order ids repeat across CPUs] -> the cancel payload carries the CPU position.
- [The screen is not covered by GameTests] -> the view model is; the screen is checked in game.
