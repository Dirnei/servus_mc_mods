# Design

## Context

See proposal.md for motivation. The warehouse is `WarehouseNetwork`, a `ServusNetwork` over `StorageNodeBlockEntity` nodes with `WarehouseConnectionRule`; the Lib graph exposes `neighbors(NodePos)` for the edges the rule accepted. Nodes have a `kind()`: `STOCK`, `RAIL`, `ELEVATOR`. The network keeps `sources`, an index with `sourcesHolding(key)`, a `connectors` set, `extract(key, amount)` and `insert(stack)`, and ticks through `onTick`. `CounterTerminalMenu.pull(key, toInventory)` extracts and gives the stack to the cursor or inventory; `StockSyncPayload` goes to the player with the menu open through `PacketDistributor.sendToPlayer`; payloads register in `ServusStoragePayloads`. `WarehouseControllerBlockEntity` is an empty marker node; `WarehouseControllerBlock.useWithoutItem` prints the summary. `StorageStatsManager` loads `warehouse.json`. The storage GameTests build on an all air template and use a mock player that is not in the player list.

## Goals / Non-Goals

**Goals:**
- Shuttles as plain data: no entities, no per block scanning, movement is O(shuttles) per tick.
- Pure path finding and movement with JUnit tests.
- Cheap rendering: positions synced only while something moves.

**Non-Goals:**
- Reservations, traffic, power. See proposal non-goals.

## Decisions

### Settles the PLAN.md open decision
"Does a warehouse without shuttles feel complete enough on its own?" is answered yes: without shuttles every pull is instant, which is the early game. Shuttles are a mid game throughput layer. Section 5 drops the bullet and section 2.3 states it.

### Pure path finding and movement
Package `io.github.dirnei.mc.servus.storage.shuttle`, no Minecraft imports:
- `RailPaths.find(NodePos start, Predicate<NodePos> goal, Function<NodePos, Set<NodePos>> neighbours)`: breadth first search returning the node list from start to the first goal node inclusive, or an empty list. The caller passes a neighbours function that only yields `RAIL` and `ELEVATOR` nodes. `RailPathsTest`: straight line, branch picks the shorter, unreachable gives empty, start is goal gives one node.
- `ShuttleMotion`: a path, a segment index and a progress from 0 to 1, with `advance(double blocksPerTick)` that walks segments and returns true on arrival, `position()` as interpolated doubles, and `reverse()` to build the way home. `ShuttleMotionTest`: advancing across segments, arrival, a one node path arrives at once, position interpolation.

### Fleet and jobs on the network
`WarehouseNetwork` gets a `ShuttleFleet` field (Minecraft side, in the `shuttle` package):
- `Shuttle` holds `ShuttleMotion` or null when parked, an `ItemStack cargo`, and the current `PickJob`.
- `PickJob(ItemKey key, int amount, Player requester, BlockPos terminal)` keeps the requester as a server side object so delivery works for mock players too; before delivery it checks `requester.isAlive()` and `!requester.isRemoved()`.
- `homeRail()`: the first neighbour of the controller position whose kind is `RAIL`, recomputed when membership changes (flag set in `onNodeAdded`, `onNodeRemoved`, `onAbsorb`).
- `canShuttle(key)`: fleet not empty, home rail present, and `sourcesHolding(key)` contains a position with a `RAIL` or `ELEVATOR` neighbour in the graph. `requestPick(key, amount, requester, terminal)` enqueues the job and returns true, else the terminal pulls instantly.
- `tick()`: assigns queued jobs to free shuttles by shortest path from each shuttle's current node to any rail touching a block holding the key (the path search runs once per assignment, not per tick); advances every moving shuttle by `shuttle_speed`; on arrival at the target rail extracts from the adjacent holding sources into cargo (same `extract` code with a position filter); builds the reversed path home; on arrival home delivers through `requester.getInventory().add` or drops at the terminal; then takes the next job or parks.
- Controller handling: `onNodeAdded` of a `WarehouseControllerBlockEntity` sizes the fleet from its `shuttleCount()`; `onNodeRemoved` drops cargo at the controller through `Block.popResource` and clears the fleet; `onAbsorb` merges fleets; `onDisposed` clears. The count itself lives on the block entity (`Shuttles` NBT int), so splits and reloads keep it.
- The controller block: right click with a Shuttle item calls `addShuttle()` on the block entity which refuses above `max_shuttles`; sneak right click with an empty hand calls `takeShuttle()`; the summary from `useWithoutItem` includes `fleet.summary()`; dismantle and `onRemove` drop `shuttleCount` shuttle items.

### Terminal integration
`CounterTerminalMenu.pull`: when `network.canShuttle(key)` it calls `requestPick(key, room, player, terminalPos)` and returns without touching the cursor; `forceSync` still runs so the stock view refreshes when the shuttle extracts. Push stays as is.

### Sync and rendering
- `ShuttleSyncPayload(BlockPos controller, List<ShuttleView> shuttles)` where `ShuttleView(List<BlockPos> path, int segment, float progress, float speed, ItemStack cargo)`; parked shuttles send a one node path at the home rail. Sent with `PacketDistributor.sendToPlayersTrackingChunk` from the fleet tick every 5 ticks while any shuttle moves, once more when the last one stops, and once when a shuttle is added or removed.
- `ClientShuttles` keeps the latest view per controller position plus the client game time at receipt; `WarehouseControllerRenderer` (a `BlockEntityRenderer` with `shouldRenderOffScreen` true and an infinite `getRenderBoundingBox`) copies each view, advances it by `speed * (ticks since receipt + partialTick)`, and draws a cart at the position: the Shuttle item model with `ItemDisplayContext.NONE` scaled 0.6, lifted two pixels above the rail, yawed to the segment direction, with the cargo as a 0.3 scale item on top. Idle shuttles are offset a sixth of a block each along the rail so a parked fleet shows as a row.
- The Shuttle item model is a datagen block style model: a 12 by 3 by 12 box with the `item/shuttle` texture, plus the standard `block/block` display transforms so it looks fine in hand and in the GUI.

### Registration and data
- `ServusStorageItems.SHUTTLE`: plain `Item` with `stacksTo(16)`, in the tab after the elevator. Recipe: `"I I"`, `"RBR"`, `"III"` with iron ingots, redstone and a Warehouse Rail, output 1.
- `warehouse.json` gains `"max_shuttles": 8` and `"shuttle_speed": 0.25`; `StorageStatsManager` reads both (`doubleField` for the speed).
- Lang: "Shuttle", `gui.servus_storage.summary.shuttles` style strings are plain `Component.literal` in the summary like the rest of it.

### GameTest
`shuttlePicksFromRail`: controller at (2,1,2), home rail at (3,1,2), rails to (7,1,2), crate at (7,1,3) beside the last rail with 40 cobblestone, terminal at (2,1,3) touching the controller, stone under every rail; one shuttle added through `controller.addShuttle()`. A mock player opens a `CounterTerminalMenu` and pulls cobblestone to inventory: the inventory stays empty right away; after 60 ticks (eight blocks there and back at a quarter block per tick is 64 ticks, the test waits 80) the inventory holds 40 cobblestone and the crate is empty. A second part pulls dirt from a crate touching the controller directly and asserts it arrives at once.

## Pure versus Minecraft logic

Pure: `RailPaths` and `ShuttleMotion` with tests. Minecraft: fleet, jobs, controller block and entity, terminal pull, payload, client store, renderer, datagen, GameTest. Rendering is checked in the client.

## Risks / Trade-offs

- [Requester reference keeps a player object alive] -> Jobs are short lived and checked for removal before delivery; a player who leaves gets a drop at the terminal.
- [Many shuttles moving means a packet every 5 ticks per controller] -> Only while moving, and the payload is small. A busier sync budget is a later knob.
- [Shuttle arrives and the stock moved to another block] -> The shuttle extracts only from blocks beside its target rail; if nothing is there it comes home empty, as specified.
- [Path search cost on big warehouses] -> Breadth first over rail nodes only, once per job assignment.
